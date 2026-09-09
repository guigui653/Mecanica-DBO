package com.mecanicadbo.service;

import com.mecanicadbo.dto.VeiculoResumoDTO;
import com.mecanicadbo.exception.RecursoNaoEncontradoException;
import com.mecanicadbo.model.Veiculo;
import com.mecanicadbo.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consultas de veículo que retornam DTO com o cliente aninhado.
 * A montagem fica na transação porque Veiculo.cliente é LAZY.
 */
@Service
@RequiredArgsConstructor
public class VeiculoConsultaService {

    private final VeiculoRepository repo;

    @Transactional(readOnly = true)
    public VeiculoResumoDTO buscarPorId(Long id) {
        Veiculo v = repo.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Veículo não encontrado: id=" + id));
        v.getCliente().getNome();
        return VeiculoResumoDTO.de(v);
    }

    @Transactional(readOnly = true)
    public VeiculoResumoDTO buscarPorPlaca(String placa) {
        Veiculo v = repo.findByPlacaIgnoreCase(placa)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Veículo não encontrado: placa=" + placa));
        v.getCliente().getNome();
        return VeiculoResumoDTO.de(v);
    }

    @Transactional(readOnly = true)
    public List<VeiculoResumoDTO> listarPorCliente(Long clienteId) {
        return repo.findByClienteIdAndAtivoTrue(clienteId).stream()
                .peek(v -> v.getCliente().getNome())
                .map(VeiculoResumoDTO::de)
                .toList();
    }
}
