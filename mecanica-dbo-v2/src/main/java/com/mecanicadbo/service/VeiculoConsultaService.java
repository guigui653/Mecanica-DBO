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
 * O cliente já vem carregado pelo JOIN FETCH das consultas do repositório.
 */
@Service
@RequiredArgsConstructor
public class VeiculoConsultaService {

    private final VeiculoRepository repo;

    /** Listagem/busca da tela de veículos: placa, modelo, marca ou nome do dono. */
    @Transactional(readOnly = true)
    public List<VeiculoResumoDTO> listar(String termo) {
        List<Veiculo> veiculos = (termo == null || termo.isBlank())
                ? repo.listarAtivos()
                : repo.buscarPorTermo(termo.trim(), Placas.paraBusca(termo));

        return veiculos.stream().map(VeiculoResumoDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public VeiculoResumoDTO buscarPorId(Long id) {
        Veiculo v = repo.buscarComCliente(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Veículo não encontrado: id=" + id));
        return VeiculoResumoDTO.de(v);
    }

    @Transactional(readOnly = true)
    public VeiculoResumoDTO buscarPorPlaca(String placa) {
        Veiculo v = repo.findByPlacaIgnoreCase(Placas.normalizar(placa))
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Veículo não encontrado: placa=" + placa));
        return VeiculoResumoDTO.de(v);
    }

    @Transactional(readOnly = true)
    public List<VeiculoResumoDTO> listarPorCliente(Long clienteId) {
        return repo.findByClienteIdAndAtivoTrue(clienteId).stream()
                .map(VeiculoResumoDTO::de)
                .toList();
    }
}
