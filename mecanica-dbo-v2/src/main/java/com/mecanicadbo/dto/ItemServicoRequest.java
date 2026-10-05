package com.mecanicadbo.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Dados aceitos ao adicionar um serviço (mão de obra) à OS.
 * Sem id e sem osId — mesmo motivo do ItemPecaRequest.
 */
public record ItemServicoRequest(

        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 200, message = "Descrição deve ter no máximo 200 caracteres")
        String descricao,

        @NotNull(message = "Valor é obrigatório")
        @PositiveOrZero(message = "Valor do serviço não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Valor inválido")
        BigDecimal valor
) {}
