package com.mecanicadbo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.stream.IntStream;

/**
 * Criptografia do CPF no banco.
 *
 * - cifrar/decifrar: AES-256-GCM com IV aleatório. O mesmo CPF gera um texto
 *   diferente a cada vez, e quem abrir o banco não lê o número.
 * - hash: HMAC-SHA256, sempre igual para o mesmo CPF e impossível de reverter.
 *   Serve para o índice único (CPF repetido) e para a busca pelo CPF completo.
 *
 * As duas chaves derivam de UMA chave mestra, a variável de ambiente CPF_CHAVE
 * (32 bytes em Base64). Sem ela o sistema não sobe. PERDER A CHAVE = PERDER OS
 * CPFs: ela deve ficar anotada fora do PC, como a senha dos backups.
 */
@Component
public class CpfCripto {

    private static final String PREFIXO = "v1:";   // permite trocar o formato no futuro
    private static final int TAMANHO_IV = 12;
    private static final int TAMANHO_TAG_BITS = 128;

    private final SecretKeySpec chaveCifra;
    private final SecretKeySpec chaveBusca;
    private final String digital;
    private final SecureRandom aleatorio = new SecureRandom();

    @Autowired
    public CpfCripto(@Value("${app.cpf.chave:}") String chaveMestraBase64) {
        this(chaveMestraBase64, "CPF_CHAVE", true);
    }

    /**
     * Para abrir CPFs gravados com uma chave antiga (troca de chave). Aceita
     * chave fraca, justamente para conseguir sair dela.
     */
    public static CpfCripto daChaveAntiga(String chaveBase64) {
        return new CpfCripto(chaveBase64, "CPF_CHAVE_ANTIGA", false);
    }

    private CpfCripto(String chaveMestraBase64, String nomeVariavel, boolean exigirChaveForte) {
        byte[] mestra = lerChave(chaveMestraBase64, nomeVariavel);
        if (exigirChaveForte && pareceChaveDeExemplo(mestra)) {
            throw new IllegalStateException("A " + nomeVariavel + " parece uma chave de exemplo (ex.: \"xxxx...\"), "
                  + "não uma chave gerada. Gere uma de verdade (veja o README do backend). "
                  + "Se os CPFs já foram gravados com ela, passe-a em CPF_CHAVE_ANTIGA para a troca.");
        }
        this.chaveCifra = new SecretKeySpec(derivar(mestra, "mecanica-dbo:cpf:cifra"), "AES");
        this.chaveBusca = new SecretKeySpec(derivar(mestra, "mecanica-dbo:cpf:busca"), "HmacSHA256");
        this.digital = HexFormat.of().formatHex(derivar(mestra, "mecanica-dbo:cpf:digital"));
    }

    private static byte[] lerChave(String base64, String nomeVariavel) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException(
                    "Defina a variável de ambiente " + nomeVariavel + " (chave da criptografia do CPF) antes de iniciar o sistema. "
                  + "Veja o README do backend para gerar uma.");
        }
        String limpa = base64.trim();
        if (limpa.length() >= 2 && limpa.startsWith("\"") && limpa.endsWith("\"")) {
            limpa = limpa.substring(1, limpa.length() - 1).trim();   // aspas coladas junto no IntelliJ
        }
        byte[] mestra;
        try {
            mestra = Base64.getDecoder().decode(limpa);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("A " + nomeVariavel + " não é um Base64 válido.", e);
        }
        if (mestra.length != 32) {
            throw new IllegalStateException("A " + nomeVariavel + " precisa ter 32 bytes (44 caracteres em Base64); tem " + mestra.length + ".");
        }
        return mestra;
    }

    /**
     * Chave gerada ao acaso tem quase sempre 25+ bytes diferentes entre os 32.
     * Menos de 16 é sinal de texto digitado ("xxxx...", "AAAA...", "1234...").
     */
    static boolean pareceChaveDeExemplo(byte[] chave) {
        return IntStream.range(0, chave.length).map(i -> chave[i] & 0xff).distinct().count() < 16;
    }

    /** Identifica a chave sem revelá-la: duas instâncias com a mesma chave têm a mesma digital. */
    public boolean mesmaChaveQue(CpfCripto outra) {
        return digital.equals(outra.digital);
    }

    /** "12345678900" → "v1:<base64 de IV + texto cifrado>". null continua null. */
    public String cifrar(String cpfDigitos) {
        if (cpfDigitos == null) return null;
        try {
            byte[] iv = new byte[TAMANHO_IV];
            aleatorio.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, chaveCifra, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(cpfDigitos.getBytes(StandardCharsets.UTF_8));
            byte[] tudo = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return PREFIXO + Base64.getEncoder().encodeToString(tudo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao criptografar o CPF.", e);
        }
    }

    /** Volta ao CPF em números. Chave errada ou texto adulterado → erro (GCM confere a integridade). */
    public String decifrar(String cpfCifrado) {
        if (cpfCifrado == null) return null;
        if (!cpfCifrado.startsWith(PREFIXO)) {
            throw new IllegalStateException("CPF criptografado em formato desconhecido.");
        }
        try {
            byte[] tudo = Base64.getDecoder().decode(cpfCifrado.substring(PREFIXO.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, chaveCifra, new GCMParameterSpec(TAMANHO_TAG_BITS, tudo, 0, TAMANHO_IV));
            byte[] claro = cipher.doFinal(tudo, TAMANHO_IV, tudo.length - TAMANHO_IV);
            return new String(claro, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Não foi possível abrir um CPF do banco. A CPF_CHAVE é a mesma usada quando ele foi gravado?", e);
        }
    }

    /** Impressão digital do CPF (64 caracteres hex), para o índice único e a busca exata. */
    public String hash(String cpfDigitos) {
        if (cpfDigitos == null) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(chaveBusca);
            return HexFormat.of().formatHex(mac.doFinal(cpfDigitos.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao calcular o hash do CPF.", e);
        }
    }

    private static byte[] derivar(byte[] mestra, String finalidade) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(mestra, "HmacSHA256"));
            return mac.doFinal(finalidade.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao preparar as chaves do CPF.", e);
        }
    }
}
