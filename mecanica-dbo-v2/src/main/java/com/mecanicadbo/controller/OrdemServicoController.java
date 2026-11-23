package com.mecanicadbo.controller;

import com.mecanicadbo.dto.*;
import com.mecanicadbo.model.*;
import com.mecanicadbo.service.OrdemServicoConsultaService;
import com.mecanicadbo.service.OrdemServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Todos os @RequestBody são DTOs de request com @Valid, nunca entidades.
 *
 * Com a entidade no corpo, um POST como
 *     { "id": 1, "status": "ENTREGUE", "totalGeral": 99999 }
 * faria merge (UPDATE na OS 1) e zeraria os campos não enviados. Os DTOs não
 * têm id, status nem totais, então não há como mandar isso.
 */
@RestController
@RequestMapping("/api/ordens")
@RequiredArgsConstructor
@Tag(name = "Ordens de Serviço", description = "Criação e gerenciamento de OS")
public class OrdemServicoController {

    private final OrdemServicoService service;
    private final OrdemServicoConsultaService consulta;

    @GetMapping
    @Operation(summary = "Buscar OS por cliente, placa ou número")
    public List<OrdemServicoDTO> buscar(@RequestParam(required = false) String q) {
        return consulta.buscar(q);
    }

    @GetMapping("/abertas")
    @Operation(summary = "Listar todas as OS abertas")
    public List<OrdemServicoDTO> abertas() {
        return consulta.listarAbertas();
    }

    /**
     * inicio e fim agora são opcionais (required = false).
     *
     * Antes os dois eram obrigatórios: chamar /periodo?inicio=2026-01-01 sem o
     * fim devolvia 500 do Spring, e não um erro explicando o que faltava.
     * Agora dá para pedir "tudo a partir de janeiro" mandando só o início.
     */
    @GetMapping("/periodo")
    @Operation(summary = "Listar OS por período (início e fim são opcionais)")
    public List<OrdemServicoDTO> porPeriodo(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return consulta.listarPorPeriodo(inicio, fim);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe completo de uma OS — inclui veículo, cliente e itens")
    public OrdemServicoDTO buscarPorId(@PathVariable Long id) {
        return consulta.buscarCompleta(id);
    }

    @GetMapping("/veiculo/{veiculoId}")
    @Operation(summary = "Listar todas as OS anteriores de um veículo")
    public List<OrdemServicoDTO> porVeiculo(@PathVariable Long veiculoId) {
        return consulta.listarPorVeiculo(veiculoId);
    }

    @PostMapping("/veiculo/{veiculoId}")
    @Operation(summary = "Criar nova OS para um veículo")
    public ResponseEntity<OrdemServicoDTO> criar(
            @PathVariable Long veiculoId,
            @RequestBody @Valid CriarOsRequest dados
    ) {
        OrdemServico salva = service.criar(veiculoId, dados);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consulta.buscarCompleta(salva.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados da OS — campos não enviados são preservados")
    public OrdemServicoDTO atualizar(
            @PathVariable Long id,
            @RequestBody @Valid AtualizarOsRequest dados
    ) {
        service.atualizar(id, dados);
        return consulta.buscarCompleta(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status da OS")
    public OrdemServicoDTO atualizarStatus(
            @PathVariable Long id,
            @RequestParam StatusOs status
    ) {
        service.atualizarStatus(id, status);
        return consulta.buscarCompleta(id);
    }

    @PostMapping("/{id}/pecas")
    @Operation(summary = "Adicionar peça à OS")
    public ResponseEntity<OrdemServicoDTO> adicionarPeca(
            @PathVariable Long id,
            @RequestBody @Valid ItemPecaRequest item
    ) {
        service.adicionarPeca(id, item);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consulta.buscarCompleta(id));
    }

    @PutMapping("/{osId}/pecas/{itemId}")
    @Operation(summary = "Corrigir peça da OS")
    public OrdemServicoDTO atualizarPeca(
            @PathVariable Long osId,
            @PathVariable Long itemId,
            @RequestBody @Valid ItemPecaRequest item
    ) {
        service.atualizarPeca(osId, itemId, item);
        return consulta.buscarCompleta(osId);
    }

    @DeleteMapping("/{osId}/pecas/{itemId}")
    @Operation(summary = "Remover peça da OS")
    public OrdemServicoDTO removerPeca(
            @PathVariable Long osId,
            @PathVariable Long itemId
    ) {
        service.removerPeca(osId, itemId);
        return consulta.buscarCompleta(osId);
    }

    @PostMapping("/{id}/servicos")
    @Operation(summary = "Adicionar serviço à OS")
    public ResponseEntity<OrdemServicoDTO> adicionarServico(
            @PathVariable Long id,
            @RequestBody @Valid ItemServicoRequest item
    ) {
        service.adicionarServico(id, item);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consulta.buscarCompleta(id));
    }

    @PutMapping("/{osId}/servicos/{itemId}")
    @Operation(summary = "Corrigir serviço da OS")
    public OrdemServicoDTO atualizarServico(
            @PathVariable Long osId,
            @PathVariable Long itemId,
            @RequestBody @Valid ItemServicoRequest item
    ) {
        service.atualizarServico(osId, itemId, item);
        return consulta.buscarCompleta(osId);
    }

    @DeleteMapping("/{osId}/servicos/{itemId}")
    @Operation(summary = "Remover serviço da OS")
    public OrdemServicoDTO removerServico(
            @PathVariable Long osId,
            @PathVariable Long itemId
    ) {
        service.removerServico(osId, itemId);
        return consulta.buscarCompleta(osId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir OS — só permitido quando o status for ENTREGUE")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
