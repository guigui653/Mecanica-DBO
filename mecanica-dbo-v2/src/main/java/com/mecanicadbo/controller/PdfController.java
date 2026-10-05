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
 * 1) O catch geral parou de engolir a exceção.
 *
 *    Antes era isto:
 *
 *        } catch (Exception e) {
 *            return ResponseEntity.status(500).build();
 *        }
 *
 *    Três problemas de uma vez: o erro não ia para o log (o e sumia), a
 *    resposta não tinha corpo nenhum (o front só via "500" e não sabia se o
 *    Python não estava instalado, se o script não foi achado ou se a OS não
 *    existia), e uma OS inexistente virava 500 em vez de 404.
 *
 *    Agora: RecursoNaoEncontradoException e RegraDeNegocioException — que são
 *    RuntimeException — passam direto para o GlobalExceptionHandler e viram
 *    404/409 com a mensagem certa. Só o que sobra (Python ausente, script
 *    faltando, erro do ReportLab) cai no catch, é logado com stack trace
 *    completo e volta com um texto que dá para ler na tela.
 *
 * 2) Saiu o produces = MediaType.APPLICATION_PDF_VALUE.
 *
 *    Esse produces é uma promessa: "este endpoint só devolve PDF". Se o
 *    navegador manda Accept: application/pdf e o método tenta responder um
 *    texto de erro, o Spring recusa com 406 Not Acceptable — e aí o erro real
 *    é substituído por outro erro, ainda mais confuso. Sem o produces, o
 *    Content-Type é definido em tempo de execução: application/pdf quando dá
 *    certo, text/plain quando dá errado.
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
            // OS não encontrada, regra de negócio, etc. Deixa subir: o
            // GlobalExceptionHandler transforma no status e na mensagem certos.
            throw e;

        } catch (Exception e) {
            // gerarRecibo() declara throws Exception (roda processo externo e
            // mexe em arquivo), então o compilador obriga este catch.
            log.error("Falha ao gerar PDF da OS {}", id, e);

            String mensagem = """
                    Não foi possível gerar o PDF da OS %d.

                    Causa: %s

                    O que verificar:
                      - o Python 3.12 está no caminho configurado no PdfService;
                      - a biblioteca reportlab está instalada nesse Python
                        (python3.12 -m pip install reportlab);
                      - o script gerar_recibo.py está na pasta esperada.

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
