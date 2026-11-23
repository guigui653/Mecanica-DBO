package com.mecanicadbo.service;

import com.mecanicadbo.dto.DashboardResumoDTO;
import com.mecanicadbo.dto.OrdemServicoDTO;
import com.mecanicadbo.model.StatusOs;
import com.mecanicadbo.repository.ClienteRepository;
import com.mecanicadbo.repository.OrdemServicoRepository;
import com.mecanicadbo.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrdemServicoRepository osRepo;
    private final ClienteRepository clienteRepo;
    private final VeiculoRepository veiculoRepo;

    /** Meses no gráfico de faturamento do painel. */
    static final int MESES_GRAFICO = 6;

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
                clienteRepo.findByAtivoTrueAndCriadoEmGreaterThanEqualOrderByCriadoEmDesc(inicioMes.atStartOfDay()),
                faturamentoMensal(inicioMes),
                osPorStatus(),
                osRepo.buscarMaisRecentes(PageRequest.of(0, 5)).stream().map(OrdemServicoDTO::resumo).toList(),
                veiculoRepo.countByAtivoTrue()
        );
    }

    /** Faturamento dos últimos meses (o atual incluído), do mais antigo para o mais novo. */
    private List<DashboardResumoDTO.FaturamentoMes> faturamentoMensal(LocalDate inicioMesAtual) {
        List<DashboardResumoDTO.FaturamentoMes> meses = new ArrayList<>();
        for (int i = MESES_GRAFICO - 1; i >= 0; i--) {
            LocalDate inicio = inicioMesAtual.minusMonths(i);
            LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());
            meses.add(new DashboardResumoDTO.FaturamentoMes(inicio.getYear(), inicio.getMonthValue(),
                    osRepo.somarTotalPorStatusNoPeriodo(StatusOs.ENTREGUE, inicio, fim)));
        }
        return meses;
    }

    private Map<StatusOs, Long> osPorStatus() {
        Map<StatusOs, Long> contagem = new EnumMap<>(StatusOs.class);
        for (StatusOs s : StatusOs.values()) {
            contagem.put(s, osRepo.countByStatus(s));
        }
        return contagem;
    }
}
