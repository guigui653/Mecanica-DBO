package com.mecanicadbo.service;

import com.mecanicadbo.dto.AtualizarOsRequest;
import com.mecanicadbo.dto.CriarOsRequest;
import com.mecanicadbo.dto.ItemPecaRequest;
import com.mecanicadbo.dto.ItemServicoRequest;
import com.mecanicadbo.exception.*;
import com.mecanicadbo.model.*;
import com.mecanicadbo.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdemServicoService {

    @PersistenceContext
    private EntityManager entityManager;

    private final OrdemServicoRepository osRepo;
    private final VeiculoRepository veiculoRepo;
    private final ItemPecaRepository itemPecaRepo;
    private final ItemServicoRepository itemServicoRepo;

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public OrdemServico buscarPorId(Long id) {
        return osRepo.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "OS não encontrada: id=" + id));
    }

    @Transactional(readOnly = true)
    public List<OrdemServico> buscar(String termo) {
        if (termo == null || termo.isBlank()) return osRepo.buscarTodas();
        return osRepo.buscarPorClienteOuPlaca(termo.trim());
    }

    @Transactional(readOnly = true)
    public List<OrdemServico> listarAbertas() {
        return osRepo.findByStatusOrderByDataEntradaDesc(StatusOs.ABERTA);
    }

    @Transactional(readOnly = true)
    public List<OrdemServico> buscarPorPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new RegraDeNegocioException(
                    "Data inicial não pode ser maior que a data final.");
        }
        return osRepo.findByPeriodo(inicio, fim);
    }

    // ------------------------------------------------------------------
    // Escrita
    // ------------------------------------------------------------------

    /**
     * Cria a OS a partir de um DTO, nunca de uma entidade vinda do JSON.
     *
     * A versão anterior recebia OrdemServico direto do @RequestBody e
     * chamava osRepo.save(os). Se o JSON trouxesse "id", o save() virava
     * merge() — ou seja, UPDATE numa OS existente, apagando todo campo
     * não enviado no corpo. Um POST conseguia destruir uma OS antiga.
     *
     * Aqui a entidade nasce dentro do método: id é sempre null, status é
     * sempre ABERTA e os totais ficam com o banco.
     */
    @Transactional
    public OrdemServico criar(Long veiculoId, CriarOsRequest dados) {
        Veiculo veiculo = veiculoRepo.findById(veiculoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Veículo não encontrado: id=" + veiculoId));

        if (Boolean.FALSE.equals(veiculo.getAtivo())) {
            throw new RegraDeNegocioException(
                    "Veículo inativo não pode receber nova OS: " + veiculo.getPlaca());
        }

        OrdemServico os = new OrdemServico();
        os.setVeiculo(veiculo);
        os.setStatus(StatusOs.ABERTA);
        os.setKmEntrada(dados.kmEntrada());
        os.setDataEntrada(dados.dataEntrada() != null ? dados.dataEntrada() : LocalDate.now());
        os.setDataSaidaPrevista(dados.dataSaidaPrevista());
        os.setReclamacoes(dados.reclamacoes());
        os.setDiagnostico(dados.diagnostico());
        os.setTotalPecas(BigDecimal.ZERO);
        os.setTotalServicos(BigDecimal.ZERO);
        os.setTotalGeral(BigDecimal.ZERO);

        return osRepo.save(os);
    }

    /**
     * Atualização parcial: campo null no DTO significa "não enviado",
     * então o valor gravado é preservado. String vazia limpa o campo.
     */
    @Transactional
    public OrdemServico atualizar(Long id, AtualizarOsRequest dados) {
        OrdemServico os = buscarPorId(id);
        garantirEditavel(os);

        if (dados.kmEntrada() != null)          os.setKmEntrada(dados.kmEntrada());
        if (dados.dataSaidaPrevista() != null)  os.setDataSaidaPrevista(dados.dataSaidaPrevista());
        if (dados.reclamacoes() != null)        os.setReclamacoes(dados.reclamacoes());
        if (dados.diagnostico() != null)        os.setDiagnostico(dados.diagnostico());

        return osRepo.save(os);
    }

    @Transactional
    public OrdemServico atualizarStatus(Long id, StatusOs novoStatus) {
        OrdemServico os = buscarPorId(id);
        StatusOs atual = os.getStatus();

        if (novoStatus == atual) return os;

        if (novoStatus == StatusOs.ENTREGUE && atual != StatusOs.CONCLUIDA) {
            throw new RegraDeNegocioException(
                    "A OS precisa estar CONCLUIDA antes de ser marcada como ENTREGUE.");
        }

        // Única saída de uma OS entregue: reabrir para CONCLUIDA.
        // Sem isso, um clique errado em "entregue" trancaria a OS para sempre.
        if (atual == StatusOs.ENTREGUE && novoStatus != StatusOs.CONCLUIDA) {
            throw new RegraDeNegocioException(
                    "OS já entregue. Para corrigi-la, volte o status para CONCLUIDA primeiro.");
        }

        os.setStatus(novoStatus);

        if (novoStatus == StatusOs.ENTREGUE) {
            os.setDataSaidaReal(LocalDate.now());
        } else if (atual == StatusOs.ENTREGUE) {
            os.setDataSaidaReal(null);   // reabriu: a data de saída deixa de valer
        }

        return osRepo.save(os);
    }

    @Transactional
    public ItemPeca adicionarPeca(Long osId, ItemPecaRequest dados) {
        OrdemServico os = buscarPorId(osId);
        garantirEditavel(os);

        ItemPeca item = new ItemPeca();
        item.setOrdemServico(os);
        item.setDescricao(dados.descricao().trim());
        item.setQuantidade(dados.quantidade() != null ? dados.quantidade() : BigDecimal.ONE);
        item.setValorUnitario(dados.valorUnitario());
        item.setPagoPeloCliente(Boolean.TRUE.equals(dados.pagoPeloCliente()));

        ItemPeca salvo = itemPecaRepo.save(item);
        entityManager.flush();
        // valor_total é gerado pelo banco; sem o refresh, a resposta traz a peça nova com total vazio.
        entityManager.refresh(salvo);
        entityManager.refresh(os);
        return salvo;
    }

    @Transactional
    public ItemServico adicionarServico(Long osId, ItemServicoRequest dados) {
        OrdemServico os = buscarPorId(osId);
        garantirEditavel(os);

        ItemServico item = new ItemServico();
        item.setOrdemServico(os);
        item.setDescricao(dados.descricao().trim());
        item.setValor(dados.valor());

        ItemServico salvo = itemServicoRepo.save(item);
        entityManager.flush();
        entityManager.refresh(os);
        return salvo;
    }

    @Transactional
    public void removerPeca(Long osId, Long itemId) {
        ItemPeca item = itemPecaRepo.findById(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Peça não encontrada: id=" + itemId));

        if (!item.getOrdemServico().getId().equals(osId)) {
            throw new RegraDeNegocioException("Item não pertence a esta OS");
        }
        OrdemServico os = item.getOrdemServico();
        garantirEditavel(os);

        itemPecaRepo.delete(item);
        entityManager.flush();
        entityManager.refresh(os);
    }

    @Transactional
    public void removerServico(Long osId, Long itemId) {
        ItemServico item = itemServicoRepo.findById(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Serviço não encontrado: id=" + itemId));

        if (!item.getOrdemServico().getId().equals(osId)) {
            throw new RegraDeNegocioException("Item não pertence a esta OS");
        }
        OrdemServico os = item.getOrdemServico();
        garantirEditavel(os);

        itemServicoRepo.delete(item);
        entityManager.flush();
        entityManager.refresh(os);
    }

    /**
     * Exclui uma OS. Só é permitido se ela já estiver com status ENTREGUE.
     * Os filhos (itens de peça, itens de serviço e notas fiscais) são apagados
     * em cascata pelo JPA (CascadeType.ALL nos relacionamentos da entidade).
     */
    @Transactional
    public void excluir(Long id) {
        OrdemServico os = buscarPorId(id);
        if (os.getStatus() != StatusOs.ENTREGUE) {
            throw new RegraDeNegocioException(
                    "Só é possível excluir uma OS com status ENTREGUE. "
                  + "Esta OS está com status: " + os.getStatus() + ".");
        }
        osRepo.delete(os);
    }

    // ------------------------------------------------------------------

    /**
     * Guarda única de "OS entregue é documento fechado".
     *
     * Antes essa regra existia só no atualizar(). Pela API dava para
     * adicionar e remover peças e serviços de uma OS já entregue — e os
     * totais eram recalculados pela trigger, então o recibo que o cliente
     * levou para casa deixava de bater com o banco.
     */
    private void garantirEditavel(OrdemServico os) {
        if (os.getStatus() == StatusOs.ENTREGUE) {
            throw new RegraDeNegocioException(
                    "OS já entregue não pode ser alterada. "
                  + "Volte o status para CONCLUIDA se precisar corrigir algo.");
        }
    }
}
