package com.mecanicadbo.service;

import com.mecanicadbo.dto.VeiculoRequest;
import com.mecanicadbo.exception.*;
import com.mecanicadbo.model.*;
import com.mecanicadbo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VeiculoService {

    private final VeiculoRepository veiculoRepo;
    private final ClienteRepository clienteRepo;

    // ------------------------------------------------------------------
    // Consultas (as que devolvem DTO ficam no VeiculoConsultaService)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Veiculo buscarPorId(Long id) {
        return veiculoRepo.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Veículo não encontrado: id=" + id));
    }

    @Transactional(readOnly = true)
    public Veiculo buscarPorPlaca(String placa) {
        return veiculoRepo.findByPlacaIgnoreCase(normalizarPlaca(placa))
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Veículo não encontrado: placa=" + placa));
    }

    // ------------------------------------------------------------------
    // Escrita
    // ------------------------------------------------------------------

    /**
     * Cria o veículo a partir do DTO.
     *
     * Mesma história dos outros criar(): com a entidade vindo direto do JSON,
     * um POST com "id" preenchido virava merge — UPDATE num veículo já
     * cadastrado, trocando o dono dele e apagando os campos não enviados.
     */
    @Transactional
    public Veiculo criar(Long clienteId, VeiculoRequest dados) {
        Cliente cliente = clienteRepo.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente não encontrado: id=" + clienteId));

        if (Boolean.FALSE.equals(cliente.getAtivo())) {
            throw new RegraDeNegocioException(
                    "Cliente inativo não pode receber veículo novo: " + cliente.getNome());
        }

        String placa = normalizarPlaca(dados.placa());
        validarPlacaDisponivel(placa, null);

        Veiculo v = new Veiculo();
        v.setCliente(cliente);
        v.setPlaca(placa);
        v.setAtivo(true);
        aplicarDados(v, dados);

        return veiculoRepo.save(v);
    }

    /**
     * A placa agora pode ser corrigida na edição.
     *
     * Antes, atualizar() não tinha setPlaca. Placa digitada errada no
     * cadastro só saía do banco na mão — e placa errada significa recibo e
     * histórico do veículo errados.
     */
    @Transactional
    public Veiculo atualizar(Long id, VeiculoRequest dados) {
        Veiculo v = buscarPorId(id);

        String placa = normalizarPlaca(dados.placa());
        validarPlacaDisponivel(placa, id);

        v.setPlaca(placa);
        aplicarDados(v, dados);

        return veiculoRepo.save(v);
    }

    @Transactional
    public void inativar(Long id) {
        Veiculo v = buscarPorId(id);
        if (Boolean.FALSE.equals(v.getAtivo())) return;
        v.setAtivo(false);
        veiculoRepo.save(v);
    }

    // ------------------------------------------------------------------

    private void aplicarDados(Veiculo v, VeiculoRequest dados) {
        v.setMarca(dados.marca().trim());
        v.setModelo(dados.modelo().trim());
        v.setCor(dados.cor().trim());
        // O CHECK do banco espera o rótulo em maiúsculas; o mecânico digita
        // "flex". A conversão aqui evita um 500 vindo da constraint.
        v.setCombustivel(dados.combustivel().trim().toUpperCase());
        v.setAnoFabricacao(dados.anoFabricacao());
        v.setChassi(vazioParaNull(dados.chassi()));
    }

    /** Placa sempre gravada em maiúsculas e sem espaços, para a busca bater. */
    private String normalizarPlaca(String placa) {
        return placa == null ? null : placa.trim().toUpperCase().replace(" ", "");
    }

    private String vazioParaNull(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    /** @param idAtual id do veículo em edição, ou null na criação. */
    private void validarPlacaDisponivel(String placa, Long idAtual) {
        veiculoRepo.findByPlacaIgnoreCase(placa).ifPresent(existente -> {
            if (existente.getId().equals(idAtual)) return;
            throw new RegraDeNegocioException("Placa já cadastrada: " + placa);
        });
    }
}
