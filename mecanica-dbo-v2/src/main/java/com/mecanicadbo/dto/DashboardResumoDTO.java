package com.mecanicadbo.dto;

import com.mecanicadbo.model.StatusOs;
import java.util.Map;

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
 * @param faturamentoMensal faturamento (OS entregues) dos últimos 6 meses, do mais antigo ao atual
 * @param osPorStatus     quantas OS há em cada situação (ABERTA, EM_ANDAMENTO, CONCLUIDA, ENTREGUE)
 * @param ultimasOs       as 5 OS mais recentes
 * @param totalVeiculos   veículos ativos
 */
public record DashboardResumoDTO(
        long noPatio,
        long osAbertas,
        BigDecimal faturamentoMes,
        long totalClientes,
        List<OrdemServicoDTO> patio,
        List<OrdemServicoDTO> prontas,
        List<Cliente> clientesDoMes,
        List<FaturamentoMes> faturamentoMensal,
        Map<StatusOs, Long> osPorStatus,
        List<OrdemServicoDTO> ultimasOs,
        long totalVeiculos
) {
    /** Um mês do gráfico: ano, mês (1 a 12) e total faturado. */
    public record FaturamentoMes(int ano, int mes, BigDecimal total) {}
}
