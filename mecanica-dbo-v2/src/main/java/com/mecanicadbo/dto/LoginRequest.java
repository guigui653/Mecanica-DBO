package com.mecanicadbo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Informe o usuário") @Size(max = 100) String username,
        @NotBlank(message = "Informe a senha") @Size(max = 200) String password) {
}
