package com.mecanicadbo.controller;

import com.mecanicadbo.dto.VeiculoRequest;
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

import java.util.List;

@RestController
@RequestMapping("/api/veiculos")
@RequiredArgsConstructor
@Tag(name = "Veículos", description = "Cadastro e busca de veículos")
public class VeiculoController {

    private final VeiculoService service;
    private final VeiculoConsultaService consulta;

    /**
     * ENDPOINT NOVO — não existia antes.
     *
     * O VeiculoService já tinha um listar(q) e o repositório já tinha a
     * consulta parcial por placa/modelo, mas nenhum controller expunha isso.
     * Era código morto: o front não tinha como listar veículos, e foi por
     * isso que a tela de busca ficou com dados mockados e o fluxo de nova OS
     * precisou tentar variações da placa até uma dar certo.
     *
     *   GET /api/veiculos          → todos os ativos
     *   GET /api/veiculos?q=jhx    → placa, modelo, marca ou nome do dono
     */
    @GetMapping
    @Operation(summary = "Listar ou buscar veículos",
               description = "Sem parâmetro retorna todos os ativos. Com ?q= busca por placa, modelo, marca ou nome do cliente.")
    public List<VeiculoResumoDTO> listar(@RequestParam(required = false) String q) {
        return consulta.listar(q);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar veículo por ID")
    public VeiculoResumoDTO buscarPorId(@PathVariable Long id) {
        return consulta.buscarPorId(id);
    }

    @GetMapping("/placa/{placa}")
    @Operation(summary = "Buscar veículo por placa exata")
    public VeiculoResumoDTO buscarPorPlaca(@PathVariable String placa) {
        return consulta.buscarPorPlaca(placa);
    }

    @PostMapping("/cliente/{clienteId}")
    @Operation(summary = "Cadastrar veículo para um cliente")
    public ResponseEntity<VeiculoResumoDTO> criar(
            @PathVariable Long clienteId,
            @RequestBody @Valid VeiculoRequest dados
    ) {
        Veiculo salvo = service.criar(clienteId, dados);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consulta.buscarPorId(salvo.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do veículo — a placa agora também pode ser corrigida")
    public VeiculoResumoDTO atualizar(
            @PathVariable Long id,
            @RequestBody @Valid VeiculoRequest dados
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
