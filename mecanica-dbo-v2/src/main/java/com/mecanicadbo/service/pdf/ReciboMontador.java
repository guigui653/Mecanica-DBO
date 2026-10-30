package com.mecanicadbo.service.pdf;

import com.mecanicadbo.service.Placas;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Transforma o JSON da OS (o mesmo que o Angular recebe em GET /api/ordens/{id})
 * nos textos prontos que o template recibo.html imprime.
 *
 * POR QUE EXISTE
 * --------------
 * Toda a formatação e todo o tratamento de nulo ficam aqui, em Java puro, e o
 * template só faz th:text. Foi exatamente a falta disso que derrubava o recibo
 * em Python: um CPF nulo virava erro 500. Aqui, campo ausente vira "—" e pronto.
 *
 * Esta classe não depende de Spring, Thymeleaf nem Flying Saucer. Pode ser
 * testada com um main() ou JUnit sem subir nada.
 *
 * O resultado é um Map porque o Thymeleaf lê Map sem configuração nenhuma
 * (${r.cliente.nome}), enquanto records exigem cuidado com o acesso por nome.
 */
public final class ReciboMontador {

    public static final String VAZIO = "—";

    private static final DecimalFormatSymbols PT_BR = new DecimalFormatSymbols(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private ReciboMontador() {}

    // ── Entrada principal ─────────────────────────────────────

    /**
     * @param os      a OS como Map (Jackson convertValue do OrdemServicoDTO)
     * @param oficina nome, endereco e telefone da oficina (campos podem ser vazios)
     * @param agora   instante da emissão (parâmetro para o teste ser previsível)
     */
    public static Map<String, Object> montar(Map<String, Object> os,
                                             Map<String, String> oficina,
                                             LocalDateTime agora) {
        Map<String, Object> veiculo = mapa(os.get("veiculo"));
        Map<String, Object> cliente = mapa(veiculo.get("cliente"));

        Map<String, Object> r = new LinkedHashMap<>();

        // Oficina: endereço e telefone podem ser nulos (o template esconde a linha).
        Map<String, Object> of = new LinkedHashMap<>();
        of.put("nome", textoOu(oficina.get("nome"), "Mecânica DBO"));
        of.put("endereco", vazioParaNulo(oficina.get("endereco")));
        of.put("telefone", vazioParaNulo(oficina.get("telefone")));
        r.put("oficina", of);

        r.put("numero", os.get("id") == null ? VAZIO : "#" + os.get("id"));
        r.put("status", rotuloStatus(texto(os.get("status"), "")));
        r.put("dataEntrada", data(os.get("dataEntrada")));
        r.put("dataSaidaPrevista", data(os.get("dataSaidaPrevista")));
        r.put("dataSaidaReal", data(os.get("dataSaidaReal")));
        r.put("km", km(os.get("kmEntrada")));

        Map<String, Object> cli = new LinkedHashMap<>();
        cli.put("nome", texto(cliente.get("nome")));
        cli.put("cpf", texto(cliente.get("cpf")));
        cli.put("telefone", texto(cliente.get("telefone1")));
        r.put("cliente", cli);

        Map<String, Object> vei = new LinkedHashMap<>();
        vei.put("placa", Placas.formatar(texto(veiculo.get("placa")).toUpperCase(Locale.ROOT)));
        vei.put("marcaModelo", juntar(veiculo.get("marca"), veiculo.get("modelo")));
        vei.put("cor", texto(veiculo.get("cor")));
        vei.put("combustivel", rotuloCombustivel(texto(veiculo.get("combustivel"), "")));
        vei.put("ano", texto(veiculo.get("anoFabricacao")));
        r.put("veiculo", vei);

        r.put("reclamacoes", linhas(os.get("reclamacoes")));
        r.put("diagnostico", linhas(os.get("diagnostico")));

        // Peças
        List<Map<String, Object>> pecas = new ArrayList<>();
        BigDecimal pagoPeloCliente = BigDecimal.ZERO;
        int i = 0;
        for (Object o : lista(os.get("itensPeca"))) {
            Map<String, Object> p = mapa(o);
            BigDecimal qtd = numero(p.get("quantidade"));
            BigDecimal unit = numero(p.get("valorUnitario"));
            BigDecimal total = numero(p.get("valorTotal"));
            // O banco calcula valorTotal; se por algum motivo não vier, calcula aqui só para exibir.
            if (total == null && qtd != null && unit != null) total = qtd.multiply(unit);
            boolean paga = Boolean.TRUE.equals(p.get("pagoPeloCliente"));
            if (paga && total != null) pagoPeloCliente = pagoPeloCliente.add(total);

            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("descricao", texto(p.get("descricao")));
            linha.put("quantidade", quantidade(qtd));
            linha.put("valorUnitario", dinheiro(unit));
            linha.put("valorTotal", dinheiro(total));
            linha.put("paga", paga);
            linha.put("classe", (i++ % 2 == 1) ? "alt" : "");
            pecas.add(linha);
        }
        r.put("pecas", pecas);
        r.put("temPecas", !pecas.isEmpty());
        r.put("semPecas", pecas.isEmpty());

        // Serviços
        List<Map<String, Object>> servicos = new ArrayList<>();
        i = 0;
        for (Object o : lista(os.get("itensServico"))) {
            Map<String, Object> s = mapa(o);
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("descricao", texto(s.get("descricao")));
            linha.put("valor", dinheiro(numero(s.get("valor"))));
            linha.put("classe", (i++ % 2 == 1) ? "alt" : "");
            servicos.add(linha);
        }
        r.put("servicos", servicos);
        r.put("temServicos", !servicos.isEmpty());
        r.put("semServicos", servicos.isEmpty());

        // Totais: quem calcula é a trigger do banco. Aqui só formatamos.
        r.put("totalPecas", dinheiro(numero(os.get("totalPecas"))));
        r.put("totalServicos", dinheiro(numero(os.get("totalServicos"))));
        r.put("totalGeral", dinheiro(numero(os.get("totalGeral"))));
        r.put("temPagasPeloCliente", pagoPeloCliente.signum() != 0);
        r.put("totalPagoPeloCliente", dinheiro(pagoPeloCliente));

        r.put("emitidoEm", (agora == null ? LocalDateTime.now() : agora).format(DATA_HORA));
        return r;
    }

    // ── Formatação ────────────────────────────────────────────

    /** 1234.5 -> "R$ 1.234,50"; -258.51 -> "-R$ 258,51"; null -> "—". */
    public static String dinheiro(BigDecimal v) {
        if (v == null) return VAZIO;
        DecimalFormat f = new DecimalFormat("#,##0.00", PT_BR);
        String abs = f.format(v.abs());
        return (v.signum() < 0 ? "-R$ " : "R$ ") + abs;
    }

    /** 2 -> "2"; 1.5 -> "1,5"; null -> "—". */
    public static String quantidade(BigDecimal v) {
        if (v == null) return VAZIO;
        BigDecimal sem = v.stripTrailingZeros();
        if (sem.scale() < 0) sem = sem.setScale(0);
        return new DecimalFormat("#,##0.##", PT_BR).format(sem);
    }

    private static String km(Object v) {
        BigDecimal n = numero(v);
        if (n == null) return VAZIO;
        return new DecimalFormat("#,##0", PT_BR).format(n) + " km";
    }

    /** Aceita "2026-10-05", "2026-10-05T14:30:00" ou [2026,10,5]. Qualquer outra coisa vira "—". */
    static String data(Object v) {
        if (v == null) return VAZIO;
        try {
            if (v instanceof List<?> l && l.size() >= 3) {
                return LocalDate.of(inteiro(l.get(0)), inteiro(l.get(1)), inteiro(l.get(2))).format(DATA);
            }
            String s = v.toString().trim();
            if (s.length() >= 10) return LocalDate.parse(s.substring(0, 10)).format(DATA);
        } catch (RuntimeException ignorado) {
            // cai no traço abaixo: data inválida não pode derrubar o recibo
        }
        return VAZIO;
    }

    private static int inteiro(Object o) {
        return ((Number) o).intValue();
    }

    private static String rotuloStatus(String s) {
        return switch (s) {
            case "ABERTA" -> "Aberta";
            case "EM_ANDAMENTO" -> "Em andamento";
            case "CONCLUIDA" -> "Concluída";
            case "ENTREGUE" -> "Entregue";
            default -> s.isBlank() ? VAZIO : s;
        };
    }

    private static String rotuloCombustivel(String s) {
        return switch (s) {
            case "GASOLINA" -> "Gasolina";
            case "ETANOL" -> "Etanol";
            case "FLEX" -> "Flex";
            case "DIESEL" -> "Diesel";
            case "ELETRICO" -> "Elétrico";
            case "HIBRIDO" -> "Híbrido";
            case "GNV" -> "GNV";
            default -> s.isBlank() ? VAZIO : s;
        };
    }

    // ── Conversões seguras ────────────────────────────────────

    /** null ou em branco vira "—". Sem escape: o Thymeleaf escapa no th:text. */
    static String texto(Object v) {
        return texto(v, VAZIO);
    }

    static String texto(Object v, String vazio) {
        if (v == null) return vazio;
        String s = v.toString().trim();
        return s.isEmpty() ? vazio : s;
    }

    private static String textoOu(String v, String padrao) {
        return (v == null || v.isBlank()) ? padrao : v.trim();
    }

    private static String vazioParaNulo(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static String juntar(Object a, Object b) {
        String x = texto(a, ""), y = texto(b, "");
        String s = (x + " " + y).trim();
        return s.isEmpty() ? VAZIO : s;
    }

    /** Quebra o texto em linhas (cada uma vira um parágrafo). Vazio vira uma linha com "—". */
    static List<String> linhas(Object v) {
        List<String> out = new ArrayList<>();
        if (v != null) {
            for (String l : v.toString().split("\\R")) {
                if (!l.isBlank()) out.add(l.trim());
            }
        }
        if (out.isEmpty()) out.add(VAZIO);
        return out;
    }

    static BigDecimal numero(Object v) {
        if (v == null) return null;
        try {
            return (v instanceof BigDecimal b) ? b : new BigDecimal(v.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object o) {
        return (o instanceof Map<?, ?> m) ? (Map<String, Object>) m : Map.of();
    }

    private static List<?> lista(Object o) {
        return (o instanceof List<?> l) ? l : List.of();
    }
}
