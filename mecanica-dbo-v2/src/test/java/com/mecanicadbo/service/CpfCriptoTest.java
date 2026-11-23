package com.mecanicadbo.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfCriptoTest {

    /** Chave só de teste (bytes 0..31). Nunca use em produção. */
    static final String CHAVE_TESTE = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";

    private final CpfCripto cripto = new CpfCripto(CHAVE_TESTE);

    @Test
    void cifraSemDeixarONumeroLegivelEDecifraDeVolta() {
        String cifrado = cripto.cifrar("12345678900");

        assertThat(cifrado).startsWith("v1:").doesNotContain("12345678900").doesNotContain("456");
        assertThat(cripto.decifrar(cifrado)).isEqualTo("12345678900");
    }

    @Test
    void mesmoCpfGeraTextoDiferenteACadaVezMasOMesmoHash() {
        assertThat(cripto.cifrar("12345678900")).isNotEqualTo(cripto.cifrar("12345678900"));
        assertThat(cripto.hash("12345678900")).isEqualTo(cripto.hash("12345678900")).hasSize(64);
        assertThat(cripto.hash("12345678900")).isNotEqualTo(cripto.hash("12345678901"));
    }

    @Test
    void chaveErradaNaoAbreOCpf() {
        String cifrado = cripto.cifrar("12345678900");
        CpfCripto outraChave = new CpfCripto("HyAhIiMkJSYnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4=");

        assertThatThrownBy(() -> outraChave.decifrar(cifrado))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CPF_CHAVE");
    }

    @Test
    void semChaveOuComChaveDeTamanhoErradoOSistemaNaoSobe() {
        assertThatThrownBy(() -> new CpfCripto("")).hasMessageContaining("CPF_CHAVE");
        assertThatThrownBy(() -> new CpfCripto("YWJj")).hasMessageContaining("32 bytes");
    }

    @Test
    void nuloContinuaNulo() {
        assertThat(cripto.cifrar(null)).isNull();
        assertThat(cripto.decifrar(null)).isNull();
        assertThat(cripto.hash(null)).isNull();
    }

    /** A chave do exemplo do README, digitada no lugar de uma gerada. */
    static final String CHAVE_DE_EXEMPLO = "x".repeat(43) + "=";

    @Test
    void recusaChaveDeExemploComoChaveAtual() {
        assertThatThrownBy(() -> new CpfCripto(CHAVE_DE_EXEMPLO)).hasMessageContaining("exemplo");
        assertThatThrownBy(() -> new CpfCripto("A".repeat(43) + "=")).hasMessageContaining("exemplo");
    }

    @Test
    void aceitaChaveDeExemploComoChaveAntigaParaConseguirSairDela() {
        CpfCripto antiga = CpfCripto.daChaveAntiga(CHAVE_DE_EXEMPLO);
        assertThat(antiga.decifrar(antiga.cifrar("12345678900"))).isEqualTo("12345678900");
    }

    @Test
    void aceitaAChaveEntreAspas() {
        CpfCripto comAspas = new CpfCripto("\"" + CHAVE_TESTE + "\"");
        assertThat(comAspas.mesmaChaveQue(cripto)).isTrue();
        assertThat(comAspas.decifrar(cripto.cifrar("12345678900"))).isEqualTo("12345678900");
    }

    @Test
    void digitalDistingueChavesSemRevelalas() {
        assertThat(new CpfCripto(CHAVE_TESTE).mesmaChaveQue(cripto)).isTrue();
        assertThat(new CpfCripto("HyAhIiMkJSYnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4=").mesmaChaveQue(cripto)).isFalse();
    }

    @Test
    void mascaraMostraSoOMeio() {
        assertThat(Cpfs.mascarar("123.456.789-00")).isEqualTo("***.456.789-**");
        assertThat(Cpfs.mascarar("12345678900")).isEqualTo("***.456.789-**");
        assertThat(Cpfs.mascarar("123")).isEqualTo("***");
        assertThat(Cpfs.mascarar("")).isNull();
        assertThat(Cpfs.mascarar(null)).isNull();
    }
}
