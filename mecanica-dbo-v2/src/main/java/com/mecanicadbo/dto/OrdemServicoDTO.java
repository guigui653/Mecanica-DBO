package com.mecanicadbo.dto;

import com.mecanicadbo.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OrdemServicoDTO(
        Long id,
        StatusOs status,
        Integer kmEntrada,
        LocalDate dataEntrada,
        LocalDate dataSaidaPrevista,
        LocalDate dataSaidaReal,
        String reclamacoes,
        String diagnostico,
        BigDecimal totalPecas,
        BigDecimal totalServicos,
        BigDecimal totalGeral,
        VeiculoResumoDTO veiculo,
        List<ItemPecaDTO> itensPeca,
        List<ItemServicoDTO> itensServico
) {

    /** Versão completa: inclui veículo, cliente e itens. Usada em GET /ordens/{id}. */
    public static OrdemServicoDTO completo(
            OrdemServico os,
            List<ItemPeca> pecas,
            List<ItemServico> servicos
    ) {
        return new OrdemServicoDTO(
                os.getId(),
                os.getStatus(),
                os.getKmEntrada(),
                os.getDataEntrada(),
                os.getDataSaidaPrevista(),
                os.getDataSaidaReal(),
                os.getReclamacoes(),
                os.getDiagnostico(),
                os.getTotalPecas(),
                os.getTotalServicos(),
                os.getTotalGeral(),
                VeiculoResumoDTO.de(os.getVeiculo()),
                pecas.stream().map(ItemPecaDTO::de).toList(),
                servicos.stream().map(ItemServicoDTO::de).toList()
        );
    }

    /** Versão resumida: veículo e cliente, sem itens. Usada nas listagens. */
    public static OrdemServicoDTO resumo(OrdemServico os) {
        return new OrdemServicoDTO(
                os.getId(),
                os.getStatus(),
                os.getKmEntrada(),
                os.getDataEntrada(),
                os.getDataSaidaPrevista(),
                os.getDataSaidaReal(),
                os.getReclamacoes(),
                os.getDiagnostico(),
                os.getTotalPecas(),
                os.getTotalServicos(),
                os.getTotalGeral(),
                VeiculoResumoDTO.de(os.getVeiculo()),
                null,
                null
        );
    }
}
