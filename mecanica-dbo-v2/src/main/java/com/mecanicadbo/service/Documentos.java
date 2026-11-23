package com.mecanicadbo.service;

import java.util.Locale;

/**
 * Documento do cliente: CPF (pessoa física) ou CNPJ (empresa).
 *
 * Os dois ficam no mesmo campo (criptografado, ver CpfCripto), sempre
 * "limpos": só letras maiúsculas e números, sem pontos, barra ou traço.
 *
 * Regras da Receita Federal:
 *  - CPF: 11 números; os 2 últimos são dígitos verificadores (módulo 11).
 *  - CNPJ: 14 caracteres. Desde julho de 2026 os 12 primeiros podem ter
 *    LETRAS (CNPJ alfanumérico, IN RFB nº 2.229/2024); os 2 últimos
 *    continuam números. No cálculo do dígito, cada caractere vale o seu
 *    código ASCII menos 48 ("0" = 0 ... "9" = 9, "A" = 17 ... "Z" = 42),
 *    com pesos de 2 a 9 da direita para a esquerda (módulo 11). Para um
 *    CNPJ só de números, é exatamente o cálculo antigo.
 *  - Sequências repetidas (000.000.000-00, 11.111.111/1111-11) são inválidas.
 */
public final class Documentos {

    public enum Tipo { CPF, CNPJ }

    private Documentos() {}

    /** " 12.abc.345/01de-35 " → "12ABC34501DE35". Vazio → null. */
    public static String limpar(String doc) {
        if (doc == null) return null;
        String limpo = doc.toUpperCase(Locale.ROOT).replaceAll("[^0-9A-Z]", "");
        return limpo.isEmpty() ? null : limpo;
    }

    /** CPF, CNPJ ou null (formato que não é nenhum dos dois). Recebe o documento já limpo. */
    public static Tipo tipo(String limpo) {
        if (limpo == null) return null;
        if (limpo.matches("\\d{11}")) return Tipo.CPF;
        if (limpo.matches("[0-9A-Z]{12}\\d{2}")) return Tipo.CNPJ;
        return null;
    }

    public static boolean cpfValido(String limpo) {
        if (limpo == null || !limpo.matches("\\d{11}") || todosIguais(limpo)) return false;
        return digitoCpf(limpo, 9) == limpo.charAt(9) - '0'
            && digitoCpf(limpo, 10) == limpo.charAt(10) - '0';
    }

    public static boolean cnpjValido(String limpo) {
        if (limpo == null || !limpo.matches("[0-9A-Z]{12}\\d{2}") || todosIguais(limpo)) return false;
        return digitoCnpj(limpo, 12) == limpo.charAt(12) - '0'
            && digitoCnpj(limpo, 13) == limpo.charAt(13) - '0';
    }

    /** "CPF" ou "CNPJ" para mensagens e para o recibo; "CPF/CNPJ" se não der para saber. */
    public static String rotulo(String doc) {
        Tipo t = tipo(limpar(doc));
        return t == null ? "CPF/CNPJ" : t.name();
    }

    /** Com pontuação: "123.456.789-09" ou "12.ABC.345/01DE-35". Formato desconhecido volta como veio. */
    public static String formatar(String doc) {
        String d = limpar(doc);
        Tipo t = tipo(d);
        if (t == Tipo.CPF) {
            return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
        }
        if (t == Tipo.CNPJ) {
            return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "/"
                 + d.substring(8, 12) + "-" + d.substring(12);
        }
        return doc;
    }

    /**
     * Para mostrar a terceiros (tela e recibo). CPF é dado pessoal: sai
     * mascarado ("***.456.789-**"). CNPJ é público (consta no cartão CNPJ
     * da Receita): sai completo. Vazio vira null.
     */
    public static String mascarar(String doc) {
        String d = limpar(doc);
        if (d == null) return null;
        Tipo t = tipo(d);
        if (t == Tipo.CPF) return "***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**";
        if (t == Tipo.CNPJ) return formatar(d);
        return "***";
    }

    // ------------------------------------------------------------------

    /** Dígito do CPF na posição {@code pos} (9 ou 10), pesos 10..2 / 11..2. */
    private static int digitoCpf(String d, int pos) {
        int soma = 0;
        for (int i = 0; i < pos; i++) soma += (d.charAt(i) - '0') * (pos + 1 - i);
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** Dígito do CNPJ na posição {@code pos} (12 ou 13): valor = ASCII - 48, pesos 2..9 da direita. */
    private static int digitoCnpj(String d, int pos) {
        int soma = 0;
        int peso = 2;
        for (int i = pos - 1; i >= 0; i--) {
            soma += (d.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static boolean todosIguais(String s) {
        return s.chars().distinct().count() == 1;
    }
}
