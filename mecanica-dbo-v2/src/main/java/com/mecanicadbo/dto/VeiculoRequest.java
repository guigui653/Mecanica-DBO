package com.mecanicadbo.dto;

import jakarta.validation.constraints.*;

/**
 * Dados aceitos no cadastro e na edição de veículo.
 * Sem id, sem clienteId (vem do path), sem ativo e sem criadoEm.
 */
public record VeiculoRequest(

        @NotBlank(message = "Placa é obrigatória")
        @Size(max = 10, message = "Placa deve ter no máximo 10 caracteres")
        String placa,

        @NotBlank(message = "Marca é obrigatória")
        @Size(max = 60, message = "Marca deve ter no máximo 60 caracteres")
        String marca,

        @NotBlank(message = "Modelo é obrigatório")
        @Size(max = 80, message = "Modelo deve ter no máximo 80 caracteres")
        String modelo,

        @Size(max = 20, message = "Chassi deve ter no máximo 20 caracteres")
        String chassi,

        @NotBlank(message = "Cor é obrigatória")
        @Size(max = 40, message = "Cor deve ter no máximo 40 caracteres")
        String cor,

        /*
         * CONFIRA ESTA LISTA CONTRA O SEU BANCO ANTES DE USAR EM PRODUÇÃO.
         *
         * No DataGrip / pgAdmin, rode:
         *
         *   SELECT conname, pg_get_constraintdef(oid)
         *   FROM   pg_constraint
         *   WHERE  conrelid = 'veiculo'::regclass AND contype = 'c';
         *
         * Se o CHECK do banco usar outros rótulos, ajuste o regexp abaixo
         * para bater exatamente com ele. O objetivo aqui é o erro voltar
         * como 400 "Combustível inválido" (mensagem que o usuário entende)
         * em vez de 500 vindo do CHECK do PostgreSQL.
         *
         * O VeiculoService converte para maiúsculas antes de salvar, por
         * isso o (?i) — aceita "flex" digitado pelo mecânico.
         */
        @NotBlank(message = "Combustível é obrigatório")
        @Pattern(
                regexp = "(?i)GASOLINA|ETANOL|FLEX|DIESEL|ELETRICO|GNV",
                message = "Combustível inválido. Use: GASOLINA, ETANOL, FLEX, DIESEL, ELETRICO ou GNV"
        )
        String combustivel,

        @Min(value = 1950, message = "Ano de fabricação inválido")
        @Max(value = 2100, message = "Ano de fabricação inválido")
        Short anoFabricacao
) {}
