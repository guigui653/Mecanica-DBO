package com.mecanicadbo.controller;

import com.mecanicadbo.dto.ClienteRequest;
import com.mecanicadbo.dto.VeiculoResumoDTO;
import com.mecanicadbo.model.Cliente;
import com.mecanicadbo.service.ClienteService;
import com.mecanicadbo.service.VeiculoConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * O retorno continua sendo a entidade Cliente, de propósito.
 *
 * Serializar Cliente é seguro aqui porque o campo veiculos tem @JsonIgnore —
 * não há risco de referência circular. E o ClienteResumoDTO só carrega id,
 * nome, cpf e telefone1: trocar o retorno por ele faria o front perder
 * telefone2 e e-mail na tela de cadastro.
 *
 * O que mudou foi só a ENTRADA: @RequestBody agora recebe ClienteRequest, que
 * não tem campo id. Antes, um POST /api/clientes com {"id": 3, ...} não criava
 * cliente nenhum — fazia merge, isto é, UPDATE no cliente 3, e os campos
 * ausentes no JSON viravam null.
 */
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Cadastro e busca de clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final VeiculoConsultaService veiculoConsulta;

    @GetMapping
    @Operation(summary = "Listar ou buscar clientes",
               description = "Sem parâmetro retorna todos os ativos. Com ?q= busca por nome, CPF ou telefone.")
    public List<Cliente> listar(@RequestParam(required = false) String q) {
        return clienteService.buscar(q);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID")
    public Cliente buscarPorId(@PathVariable Long id) {
        return clienteService.buscarPorId(id);
    }

    @GetMapping("/{id}/veiculos")
    @Operation(summary = "Listar veículos de um cliente")
    public List<VeiculoResumoDTO> veiculosDoCliente(@PathVariable Long id) {
        return veiculoConsulta.listarPorCliente(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo cliente")
    public ResponseEntity<Cliente> criar(@RequestBody @Valid ClienteRequest dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.criar(dados));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do cliente — o CPF agora também pode ser corrigido")
    public Cliente atualizar(@PathVariable Long id, @RequestBody @Valid ClienteRequest dados) {
        return clienteService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativar cliente")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        clienteService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
