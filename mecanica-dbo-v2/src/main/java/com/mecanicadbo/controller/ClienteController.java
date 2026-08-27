package com.mecanicadbo.controller;

import com.mecanicadbo.model.Cliente;
import com.mecanicadbo.model.Veiculo;
import com.mecanicadbo.service.ClienteService;
import com.mecanicadbo.service.VeiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Cadastro e busca de clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final VeiculoService veiculoService;

    @GetMapping
    @Operation(summary = "Listar ou buscar clientes",
               description = "Sem parâmetro retorna todos. Com ?q= busca por nome, CPF ou telefone.")
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
    public List<Veiculo> veiculosDoCliente(@PathVariable Long id) {
        return veiculoService.listarPorCliente(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo cliente")
    public ResponseEntity<Cliente> criar(@RequestBody @Valid Cliente cliente) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.criar(cliente));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do cliente")
    public Cliente atualizar(@PathVariable Long id, @RequestBody @Valid Cliente dados) {
        return clienteService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativar cliente")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        clienteService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
