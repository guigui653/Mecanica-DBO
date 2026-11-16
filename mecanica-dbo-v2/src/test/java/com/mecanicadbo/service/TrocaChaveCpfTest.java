package com.mecanicadbo.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrocaChaveCpfTest {

    private final CpfCripto nova = new CpfCripto(CpfCriptoTest.CHAVE_TESTE);
    private final CpfCripto antiga = CpfCripto.daChaveAntiga(CpfCriptoTest.CHAVE_DE_EXEMPLO);
    private final TrocaChaveCpf troca = new TrocaChaveCpf(nova, CpfCriptoTest.CHAVE_DE_EXEMPLO, null);

    @Test
    void cpfDaChaveAntigaEAbertoParaSerTrancadoDeNovo() {
        var r = new TrocaChaveCpf.Resultado();
        assertThat(troca.abrir(1L, antiga.cifrar("12345678900"), antiga, r)).isEqualTo("12345678900");
        assertThat(r.trocados).isEqualTo(1);
    }

    @Test
    void cpfJaNaChaveNovaFicaComoEsta() {
        var r = new TrocaChaveCpf.Resultado();
        assertThat(troca.abrir(1L, nova.cifrar("12345678900"), antiga, r)).isNull();
        assertThat(r.jaNaChaveNova).isEqualTo(1);
        assertThat(r.trocados).isZero();
    }

    @Test
    void cpfQueNaoAbreComNenhumaChaveCancelaTudo() {
        CpfCripto terceira = new CpfCripto("HyAhIiMkJSYnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4=");
        assertThatThrownBy(() -> troca.abrir(7L, terceira.cifrar("12345678900"), antiga, new TrocaChaveCpf.Resultado()))
                .hasMessageContaining("id 7").hasMessageContaining("Nada foi alterado");
    }
}
