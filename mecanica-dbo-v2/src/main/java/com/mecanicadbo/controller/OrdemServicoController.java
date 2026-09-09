package com.mecanicadbo.controller;

import com.mecanicadbo.dto.*;
import com.mecanicadbo.model.*;
import com.mecanicadbo.service.OrdemServicoConsultaService;
import com.mecanicadbo.service.OrdemServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ordens")
@RequiredArgsConstructor
@Tag(name = "Ordens de Serviço", description = "Criação e gerenciamento de OS")
public class OrdemServicoController {

    private final OrdemServicoService service;
    private final OrdemServicoConsultaService consulta;

    @GetMapping
    @Operation(summary = "Buscar OS por cliente ou placa")
    public List<OrdemServicoDTO> buscar(@RequestParam(required = false) String q) {
        return consulta.buscar(q);
    }

    @GetMapping("/abertas")
    @Operation(summary = "Listar todas as OS abertas")
    public List<OrdemServicoDTO> abertas() {
        return consulta.listarAbertas();
    }

    @GetMapping("/periodo")
    @Operation(summary = "Listar OS por período")
    public List<OrdemServicoDTO> porPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
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
            @RequestBody OrdemServico os
    ) {
        OrdemServico salva = service.criar(veiculoId, os);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consulta.buscarCompleta(salva.getId()));
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
            @RequestBody ItemPeca item
    ) {
        service.adicionarPeca(id, item);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consulta.buscarCompleta(id));
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
            @RequestBody ItemServico item
    ) {
        service.adicionarServico(id, item);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consulta.buscarCompleta(id));
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
}
