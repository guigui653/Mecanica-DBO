package com.mecanicadbo.exception;

/**
 * Falha ao gerar o recibo em PDF (Python ausente, reportlab não instalado,
 * erro no script ou tempo limite estourado).
 *
 * A mensagem guarda o detalhe técnico para o log. O usuário recebe só o texto
 * genérico definido no GlobalExceptionHandler.
 */
public class GeracaoPdfException extends RuntimeException {
    public GeracaoPdfException(String mensagem) {
        super(mensagem);
    }

    public GeracaoPdfException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
