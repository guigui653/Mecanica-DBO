package com.mecanicadbo.config;

import com.mecanicadbo.service.TrocaChaveCpf;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Depois das migrações do Flyway, roda a troca da chave do CPF (se pedida)
 * e confere se a CPF_CHAVE abre os CPFs do banco.
 * Fica aqui porque o Hibernate só começa depois do Flyway: assim nenhum
 * cliente é carregado antes de os CPFs estarem na chave atual.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy migrarETrocarChaveCpf(TrocaChaveCpf trocaChave) {
        return flyway -> {
            flyway.migrate();
            trocaChave.executarSePedido();
            trocaChave.conferirChave();
        };
    }
}
