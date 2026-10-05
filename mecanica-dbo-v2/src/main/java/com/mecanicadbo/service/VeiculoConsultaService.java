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
 *
 * As chamadas .getCliente().getNome() que existiam aqui só para "acordar" o
 * proxy lazy sumiram: as consultas do repositório agora fazem JOIN FETCH do
 * cliente, então ele já vem carregado na mesma query.
 */
@Service
@RequiredArgsConstructor
public class VeiculoConsultaService {

    private final VeiculoRepository repo;

    /**
     * Listagem/busca da tela de consulta de veículos.
     *
     * Este método não existia — o VeiculoService tinha um listar(q) que
     * nenhum controller chamava. Era código morto, e por isso a tela de busca
     * do front ficou com dados mockados.
     */
    @Transactional(readOnly = true)
    public List<VeiculoResumoDTO> listar(String termo) {
        List<Veiculo> veiculos = (termo == null || termo.isBlank())
                ? repo.listarAtivos()
                : repo.buscarPorTermo(termo.trim());

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
        String normalizada = placa == null ? null : placa.trim().toUpperCase().replace(" ", "");

        Veiculo v = repo.findByPlacaIgnoreCase(normalizada)
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
