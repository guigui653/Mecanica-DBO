package com.mecanicadbo.dto;

import jakarta.validation.Valid;

/**
 * Cadastro de cliente, veículo e OS numa chamada só (POST /api/atendimentos).
 *
 * Combinações aceitas (o AtendimentoService confere):
 *  - veiculoId + os                               → só abre a OS
 *  - clienteId  + novoVeiculo [+ os]              → veículo novo de cliente existente
 *  - novoCliente + novoVeiculo [+ os]             → tudo novo
 *
 * Sem os, grava só cliente/veículo (é o que o cadastro rápido do painel usa).
 * Os DTOs aninhados são os mesmos dos endpoints antigos, com as mesmas
 * validações, e nenhum deles tem id.
 */
public record AtendimentoRequest(

        Long clienteId,

        @Valid
        ClienteRequest novoCliente,

        Long veiculoId,

        @Valid
        VeiculoRequest novoVeiculo,

        @Valid
        CriarOsRequest os
) {}
