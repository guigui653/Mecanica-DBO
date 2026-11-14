package com.mecanicadbo.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlacasTest {

    @Test
    void normalizaParaSoLetrasENumerosEmMaiusculas() {
        assertThat(Placas.normalizar(" jeh-3030 ")).isEqualTo("JEH3030");
        assertThat(Placas.normalizar("JEH 3030")).isEqualTo("JEH3030");
        assertThat(Placas.normalizar("abc1d23")).isEqualTo("ABC1D23");
        assertThat(Placas.normalizar(null)).isNull();
    }

    @Test
    void formataComHifenSoOPadraoAntigo() {
        assertThat(Placas.formatar("JEH3030")).isEqualTo("JEH-3030");
        assertThat(Placas.formatar("jeh-3030")).isEqualTo("JEH-3030");
        assertThat(Placas.formatar("ABC1D23")).isEqualTo("ABC1D23");
        assertThat(Placas.formatar("TESTE001")).isEqualTo("TESTE001");
    }

    @Test
    void termoDeBuscaSemNadaAproveitavelNaoViraCuringa() {
        assertThat(Placas.paraBusca("jeh-30")).isEqualTo("JEH30");
        // "-" normalizado ficaria vazio, e LIKE '%%' acharia todos os carros.
        assertThat(Placas.paraBusca("-")).isEqualTo("-");
    }
}
