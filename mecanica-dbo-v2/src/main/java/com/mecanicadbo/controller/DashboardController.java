package com.mecanicadbo.controller;

import com.mecanicadbo.dto.DashboardResumoDTO;
import com.mecanicadbo.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Painel", description = "Números do painel do dia")
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/resumo")
    @Operation(summary = "Contadores, pátio, prontas, faturamento e clientes novos do mês")
    public DashboardResumoDTO resumo() {
        return service.resumo(LocalDate.now());
    }
}
