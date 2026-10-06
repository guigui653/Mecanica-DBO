package com.mecanicadbo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Erros do OCR e do upload da foto, no mesmo formato do GlobalExceptionHandler:
 * { "timestamp", "status", "mensagem" } — o front lê e.error?.mensagem.
 *
 * Fica numa classe separada, com HIGHEST_PRECEDENCE, por dois motivos:
 *  1) não mexe no GlobalExceptionHandler que já está funcionando;
 *  2) o MaxUploadSizeExceededException é lançado ANTES de o controller ser
 *     escolhido, então um @ExceptionHandler dentro do controller não o pega —
 *     e o handler genérico (Exception.class) do GlobalExceptionHandler o
 *     transformaria em um 500 sem explicação.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OcrExceptionHandler {

    @ExceptionHandler(OcrIndisponivelException.class)
    public ResponseEntity<Map<String, Object>> handleOcrIndisponivel(OcrIndisponivelException ex) {
        log.warn("OCR indisponível: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(corpo(503, ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleArquivoGrande(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(corpo(413, "A foto é grande demais. O limite é 10 MB — "
                        + "tire a foto de novo ou reduza a qualidade da câmera."));
    }

    private Map<String, Object> corpo(int status, String mensagem) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("mensagem", mensagem);
        return body;
    }
}
