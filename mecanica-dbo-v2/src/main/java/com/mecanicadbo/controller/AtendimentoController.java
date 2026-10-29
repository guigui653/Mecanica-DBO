package com.mecanicadbo.controller;

import com.mecanicadbo.dto.AtendimentoRequest;
import com.mecanicadbo.dto.AtendimentoResposta;
import com.mecanicadbo.service.AtendimentoService;
import com.mecanicadbo.service.OrdemServicoConsultaService;
import com.mecanicadbo.service.VeiculoConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atendimentos")
@RequiredArgsConstructor
@Tag(name = "Atendimentos", description = "Cliente, veículo e OS gravados juntos")
public class AtendimentoController {

    private final AtendimentoService service;
    private final VeiculoConsultaService veiculoConsulta;
    private final OrdemServicoConsultaService osConsulta;

    @PostMapping
    @Operation(summary = "Cadastra cliente e/ou veículo e abre a OS numa transação só",
               description = "Se qualquer parte falhar, nada é gravado. Sem 'os', grava só cliente e veículo.")
    public ResponseEntity<AtendimentoResposta> registrar(@RequestBody @Valid AtendimentoRequest pedido) {
        AtendimentoService.Resultado r = service.registrar(pedido);

        AtendimentoResposta resposta = new AtendimentoResposta(
                veiculoConsulta.buscarPorId(r.veiculoId()),
                r.osId() == null ? null : osConsulta.buscarCompleta(r.osId())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }
}
