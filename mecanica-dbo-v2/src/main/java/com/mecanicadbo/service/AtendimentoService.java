package com.mecanicadbo.service;

import com.mecanicadbo.dto.AtendimentoRequest;
import com.mecanicadbo.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava cliente, veículo e OS numa transação só: se o veículo falhar (placa
 * repetida, por exemplo), o cliente também não fica gravado — e um novo
 * clique em salvar não cria um cliente repetido.
 *
 * Aqui os criar() dos services existentes são chamados pelos beans, então
 * entram nesta mesma transação e reaproveitam todas as regras (CPF e placa
 * repetidos, cliente/veículo inativo). Qualquer exceção desfaz tudo.
 */
@Service
@RequiredArgsConstructor
public class AtendimentoService {

    private final ClienteService clienteService;
    private final VeiculoService veiculoService;
    private final OrdemServicoService ordemServicoService;

    /** Ids do que foi gravado. osId é null quando não veio OS no pedido. */
    public record Resultado(Long veiculoId, Long osId) {}

    @Transactional
    public Resultado registrar(AtendimentoRequest pedido) {
        validarCombinacao(pedido);

        Long veiculoId = pedido.veiculoId();
        if (veiculoId == null) {
            Long clienteId = pedido.clienteId() != null
                    ? pedido.clienteId()
                    : clienteService.criar(pedido.novoCliente()).getId();
            veiculoId = veiculoService.criar(clienteId, pedido.novoVeiculo()).getId();
        }

        Long osId = null;
        if (pedido.os() != null) {
            osId = ordemServicoService.criar(veiculoId, pedido.os()).getId();
        }

        // Os ids são IDENTITY: cada save() já executou o INSERT, então um erro
        // de constraint do banco estoura aqui dentro (e vira mensagem traduzida
        // no GlobalExceptionHandler), não no commit.
        return new Resultado(veiculoId, osId);
    }

    private void validarCombinacao(AtendimentoRequest p) {
        if (p.veiculoId() != null) {
            if (p.novoVeiculo() != null || p.clienteId() != null || p.novoCliente() != null) {
                throw new RegraDeNegocioException(
                        "Escolha um veículo já cadastrado OU preencha os dados de um veículo novo, não os dois.");
            }
            if (p.os() == null) {
                throw new RegraDeNegocioException(
                        "Para um veículo já cadastrado, preencha os dados da ordem de serviço.");
            }
        } else {
            if (p.novoVeiculo() == null) {
                throw new RegraDeNegocioException(
                        "Informe o veículo: escolha um já cadastrado ou preencha os dados do veículo novo.");
            }
            boolean temExistente = p.clienteId() != null;
            boolean temNovo = p.novoCliente() != null;
            if (temExistente == temNovo) {
                throw new RegraDeNegocioException(
                        "Informe o dono do veículo: escolha um cliente já cadastrado OU preencha os dados de um cliente novo.");
            }
        }

        if (p.os() != null && (p.os().reclamacoes() == null || p.os().reclamacoes().isBlank())) {
            throw new RegraDeNegocioException("O relato do cliente é obrigatório para abrir a ordem de serviço.");
        }
    }
}
