package com.mecanicadbo.dto;

import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/**
 * Dados aceitos na criação de uma OS.
 *
 * IMPORTANTE — este record NÃO tem os campos id, status, totalPecas,
 * totalServicos, totalGeral nem veiculo. Isso é proposital:
 *
 *  - id      → se viesse preenchido, o JpaRepository.save() faria MERGE
 *              (UPDATE numa OS existente) em vez de INSERT. Era o furo
 *              mais grave do backend: um POST podia sobrescrever uma OS
 *              antiga inteira, zerando os campos não enviados.
 *  - status  → sempre começa em ABERTA; muda só pelo PATCH /status.
 *  - totais  → calculados por trigger no PostgreSQL, nunca pelo Java.
 *  - veiculo → vem do path (/api/ordens/veiculo/{veiculoId}).
 *
 * Como o campo simplesmente não existe aqui, o Jackson ignora o que vier
 * no JSON. A proteção é estrutural, não depende de ninguém lembrar de
 * validar.
 */
public record CriarOsRequest(

        @PositiveOrZero(message = "KM de entrada não pode ser negativo")
        Integer kmEntrada,

        /** Se vier null, o banco/entidade usa a data de hoje. */
        LocalDate dataEntrada,

        LocalDate dataSaidaPrevista,

        String reclamacoes,

        String diagnostico
) {}
