package com.mecanicadbo.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentosTest {

    @Test
    void cpfComDigitosVerificadoresCertos() {
        assertThat(Documentos.cpfValido("52998224725")).isTrue();
        assertThat(Documentos.cpfValido(Documentos.limpar("529.982.247-25"))).isTrue();
        assertThat(Documentos.cpfValido("52998224724")).isFalse();   // último dígito errado
        assertThat(Documentos.cpfValido("11111111111")).isFalse();   // sequência repetida
        assertThat(Documentos.cpfValido("5299822472")).isFalse();    // 10 números
    }

    @Test
    void cnpjSoDeNumerosComoAntes() {
        assertThat(Documentos.cnpjValido("11222333000181")).isTrue();
        assertThat(Documentos.cnpjValido(Documentos.limpar("11.222.333/0001-81"))).isTrue();
        assertThat(Documentos.cnpjValido("11222333000182")).isFalse();
        assertThat(Documentos.cnpjValido("00000000000000")).isFalse();
    }

    @Test
    void cnpjAlfanumericoDaReceita() {
        // Exemplo oficial da Receita Federal para o CNPJ alfanumérico.
        assertThat(Documentos.cnpjValido(Documentos.limpar("12.ABC.345/01DE-35"))).isTrue();
        assertThat(Documentos.cnpjValido(Documentos.limpar("12.abc.345/01de-35"))).isTrue();   // minúsculas
        assertThat(Documentos.cnpjValido("12ABC34501DE36")).isFalse();
        assertThat(Documentos.cnpjValido("12ABC34501DEAB")).isFalse();   // dígitos verificadores são números
    }

    @Test
    void tipoFormatoEMascara() {
        assertThat(Documentos.tipo("52998224725")).isEqualTo(Documentos.Tipo.CPF);
        assertThat(Documentos.tipo("12ABC34501DE35")).isEqualTo(Documentos.Tipo.CNPJ);
        assertThat(Documentos.tipo("123")).isNull();

        assertThat(Documentos.formatar("52998224725")).isEqualTo("529.982.247-25");
        assertThat(Documentos.formatar("12ABC34501DE35")).isEqualTo("12.ABC.345/01DE-35");

        assertThat(Documentos.mascarar("52998224725")).isEqualTo("***.982.247-**");   // CPF: dado pessoal
        assertThat(Documentos.mascarar("11222333000181")).isEqualTo("11.222.333/0001-81");   // CNPJ: público
        assertThat(Documentos.mascarar("")).isNull();
        assertThat(Documentos.rotulo("11.222.333/0001-81")).isEqualTo("CNPJ");
        assertThat(Documentos.rotulo("529.982.247-25")).isEqualTo("CPF");
    }
}
