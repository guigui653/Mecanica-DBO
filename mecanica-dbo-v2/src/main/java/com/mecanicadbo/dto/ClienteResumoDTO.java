package com.mecanicadbo.dto;

import com.mecanicadbo.model.Cliente;

public record ClienteResumoDTO(
        Long id,
        String nome,
        String cpf,
        String telefone1
) {
    public static ClienteResumoDTO de(Cliente c) {
        if (c == null) return null;
        return new ClienteResumoDTO(
                c.getId(), c.getNome(), c.getCpf(), c.getTelefone1()
        );
    }
}
