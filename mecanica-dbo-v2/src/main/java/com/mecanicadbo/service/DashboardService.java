package com.mecanicadbo.service;

import com.mecanicadbo.dto.DashboardResumoDTO;
import com.mecanicadbo.dto.OrdemServicoDTO;
import com.mecanicadbo.model.StatusOs;
import com.mecanicadbo.repository.ClienteRepository;
import com.mecanicadbo.repository.OrdemServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrdemServicoRepository osRepo;
    private final ClienteRepository clienteRepo;

    @Transactional(readOnly = true)
    public DashboardResumoDTO resumo(LocalDate hoje) {
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());

        List<OrdemServicoDTO> patio = osRepo
                .buscarPorStatusMaisAntigasPrimeiro(List.of(StatusOs.ABERTA, StatusOs.EM_ANDAMENTO))
                .stream().map(OrdemServicoDTO::resumo).toList();

        List<OrdemServicoDTO> prontas = osRepo
                .buscarPorStatusMaisAntigasPrimeiro(List.of(StatusOs.CONCLUIDA))
                .stream().map(OrdemServicoDTO::resumo).toList();

        return new DashboardResumoDTO(
                patio.size(),
                osRepo.countByStatus(StatusOs.ABERTA),
                osRepo.somarTotalPorStatusNoPeriodo(StatusOs.ENTREGUE, inicioMes, fimMes),
                clienteRepo.countByAtivoTrue(),
                patio,
                prontas,
                clienteRepo.findByAtivoTrueAndCriadoEmGreaterThanEqualOrderByCriadoEmDesc(inicioMes.atStartOfDay())
        );
    }
}
