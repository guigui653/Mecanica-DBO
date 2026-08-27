package com.mecanicadbo.controller;

import com.mecanicadbo.model.Veiculo;
import com.mecanicadbo.service.VeiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/veiculos")
@RequiredArgsConstructor
@Tag(name = "Veículos", description = "Cadastro e busca de veículos")
public class VeiculoController {

    private final VeiculoService service;

    @GetMapping("/{id}")
    @Operation(summary = "Buscar veículo por ID")
    public Veiculo buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/placa/{placa}")
    @Operation(summary = "Buscar veículo por placa")
    public Veiculo buscarPorPlaca(@PathVariable String placa) {
        return service.buscarPorPlaca(placa);
    }

    @PostMapping("/cliente/{clienteId}")
    @Operation(summary = "Cadastrar veículo para um cliente")
    public ResponseEntity<Veiculo> criar(
        @PathVariable Long clienteId,
        @RequestBody @Valid Veiculo veiculo
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.criar(clienteId, veiculo));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do veículo")
    public Veiculo atualizar(@PathVariable Long id, @RequestBody @Valid Veiculo dados) {
        return service.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativar veículo")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
