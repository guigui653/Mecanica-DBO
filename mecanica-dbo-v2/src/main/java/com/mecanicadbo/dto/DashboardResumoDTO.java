package com.mecanicadbo.dto;

import com.mecanicadbo.model.Cliente;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tudo que o painel e o menu lateral mostram, numa resposta só e já contado
 * pelo banco — em vez de o navegador baixar todas as OS e todos os clientes.
 *
 * @param noPatio         carros na oficina (ABERTA + EM_ANDAMENTO)
 * @param osAbertas       OS ABERTA (aguardando atendimento)
 * @param faturamentoMes  soma das OS ENTREGUE no mês corrente
 * @param totalClientes   clientes ativos
 * @param patio           OS no pátio, da mais antiga para a mais nova
 * @param prontas         OS CONCLUIDA (ligar para o cliente buscar)
 * @param clientesDoMes   clientes cadastrados no mês corrente
 */
public record DashboardResumoDTO(
        long noPatio,
        long osAbertas,
        BigDecimal faturamentoMes,
        long totalClientes,
        List<OrdemServicoDTO> patio,
        List<OrdemServicoDTO> prontas,
        List<Cliente> clientesDoMes
) {}
