package com.mecanicadbo.controller;

import com.mecanicadbo.service.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

/**
 * O QUE MUDOU AQUI
 * ----------------
 * Só o texto da mensagem de erro. A versão anterior mandava o usuário conferir
 * o Python e o ReportLab, que não fazem mais parte do caminho do PDF.
 * O resto (rethrow de RuntimeException para virar 404/409, ausência de
 * produces, corpo text/plain no erro) continua igual.
 */
@Slf4j
@RestController
@RequestMapping({"/api/ordens", "/api/ordens-servico"})
@RequiredArgsConstructor
@Tag(name = "Ordens de Serviço")
public class PdfController {

    private final PdfService pdfService;

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Gerar PDF do recibo de uma OS")
    public ResponseEntity<byte[]> gerarPdf(@PathVariable Long id) {
        try {
            byte[] pdf = pdfService.gerarRecibo(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("recibo_os_" + id + ".pdf")
                            .build()
            );
            headers.setContentLength(pdf.length);

            return ResponseEntity.ok().headers(headers).body(pdf);

        } catch (RuntimeException e) {
            // OS não encontrada, regra de negócio etc.: o GlobalExceptionHandler
            // transforma no status e na mensagem certos.
            throw e;

        } catch (Exception e) {
            log.error("Falha ao gerar PDF da OS {}", id, e);

            String mensagem = """
                    Não foi possível gerar o PDF da OS %d.

                    Causa: %s

                    O erro completo está no log do Spring.
                    """.formatted(id, e.getClass().getSimpleName() + ": " + e.getMessage());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .headers(headers)
                    .body(mensagem.getBytes(StandardCharsets.UTF_8));
        }
    }
}
