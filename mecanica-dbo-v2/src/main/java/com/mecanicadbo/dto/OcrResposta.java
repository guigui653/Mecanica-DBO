package com.mecanicadbo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resposta do microserviço Python (ocr_service.py), já em camelCase para o Angular.
 *
 * O Python fala snake_case (valor_unitario, texto_bruto...). @JsonAlias aceita o
 * nome do Python na LEITURA e a saída para o front sai em camelCase — por isso
 * não se usa @JsonProperty aqui (ele trocaria os dois sentidos).
 *
 * Nada disto é gravado no banco: são sugestões que o mecânico revisa na tela.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OcrResposta(
        Boolean sucesso,
        Metadados metadados,
        List<Item> itens,
        @JsonAlias("texto_bruto") String textoBruto,
        String mensagem
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String descricao,
            BigDecimal quantidade,
            @JsonAlias("valor_unitario") BigDecimal valorUnitario,
            @JsonAlias("valor_total") BigDecimal valorTotal
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadados(
            String fornecedor,
            @JsonAlias("cnpj_fornecedor") String cnpjFornecedor,
            @JsonAlias("numero_nf") String numeroNf,
            @JsonAlias("data_emissao") String dataEmissao,
            @JsonAlias("valor_total_nf") BigDecimal valorTotalNf
    ) {}
}
