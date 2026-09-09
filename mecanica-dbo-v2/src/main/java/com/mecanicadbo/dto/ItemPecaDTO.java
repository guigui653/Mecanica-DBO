package com.mecanicadbo.dto;

import com.mecanicadbo.model.ItemPeca;
import java.math.BigDecimal;

public record ItemPecaDTO(
        Long id,
        String descricao,
        BigDecimal quantidade,
        BigDecimal valorUnitario,
        BigDecimal valorTotal,
        Boolean pagoPeloCliente
) {
    public static ItemPecaDTO de(ItemPeca p) {
        return new ItemPecaDTO(
                p.getId(),
                p.getDescricao(),
                p.getQuantidade(),
                p.getValorUnitario(),
                p.getValorTotal(),
                p.getPagoPeloCliente()
        );
    }
}
