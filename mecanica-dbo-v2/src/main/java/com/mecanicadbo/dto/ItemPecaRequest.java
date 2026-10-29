package com.mecanicadbo.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Dados aceitos ao adicionar uma peça à OS.
 *
 * Sem id (evita merge/UPDATE disfarçado de POST), sem osId (vem do path)
 * e sem valorTotal (é coluna GENERATED ALWAYS AS ... STORED no PostgreSQL —
 * quem calcula é o banco).
 *
 * valorUnitario NÃO tem @PositiveOrZero. Isso é intencional: a oficina
 * lança linhas de "Crédito mecânico" com valor negativo (ex.: -258,51).
 * Era exatamente isso que travava no cadastro pela tela.
 */
public record ItemPecaRequest(

        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 200, message = "Descrição deve ter no máximo 200 caracteres")
        String descricao,

        /** Se vier null, assume 1. */
        @Positive(message = "Quantidade deve ser maior que zero")
        @Digits(integer = 6, fraction = 2, message = "Quantidade inválida")
        BigDecimal quantidade,

        @NotNull(message = "Valor unitário é obrigatório")
        @Digits(integer = 8, fraction = 2, message = "Valor unitário inválido")
        BigDecimal valorUnitario,

        /** Se vier null, assume false. */
        Boolean pagoPeloCliente
) {}
