package com.mecanicadbo.controller;

import com.mecanicadbo.dto.VeiculoResumoDTO;
import com.mecanicadbo.model.Veiculo;
import com.mecanicadbo.service.VeiculoConsultaService;
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
    private final VeiculoConsultaService consulta;

    @GetMapping("/{id}")
    @Operation(summary = "Buscar veículo por ID")
    public VeiculoResumoDTO buscarPorId(@PathVariable Long id) {
        return consulta.buscarPorId(id);
    }

    @GetMapping("/placa/{placa}")
    @Operation(summary = "Buscar veículo por placa")
    public VeiculoResumoDTO buscarPorPlaca(@PathVariable String placa) {
        return consulta.buscarPorPlaca(placa);
    }

    @PostMapping("/cliente/{clienteId}")
    @Operation(summary = "Cadastrar veículo para um cliente")
    public ResponseEntity<VeiculoResumoDTO> criar(
            @PathVariable Long clienteId,
            @RequestBody @Valid Veiculo veiculo
    ) {
        Veiculo salvo = service.criar(clienteId, veiculo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consulta.buscarPorId(salvo.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do veículo")
    public VeiculoResumoDTO atualizar(
            @PathVariable Long id,
            @RequestBody @Valid Veiculo dados
    ) {
        service.atualizar(id, dados);
        return consulta.buscarPorId(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativar veículo")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
