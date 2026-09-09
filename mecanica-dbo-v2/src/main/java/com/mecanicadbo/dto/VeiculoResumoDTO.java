package com.mecanicadbo.dto;

import com.mecanicadbo.model.Veiculo;

public record VeiculoResumoDTO(
        Long id,
        String placa,
        String marca,
        String modelo,
        String cor,
        String combustivel,
        String chassi,
        Short anoFabricacao,
        ClienteResumoDTO cliente
) {
    public static VeiculoResumoDTO de(Veiculo v) {
        if (v == null) return null;
        return new VeiculoResumoDTO(
                v.getId(),
                v.getPlaca(),
                v.getMarca(),
                v.getModelo(),
                v.getCor(),
                v.getCombustivel(),
                v.getChassi(),
                v.getAnoFabricacao(),
                ClienteResumoDTO.de(v.getCliente())
        );
    }
}
