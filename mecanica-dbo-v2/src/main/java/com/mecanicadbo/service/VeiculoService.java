package com.mecanicadbo.service;

import com.mecanicadbo.exception.*;
import com.mecanicadbo.model.*;
import com.mecanicadbo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VeiculoService {

    private final VeiculoRepository veiculoRepo;
    private final ClienteRepository clienteRepo;

    @Transactional(readOnly = true)
    public Veiculo buscarPorId(Long id) {
        return veiculoRepo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Veículo não encontrado: id=" + id));
    }

    @Transactional(readOnly = true)
    public Veiculo buscarPorPlaca(String placa) {
        return veiculoRepo.findByPlacaIgnoreCase(placa)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Veículo não encontrado: placa=" + placa));
    }

    @Transactional(readOnly = true)
    public List<Veiculo> listarPorCliente(Long clienteId) {
        return veiculoRepo.findByClienteIdAndAtivoTrue(clienteId);
    }

    @Transactional
    public Veiculo criar(Long clienteId, Veiculo veiculo) {
        Cliente cliente = clienteRepo.findById(clienteId)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Cliente não encontrado: id=" + clienteId));

        veiculoRepo.findByPlacaIgnoreCase(veiculo.getPlaca()).ifPresent(v -> {
            throw new RegraDeNegocioException(
                "Placa já cadastrada: " + veiculo.getPlaca());
        });

        veiculo.setCliente(cliente);
        return veiculoRepo.save(veiculo);
    }

    @Transactional
    public Veiculo atualizar(Long id, Veiculo dados) {
        Veiculo v = buscarPorId(id);
        v.setMarca(dados.getMarca());
        v.setModelo(dados.getModelo());
        v.setCor(dados.getCor());
        v.setCombustivel(dados.getCombustivel());
        v.setAnoFabricacao(dados.getAnoFabricacao());
        v.setChassi(dados.getChassi());
        return veiculoRepo.save(v);
    }

    @Transactional
    public void inativar(Long id) {
        Veiculo v = buscarPorId(id);
        v.setAtivo(false);
        veiculoRepo.save(v);
    }
}
