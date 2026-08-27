package com.mecanicadbo.controller;

import com.mecanicadbo.service.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ordens")
@RequiredArgsConstructor
@Tag(name = "Ordens de Serviço")
public class PdfController {

    private final PdfService pdfService;

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
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

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
