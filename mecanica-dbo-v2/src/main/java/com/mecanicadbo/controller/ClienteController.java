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
 * O retorno é a entidade Cliente, de propósito: o campo veiculos tem
 * @JsonIgnore (sem referência circular), e o ClienteResumoDTO não traz
 * telefone2 nem e-mail, que a tela de cadastro precisa.
 *
 * A entrada é ClienteRequest, sem campo id: com a entidade no corpo, um POST
 * com "id" faria merge (UPDATE num cliente existente) em vez de INSERT.
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
    @Operation(summary = "Apagar cliente — só se não tiver nenhuma OS (cadastro duplicado ou de teste)")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        clienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/inativar")
    @Operation(summary = "Inativar cliente — some da lista, mas o histórico de OS continua")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        clienteService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
