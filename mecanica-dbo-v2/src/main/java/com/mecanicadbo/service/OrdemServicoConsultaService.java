package com.mecanicadbo.service;

import com.mecanicadbo.dto.OrdemServicoDTO;
import com.mecanicadbo.exception.RecursoNaoEncontradoException;
import com.mecanicadbo.exception.RegraDeNegocioException;
import com.mecanicadbo.model.*;
import com.mecanicadbo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Consultas de OS que retornam DTOs.
 *
 * As entidades têm @JsonIgnore nos relacionamentos para evitar referência
 * circular, então a montagem acontece aqui dentro da transação — se fosse
 * feita no controller, o lazy loading falharia com LazyInitializationException.
 *
 * O QUE MUDOU
 * -----------
 * Sumiram os .peek(os -> os.getVeiculo().getCliente().getNome()). Eles
 * existiam para forçar a carga do proxy lazy, e era exatamente esse peek que
 * disparava o N+1: para cada OS da lista, dois SELECTs extras.
 *
 * Agora as consultas do OrdemServicoRepository fazem JOIN FETCH do veículo e
 * do cliente, então tudo vem numa query só e o peek deixou de ser necessário.
 */
@Service
@RequiredArgsConstructor
public class OrdemServicoConsultaService {

    private final OrdemServicoRepository osRepo;
    private final ItemPecaRepository itemPecaRepo;
    private final ItemServicoRepository itemServicoRepo;

    @Transactional(readOnly = true)
    public OrdemServicoDTO buscarCompleta(Long id) {
        OrdemServico os = osRepo.buscarCompletaPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "OS não encontrada: id=" + id));

        return OrdemServicoDTO.completo(
                os,
                itemPecaRepo.findByOrdemServicoId(id),
                itemServicoRepo.findByOrdemServicoId(id)
        );
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoDTO> buscar(String termo) {
        List<OrdemServico> ordens = (termo == null || termo.isBlank())
                ? osRepo.buscarTodas()
                : osRepo.buscarPorClienteOuPlaca(termo.trim());
        return paraResumo(ordens);
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoDTO> listarAbertas() {
        return paraResumo(osRepo.findByStatusOrderByDataEntradaDesc(StatusOs.ABERTA));
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoDTO> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new RegraDeNegocioException(
                    "Data inicial não pode ser maior que a data final.");
        }
        return paraResumo(osRepo.findByPeriodo(inicio, fim));
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoDTO> listarPorVeiculo(Long veiculoId) {
        return paraResumo(osRepo.findByVeiculoIdOrderByDataEntradaDesc(veiculoId));
    }

    private List<OrdemServicoDTO> paraResumo(List<OrdemServico> ordens) {
        return ordens.stream().map(OrdemServicoDTO::resumo).toList();
    }
}
