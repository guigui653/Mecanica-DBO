package com.mecanicadbo.exception;

/**
 * O serviço Python de OCR não respondeu (desligado, porta errada, travou).
 * Vira 503 com mensagem em português no OcrExceptionHandler.
 */
public class OcrIndisponivelException extends RuntimeException {
    public OcrIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public OcrIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
