package com.mecanicadbo.service;

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

    @Transactional(readOnly = true)
    public List<Cliente> buscar(String termo) {
        if (termo == null || termo.isBlank()) return repo.findAll();
        return repo.buscarPorTermo(termo.trim());
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Cliente não encontrado: id=" + id));
    }

    @Transactional
    public Cliente criar(Cliente cliente) {
        if (cliente.getCpf() != null && !cliente.getCpf().isBlank()) {
            repo.findByCpfAndAtivoTrue(cliente.getCpf()).ifPresent(c -> {
                throw new RegraDeNegocioException(
                    "CPF já cadastrado para: " + c.getNome());
            });
        }
        return repo.save(cliente);
    }

    @Transactional
    public Cliente atualizar(Long id, Cliente dados) {
        Cliente c = buscarPorId(id);
        c.setNome(dados.getNome());
        c.setTelefone1(dados.getTelefone1());
        c.setTelefone2(dados.getTelefone2());
        c.setEmail(dados.getEmail());
        return repo.save(c);
    }

    @Transactional
    public void inativar(Long id) {
        Cliente c = buscarPorId(id);
        c.setAtivo(false);
        repo.save(c);
    }
}
