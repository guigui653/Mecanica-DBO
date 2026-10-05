package com.mecanicadbo.dto;

import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/**
 * Campos editáveis de uma OS pela tela de detalhe.
 *
 * Status, totais e veículo não entram aqui — status tem endpoint próprio
 * (com regra de transição), totais são recalculados por trigger no banco e
 * o veículo é definido na criação.
 *
 * CONVENÇÃO DE ATUALIZAÇÃO PARCIAL:
 *   campo null      → não mexe no valor que já está gravado
 *   campo ""        → limpa o campo de texto
 *
 * Antes, os quatro setters rodavam sempre: um PUT mandando só o
 * diagnóstico apagava km, previsão de saída e reclamações da OS.
 */
public record AtualizarOsRequest(

        @PositiveOrZero(message = "KM de entrada não pode ser negativo")
        Integer kmEntrada,

        LocalDate dataSaidaPrevista,

        String reclamacoes,

        String diagnostico
) {}
