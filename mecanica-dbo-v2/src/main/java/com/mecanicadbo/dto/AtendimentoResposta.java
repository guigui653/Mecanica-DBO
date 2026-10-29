package com.mecanicadbo.dto;

/**
 * Resposta do POST /api/atendimentos.
 *
 * veiculo vem sempre (com o cliente aninhado). os vem só quando a OS foi
 * aberta, no mesmo formato do GET /api/ordens/{id}; sem OS, o campo some do
 * JSON (default-property-inclusion=non_null).
 */
public record AtendimentoResposta(
        VeiculoResumoDTO veiculo,
        OrdemServicoDTO os
) {}
