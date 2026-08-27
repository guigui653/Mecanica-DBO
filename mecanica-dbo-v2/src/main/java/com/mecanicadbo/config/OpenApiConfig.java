package com.mecanicadbo.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Mecânica DBO — API")
                .description("Sistema de Gestão de Ordens de Serviço")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Guilherme")
                    .email("dev@mecanicadbo.com.br"))
            );
    }
}
