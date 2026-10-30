package com.mecanicadbo.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * O QUE MUDOU AQUI
 * ----------------
 * O FORMATO DA RESPOSTA CONTINUA EXATAMENTE O MESMO:
 *
 *     { "timestamp": "...", "status": 409, "mensagem": "...", "campos": {...} }
 *
 * Isso não é detalhe: o seu front lê e.error?.mensagem no mensagemDoErro().
 * Se o formato mudasse, toda mensagem de erro da tela viraria o texto padrão
 * "Erro inesperado". Por isso o método erro() ficou idêntico.
 *
 * O que mudou foi a COBERTURA. Antes havia 3 handlers; tudo que não fosse
 * RecursoNaoEncontrado, RegraDeNegocio ou validação de @RequestBody caía no
 * erro genérico do Spring: uma resposta com um formato diferente, sem o campo
 * mensagem — e o front mostrava "Erro inesperado" mesmo quando o problema era
 * óbvio (data digitada errada, CPF repetido, JSON malformado).
 *
 * Agora cada caso comum tem um handler que explica em português o que houve.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------
    // Os três originais — sem mudança de comportamento
    // ------------------------------------------------------------------

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro(404, ex.getMessage()));
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(RegraDeNegocioException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro(409, ex.getMessage()));
    }

    /** Validação dos DTOs anotados com @Valid no @RequestBody. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            campos.put(fe.getField(), fe.getDefaultMessage());
        }
        Map<String, Object> body = erro(400, "Dados inválidos");
        body.put("campos", campos);
        return ResponseEntity.badRequest().body(body);
    }

    // ------------------------------------------------------------------
    // Handlers novos
    // ------------------------------------------------------------------

    /**
     * Constraint do banco violada: índice único (CPF, placa), CHECK
     * (combustível, valor de serviço) ou chave estrangeira.
     *
     * Antes isso virava 500 com stack trace na tela. A mensagem crua do
     * PostgreSQL é ilegível para o seu pai, então aqui ela é traduzida pelo
     * nome da constraint — que é justamente o que o Postgres coloca no texto
     * do erro.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade no banco", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(erro(409, traduzirIntegridade(causaRaiz(ex))));
    }

    /**
     * JSON malformado, tipo errado no corpo ("quantidade": "abc") ou enum
     * inexistente. Era 400 sem campo mensagem — o front mostrava
     * "Erro inesperado" e você não sabia qual campo estava errado.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonIlegivel(HttpMessageNotReadableException ex) {
        log.warn("Corpo da requisição ilegível: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(erro(400,
                "Não foi possível ler os dados enviados. "
                        + "Verifique se algum número foi preenchido com texto ou se a data está no formato AAAA-MM-DD."));
    }

    /**
     * Parâmetro da URL com o tipo errado: /api/ordens/abc (id não numérico) ou
     * /periodo?inicio=01/02/2026 (data fora do formato ISO).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        String esperado = ex.getRequiredType() == null ? "outro tipo" : switch (ex.getRequiredType().getSimpleName()) {
            case "Long", "Integer", "Short" -> "um número inteiro";
            case "LocalDate"                -> "uma data no formato AAAA-MM-DD";
            case "BigDecimal", "Double"     -> "um número";
            case "StatusOs"                 -> "ABERTA, EM_ANDAMENTO, CONCLUIDA ou ENTREGUE";
            default                         -> ex.getRequiredType().getSimpleName();
        };
        return ResponseEntity.badRequest().body(erro(400,
                "O parâmetro '" + ex.getName() + "' recebeu o valor '" + ex.getValue()
                        + "', mas deveria ser " + esperado + "."));
    }

    /** Falta um @RequestParam obrigatório, como o status no PATCH de status. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleParametroFaltando(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(erro(400,
                "Falta o parâmetro obrigatório '" + ex.getParameterName() + "'."));
    }

    /** Validação em @RequestParam/@PathVariable (classe anotada com @Validated). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(ConstraintViolationException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v ->
                campos.put(String.valueOf(v.getPropertyPath()), v.getMessage()));
        Map<String, Object> body = erro(400, "Dados inválidos");
        body.put("campos", campos);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * URL que não existe. Vira 404 com o mesmo formato dos outros erros, em
     * vez da página de erro padrão do Spring — assim o front consegue ler a
     * mensagem quando alguém erra o endereço do endpoint.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRotaInexistente(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(erro(404, "Endereço não encontrado: " + ex.getResourcePath()));
    }

    /**
     * Falha ao gerar o recibo em PDF. O detalhe (saída do Python, traceback)
     * já foi gravado no log pelo PdfService; aqui só vai a mensagem amigável.
     */
    @ExceptionHandler(GeracaoPdfException.class)
    public ResponseEntity<Map<String, Object>> handleGeracaoPdf(GeracaoPdfException ex) {
        log.error("Falha na geração do PDF: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro(500,
                "Não foi possível gerar o recibo em PDF. O detalhe foi gravado no log do servidor."));
    }

    /**
     * Exceções de segurança lançadas DENTRO de um controller (ex.: @PreAuthorize).
     * Sem estes handlers, o handler genérico abaixo as transformaria em 500.
     * As que acontecem nos filtros (sem sessão, CSRF) são respondidas pelo SecurityConfig.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleNaoAutenticado(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(erro(401, "Sessão expirada ou usuário não autenticado."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAcessoNegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(erro(403, "Acesso negado."));
    }

    /**
     * Rede de segurança. Qualquer coisa não prevista acima vira 500 com uma
     * mensagem curta na tela e o stack trace COMPLETO no log do Spring.
     *
     * A mensagem devolvida é propositalmente genérica: detalhe interno de
     * exceção não deve aparecer para o usuário final. Quem precisa do detalhe
     * é você, e ele está no console do IntelliJ.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenerico(Exception ex) {
        log.error("Erro não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro(500,
                "Erro interno no sistema. O detalhe foi gravado no log do servidor."));
    }

    // ------------------------------------------------------------------

    /**
     * Formato de resposta — NÃO MUDAR.
     * O front depende das chaves status, mensagem e campos.
     */
    private Map<String, Object> erro(int status, String mensagem) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("mensagem", mensagem);
        return body;
    }

    private String causaRaiz(Throwable ex) {
        Throwable atual = ex;
        while (atual.getCause() != null && atual.getCause() != atual) {
            atual = atual.getCause();
        }
        return atual.getMessage() == null ? "" : atual.getMessage();
    }

    /**
     * Traduz a mensagem do PostgreSQL pelo nome da constraint que aparece nela.
     *
     * Se você criar outra constraint no banco, acrescente um if aqui — senão
     * o usuário cai na mensagem genérica do final.
     */
    private String traduzirIntegridade(String detalhe) {
        String d = detalhe.toLowerCase();

        if (d.contains("cliente_cpf"))                return "Já existe um cliente cadastrado com esse CPF.";
        // Antes do "veiculo_placa": o nome desta constraint também contém esse texto.
        if (d.contains("veiculo_placa_formato"))      return "Placa inválida: use só letras e números, como ABC1234 ou ABC1D23.";
        if (d.contains("veiculo_placa"))              return "Já existe um veículo cadastrado com essa placa.";
        if (d.contains("veiculo_chassi"))             return "Já existe um veículo cadastrado com esse chassi.";
        if (d.contains("combustivel"))                return "Combustível inválido. Use GASOLINA, ETANOL, FLEX, DIESEL, ELETRICO, HIBRIDO ou GNV.";
        if (d.contains("item_servico_valor"))         return "O valor do serviço não pode ser negativo.";
        if (d.contains("chk_datas"))                  return "A previsão de entrega não pode ser antes da data de entrada do carro.";
        if (d.contains("foreign key") || d.contains("chave estrangeira"))
            return "Não é possível concluir: este registro está ligado a outro (OS, veículo ou item).";
        if (d.contains("not null") || d.contains("não-nulo") || d.contains("nao-nulo"))
            return "Um campo obrigatório ficou em branco.";

        return "Os dados enviados conflitam com uma regra do banco de dados. Verifique CPF, placa e campos obrigatórios.";
    }
}
