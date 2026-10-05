package com.mecanicadbo.service;

import com.mecanicadbo.dto.ClienteRequest;
import com.mecanicadbo.exception.*;
import com.mecanicadbo.model.Cliente;
import com.mecanicadbo.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repo;

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /**
     * Listagem da tela de clientes.
     *
     * Antes: sem termo usava findAll() (trazia inativos), com termo usava
     * buscarPorTermo() (só ativos). O cliente desativado aparecia na lista
     * "todos" e sumia ao digitar o nome — duas telas discordando entre si.
     * Agora as duas pontas mostram só ativos.
     */
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

    /**
     * O CPF agora pode ser corrigido na edição.
     *
     * Antes, atualizar() simplesmente não tinha setCpf. Quem digitasse o CPF
     * errado no cadastro não conseguia arrumar pela tela — tinha que mexer no
     * banco na mão.
     */
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

    // ------------------------------------------------------------------

    /**
     * ESTE É O BUG DO "CADASTREI O CLIENTE E ELE NÃO APARECE NA LISTA".
     *
     * A coluna cpf tem índice único. String vazia NÃO é null para o
     * PostgreSQL: o primeiro cliente sem CPF gravava "", o segundo tentava
     * gravar "" de novo e batia no índice único. Como não havia handler para
     * DataIntegrityViolationException, o erro virava um 500 genérico que o
     * front engolia — o cadastro parecia ter dado certo e o cliente nunca
     * aparecia.
     *
     * Null, ao contrário de "", pode se repetir à vontade num índice único.
     * Por isso todo CPF em branco vira null antes de chegar no banco.
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

        // findByCpf, e não findByCpfAndAtivoTrue: o índice único vale para a
        // tabela toda, inclusive para clientes inativos. Checar só os ativos
        // deixava passar um CPF que o banco ia recusar depois.
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
