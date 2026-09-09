package com.mecanicadbo.service;

import com.mecanicadbo.dto.OrdemServicoDTO;
import com.mecanicadbo.exception.RecursoNaoEncontradoException;
import com.mecanicadbo.model.*;
import com.mecanicadbo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Consultas de OS que retornam DTOs completos.
 * As entidades têm @JsonIgnore nos relacionamentos para evitar referência
 * circular, então a montagem acontece aqui dentro da transação — se fosse
 * feita no controller, o lazy loading falharia com LazyInitializationException.
 */
@Service
@RequiredArgsConstructor
public class OrdemServicoConsultaService {

    private final OrdemServicoRepository osRepo;
    private final ItemPecaRepository itemPecaRepo;
    private final ItemServicoRepository itemServicoRepo;

    @Transactional(readOnly = true)
    public OrdemServicoDTO buscarCompleta(Long id) {
        OrdemServico os = osRepo.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "OS não encontrada: id=" + id));

        // força a carga do veículo e do cliente dentro da transação
        os.getVeiculo().getCliente().getNome();

        return OrdemServicoDTO.completo(
                os,
                itemPecaRepo.findByOrdemServicoId(id),
                itemServicoRepo.findByOrdemServicoId(id)
        );
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoDTO> buscar(String termo) {
        List<OrdemServico> ordens = (termo == null || termo.isBlank())
                ? osRepo.findAll()
                : osRepo.buscarPorClienteOuPlaca(termo.trim());
        return paraResumo(ordens);
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoDTO> listarAbertas() {
        return paraResumo(osRepo.findByStatusOrderByDataEntradaDesc(StatusOs.ABERTA));
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoDTO> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        return paraResumo(osRepo.findByPeriodo(inicio, fim));
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoDTO> listarPorVeiculo(Long veiculoId) {
        return paraResumo(osRepo.findByVeiculoIdOrderByDataEntradaDesc(veiculoId));
    }

    private List<OrdemServicoDTO> paraResumo(List<OrdemServico> ordens) {
        return ordens.stream()
                .peek(os -> os.getVeiculo().getCliente().getNome())
                .map(OrdemServicoDTO::resumo)
                .toList();
    }
}
