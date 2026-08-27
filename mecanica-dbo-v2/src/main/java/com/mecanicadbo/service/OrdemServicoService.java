package com.mecanicadbo.service;

import com.mecanicadbo.exception.*;
import com.mecanicadbo.model.*;
import com.mecanicadbo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdemServicoService {

    private final OrdemServicoRepository osRepo;
    private final VeiculoRepository veiculoRepo;
    private final ItemPecaRepository itemPecaRepo;
    private final ItemServicoRepository itemServicoRepo;

    @Transactional(readOnly = true)
    public OrdemServico buscarPorId(Long id) {
        return osRepo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "OS não encontrada: id=" + id));
    }

    @Transactional(readOnly = true)
    public List<OrdemServico> buscar(String termo) {
        if (termo == null || termo.isBlank()) return osRepo.findAll();
        return osRepo.buscarPorClienteOuPlaca(termo.trim());
    }

    @Transactional(readOnly = true)
    public List<OrdemServico> listarAbertas() {
        return osRepo.findByStatusOrderByDataEntradaDesc(StatusOs.ABERTA);
    }

    @Transactional(readOnly = true)
    public List<OrdemServico> buscarPorPeriodo(LocalDate inicio, LocalDate fim) {
        return osRepo.findByPeriodo(inicio, fim);
    }

    @Transactional
    public OrdemServico criar(Long veiculoId, OrdemServico os) {
        Veiculo veiculo = veiculoRepo.findById(veiculoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Veículo não encontrado: id=" + veiculoId));
        os.setVeiculo(veiculo);
        os.setStatus(StatusOs.ABERTA);
        return osRepo.save(os);
    }

    @Transactional
    public OrdemServico atualizarStatus(Long id, StatusOs novoStatus) {
        OrdemServico os = buscarPorId(id);
        if (novoStatus == StatusOs.ENTREGUE && os.getStatus() != StatusOs.CONCLUIDA) {
            throw new RegraDeNegocioException(
                "A OS precisa estar CONCLUIDA antes de ser marcada como ENTREGUE.");
        }
        os.setStatus(novoStatus);
        if (novoStatus == StatusOs.ENTREGUE) {
            os.setDataSaidaReal(LocalDate.now());
        }
        return osRepo.save(os);
    }

    @Transactional
    public ItemPeca adicionarPeca(Long osId, ItemPeca item) {
        OrdemServico os = buscarPorId(osId);
        item.setOrdemServico(os);
        return itemPecaRepo.save(item);
    }

    @Transactional
    public ItemServico adicionarServico(Long osId, ItemServico item) {
        OrdemServico os = buscarPorId(osId);
        item.setOrdemServico(os);
        return itemServicoRepo.save(item);
    }

    @Transactional
    public void removerPeca(Long osId, Long itemId) {
        ItemPeca item = itemPecaRepo.findById(itemId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada: id=" + itemId));
        if (!item.getOrdemServico().getId().equals(osId))
            throw new RegraDeNegocioException("Item não pertence a esta OS");
        itemPecaRepo.delete(item);
    }

    @Transactional
    public void removerServico(Long osId, Long itemId) {
        ItemServico item = itemServicoRepo.findById(itemId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: id=" + itemId));
        if (!item.getOrdemServico().getId().equals(osId))
            throw new RegraDeNegocioException("Item não pertence a esta OS");
        itemServicoRepo.delete(item);
    }
}
