package com.mecanicadbo.service;

import com.mecanicadbo.dto.ClienteRequest;
import com.mecanicadbo.exception.*;
import com.mecanicadbo.model.Cliente;
import com.mecanicadbo.repository.ClienteRepository;
import com.mecanicadbo.repository.OrdemServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repo;
    private final OrdemServicoRepository osRepo;

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /** Listagem da tela de clientes. Com ou sem termo, só ativos. */
    @Transactional(readOnly = true)
    public List<Cliente> buscar(String termo) {
        if (termo == null || termo.isBlank()) return repo.findByAtivoTrueOrderByNome();
        return repo.buscarPorTermo(termo.trim());
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente não encontrado: id=" + id));
    }

    // ------------------------------------------------------------------
    // Escrita
    // ------------------------------------------------------------------

    /**
     * Cria o cliente a partir do DTO, nunca da entidade vinda do JSON.
     *
     * Motivo: repo.save(entidade) com id preenchido não insere — faz merge,
     * isto é, UPDATE. Um POST /api/clientes com {"id": 3, "nome": "Teste"}
     * sobrescrevia o cliente 3 e ainda apagava telefone, e-mail e CPF dele,
     * porque os campos ausentes no corpo chegavam null.
     */
    @Transactional
    public Cliente criar(ClienteRequest dados) {
        String cpf = normalizarCpf(dados.cpf());
        validarCpfDisponivel(cpf, null);

        Cliente c = new Cliente();
        c.setNome(dados.nome().trim());
        c.setCpf(cpf);
        c.setTelefone1(dados.telefone1().trim());
        c.setTelefone2(vazioParaNull(dados.telefone2()));
        c.setEmail(vazioParaNull(dados.email()));
        c.setAtivo(true);

        return repo.save(c);
    }

    /** Edição completa, inclusive do CPF (para corrigir digitação errada pela tela). */
    @Transactional
    public Cliente atualizar(Long id, ClienteRequest dados) {
        Cliente c = buscarPorId(id);

        String cpf = normalizarCpf(dados.cpf());
        validarCpfDisponivel(cpf, id);

        c.setNome(dados.nome().trim());
        c.setCpf(cpf);
        c.setTelefone1(dados.telefone1().trim());
        c.setTelefone2(vazioParaNull(dados.telefone2()));
        c.setEmail(vazioParaNull(dados.email()));

        return repo.save(c);
    }

    @Transactional
    public void inativar(Long id) {
        Cliente c = buscarPorId(id);
        if (Boolean.FALSE.equals(c.getAtivo())) return;   // já estava inativo
        c.setAtivo(false);
        repo.save(c);
    }

    /**
     * Apaga de verdade um cliente SEM nenhuma OS (cadastro duplicado ou de
     * teste), junto com os veículos dele.
     *
     * Cliente com OS no histórico não é apagado: as OS entregues são o
     * histórico do carro e entram no faturamento dos meses passados. Para
     * tirá-lo da lista sem perder nada, use inativar().
     */
    @Transactional
    public void excluir(Long id) {
        Cliente c = buscarPorId(id);
        long ordens = osRepo.countByVeiculoClienteId(id);
        if (ordens > 0) {
            throw new RegraDeNegocioException(
                    c.getNome() + " tem " + ordens + (ordens == 1 ? " ordem de serviço" : " ordens de serviço")
                  + " no histórico e não pode ser apagado. Use \"Inativar\" para tirá-lo da lista sem perder o histórico.");
        }
        repo.delete(c);
    }

    // ------------------------------------------------------------------

    /**
     * CPF em branco vira null antes de chegar no banco.
     *
     * A coluna cpf tem índice único, e para o PostgreSQL "" não é null: o
     * segundo cliente sem CPF gravaria "" de novo e bateria no índice. Null,
     * ao contrário, pode se repetir à vontade num índice único.
     */
    private String normalizarCpf(String cpf) {
        if (cpf == null) return null;
        String limpo = cpf.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private String vazioParaNull(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    /**
     * @param idAtual id do cliente sendo editado, ou null na criação.
     *                Serve para o cliente não colidir com ele mesmo ao salvar
     *                a edição sem mexer no CPF.
     */
    private void validarCpfDisponivel(String cpf, Long idAtual) {
        if (cpf == null) return;

        // Sem filtrar por ativo: o índice único vale para a tabela toda.
        repo.findByCpf(cpf).ifPresent(existente -> {
            if (existente.getId().equals(idAtual)) return;

            String situacao = Boolean.FALSE.equals(existente.getAtivo())
                    ? " (cliente inativo — reative o cadastro em vez de criar outro)"
                    : "";
            throw new RegraDeNegocioException(
                    "CPF já cadastrado para: " + existente.getNome() + situacao);
        });
    }
}
