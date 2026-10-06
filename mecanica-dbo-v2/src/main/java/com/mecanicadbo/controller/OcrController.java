package com.mecanicadbo.controller;

import com.mecanicadbo.dto.OcrResposta;
import com.mecanicadbo.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Leitura de nota fiscal por foto.
 *
 * Só LÊ e devolve sugestões. Quem grava as peças é o endpoint que já existe,
 * POST /api/ordens/{id}/pecas, chamado pela tela depois que o mecânico revisa.
 * Assim a regra "OS entregue não pode ser alterada" continua valendo sozinha.
 *
 * Exige login como qualquer outro /api/** (SecurityConfig: anyRequest().authenticated()).
 */
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
@Tag(name = "OCR de notas fiscais")
public class OcrController {

    private final OcrService ocrService;

    @PostMapping(value = "/nota", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Lê uma foto de nota fiscal e sugere os itens (não grava nada)")
    public OcrResposta lerNota(@RequestParam("file") MultipartFile file) {
        return ocrService.lerNota(file);
    }

    @GetMapping("/status")
    @Operation(summary = "Informa se o serviço de OCR está no ar")
    public Map<String, Boolean> status() {
        return Map.of("disponivel", ocrService.disponivel());
    }
}
