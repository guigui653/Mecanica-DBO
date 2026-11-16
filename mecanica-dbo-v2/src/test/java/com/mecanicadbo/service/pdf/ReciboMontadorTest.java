package com.mecanicadbo.service.pdf;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReciboMontadorTest {

    private static final Map<String, String> OFICINA = Map.of("nome", "Mecânica DBO", "endereco", "", "telefone", "");

    @Test
    void dinheiroEmReaisComSinalAntesDoSimbolo() {
        assertThat(ReciboMontador.dinheiro(new BigDecimal("1234.5"))).isEqualTo("R$ 1.234,50");
        assertThat(ReciboMontador.dinheiro(new BigDecimal("-258.51"))).isEqualTo("-R$ 258,51");
        assertThat(ReciboMontador.dinheiro(null)).isEqualTo(ReciboMontador.VAZIO);
    }

    @Test
    void quantidadeSemZerosInuteis() {
        assertThat(ReciboMontador.quantidade(new BigDecimal("2.00"))).isEqualTo("2");
        assertThat(ReciboMontador.quantidade(new BigDecimal("1.50"))).isEqualTo("1,5");
        assertThat(ReciboMontador.quantidade(null)).isEqualTo(ReciboMontador.VAZIO);
    }

    @Test
    void dataInvalidaOuAusenteNaoDerrubaORecibo() {
        assertThat(ReciboMontador.data("2026-10-05")).isEqualTo("05/10/2026");
        assertThat(ReciboMontador.data(List.of(2026, 10, 5))).isEqualTo("05/10/2026");
        assertThat(ReciboMontador.data("lixo")).isEqualTo(ReciboMontador.VAZIO);
        assertThat(ReciboMontador.data(null)).isEqualTo(ReciboMontador.VAZIO);
    }

    @Test
    void osComCamposVaziosVirandoTracoECreditoNegativo() {
        Map<String, Object> cliente = new HashMap<>();
        cliente.put("nome", "Teste Dois");
        cliente.put("cpf", "12345678900");

        Map<String, Object> veiculo = new HashMap<>();
        veiculo.put("placa", "JEH3030");
        veiculo.put("combustivel", "HIBRIDO");
        veiculo.put("cliente", cliente);

        Map<String, Object> os = new HashMap<>();
        os.put("id", 3);
        os.put("status", "CONCLUIDA");
        os.put("veiculo", veiculo);
        os.put("reclamacoes", "Barulho\n\nna frente");
        os.put("itensPeca", List.of(
                Map.of("descricao", "Filtro", "quantidade", 2, "valorUnitario", 10, "valorTotal", 20, "pagoPeloCliente", false),
                Map.of("descricao", "Crédito mecânico", "quantidade", 1, "valorUnitario", -5, "valorTotal", -5, "pagoPeloCliente", false),
                Map.of("descricao", "Pneu do cliente", "quantidade", 1, "valorUnitario", 100, "valorTotal", 100, "pagoPeloCliente", true)));
        os.put("totalGeral", 15);

        Map<String, Object> r = ReciboMontador.montar(os, OFICINA, LocalDateTime.of(2026, 11, 14, 19, 30));

        @SuppressWarnings("unchecked") Map<String, Object> cli = (Map<String, Object>) r.get("cliente");
        @SuppressWarnings("unchecked") Map<String, Object> vei = (Map<String, Object>) r.get("veiculo");
        @SuppressWarnings("unchecked") List<Map<String, Object>> pecas = (List<Map<String, Object>>) r.get("pecas");

        assertThat(r.get("numero")).isEqualTo("#3");
        assertThat(r.get("status")).isEqualTo("Concluída");
        assertThat(cli.get("cpf")).isEqualTo("***.456.789-**");      // recibo vai para o cliente: sempre mascarado
        assertThat(cli.get("telefone")).isEqualTo(ReciboMontador.VAZIO);  // campo ausente não pode quebrar
        assertThat(vei.get("placa")).isEqualTo("JEH-3030");
        assertThat(vei.get("combustivel")).isEqualTo("Híbrido");
        assertThat(r.get("reclamacoes")).isEqualTo(List.of("Barulho", "na frente"));
        assertThat(pecas.get(1).get("valorTotal")).isEqualTo("-R$ 5,00");
        assertThat(r.get("temPagasPeloCliente")).isEqualTo(true);
        assertThat(r.get("totalPagoPeloCliente")).isEqualTo("R$ 100,00");
        assertThat(r.get("totalGeral")).isEqualTo("R$ 15,00");
        assertThat(r.get("emitidoEm")).isEqualTo("14/11/2026 às 19:30");
    }
}
