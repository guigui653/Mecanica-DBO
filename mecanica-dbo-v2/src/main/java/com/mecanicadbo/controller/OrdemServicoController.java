package com.mecanicadbo.controller;

import com.mecanicadbo.model.*;
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

    @GetMapping
    @Operation(summary = "Buscar OS por cliente ou placa")
    public List<OrdemServico> buscar(@RequestParam(required = false) String q) {
        return service.buscar(q);
    }

    @GetMapping("/abertas")
    @Operation(summary = "Listar todas as OS abertas")
    public List<OrdemServico> abertas() {
        return service.listarAbertas();
    }

    @GetMapping("/periodo")
    @Operation(summary = "Listar OS por período")
    public List<OrdemServico> porPeriodo(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return service.buscarPorPeriodo(inicio, fim);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe completo de uma OS")
    public OrdemServico buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping("/veiculo/{veiculoId}")
    @Operation(summary = "Criar nova OS para um veículo")
    public ResponseEntity<OrdemServico> criar(
        @PathVariable Long veiculoId,
        @RequestBody OrdemServico os
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(veiculoId, os));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status da OS")
    public OrdemServico atualizarStatus(
        @PathVariable Long id,
        @RequestParam StatusOs status
    ) {
        return service.atualizarStatus(id, status);
    }

    @PostMapping("/{id}/pecas")
    @Operation(summary = "Adicionar peça à OS")
    public ResponseEntity<ItemPeca> adicionarPeca(
        @PathVariable Long id,
        @RequestBody ItemPeca item
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionarPeca(id, item));
    }

    @DeleteMapping("/{osId}/pecas/{itemId}")
    @Operation(summary = "Remover peça da OS")
    public ResponseEntity<Void> removerPeca(
        @PathVariable Long osId, @PathVariable Long itemId
    ) {
        service.removerPeca(osId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/servicos")
    @Operation(summary = "Adicionar serviço à OS")
    public ResponseEntity<ItemServico> adicionarServico(
        @PathVariable Long id,
        @RequestBody ItemServico item
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionarServico(id, item));
    }

    @DeleteMapping("/{osId}/servicos/{itemId}")
    @Operation(summary = "Remover serviço da OS")
    public ResponseEntity<Void> removerServico(
        @PathVariable Long osId, @PathVariable Long itemId
    ) {
        service.removerServico(osId, itemId);
        return ResponseEntity.noContent().build();
    }
}
