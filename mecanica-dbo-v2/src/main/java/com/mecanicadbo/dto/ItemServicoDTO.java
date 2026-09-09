package com.mecanicadbo.dto;

import com.mecanicadbo.model.ItemServico;
import java.math.BigDecimal;

public record ItemServicoDTO(
        Long id,
        String descricao,
        BigDecimal valor
) {
    public static ItemServicoDTO de(ItemServico s) {
        return new ItemServicoDTO(s.getId(), s.getDescricao(), s.getValor());
    }
}
