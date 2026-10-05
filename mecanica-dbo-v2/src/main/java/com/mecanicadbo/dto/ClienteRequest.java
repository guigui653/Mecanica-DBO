package com.mecanicadbo.dto;

import jakarta.validation.constraints.*;

/**
 * Dados aceitos no cadastro e na edição de cliente.
 *
 * Sem id (evita merge), sem ativo (muda só pelo DELETE /clientes/{id})
 * e sem criadoEm (definido pelo @PrePersist).
 *
 * O CPF é opcional. Quando vier vazio ("" ou só espaços), o ClienteService
 * converte para null antes de salvar — senão o segundo cliente sem CPF
 * colide no índice único da coluna cpf e o cadastro falha em silêncio.
 * Era essa a causa do "cadastrei o cliente e ele não aparece na lista".
 */
public record ClienteRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String nome,

        @Size(max = 14, message = "CPF deve ter no máximo 14 caracteres")
        String cpf,

        @NotBlank(message = "Telefone é obrigatório")
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String telefone1,

        @Size(max = 20, message = "Telefone 2 deve ter no máximo 20 caracteres")
        String telefone2,

        @Email(message = "E-mail inválido")
        @Size(max = 100, message = "E-mail deve ter no máximo 100 caracteres")
        String email
) {}
