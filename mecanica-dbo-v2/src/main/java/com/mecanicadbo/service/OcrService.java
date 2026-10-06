package com.mecanicadbo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanicadbo.dto.OcrResposta;
import com.mecanicadbo.exception.OcrIndisponivelException;
import com.mecanicadbo.exception.RegraDeNegocioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

/**
 * Ponte entre o Spring e o microserviço Python de OCR (ocr_service.py).
 *
 * O Angular nunca fala direto com o Python: a foto passa por aqui, depois do
 * login e do CSRF, e o endereço do OCR fica escondido no servidor. A imagem vai
 * ao Python em JSON/base64, formato que o ocr_service.py já aceita.
 *
 * Este serviço NÃO grava nada. O resultado é só sugestão para revisão.
 */
@Slf4j
@Service
public class OcrService {

    private static final long TAMANHO_MAXIMO = 10L * 1024 * 1024;   // 10 MB, igual ao multipart
    private static final Set<String> TIPOS_ACEITOS =
            Set.of("image/jpeg", "image/jpg", "image/png", "image/webp", "image/bmp");

    private final ObjectMapper mapper;
    private final HttpClient http;
    private final URI urlProcessar;
    private final URI urlSaude;

    public OcrService(ObjectMapper mapper,
                      @Value("${app.ocr.url:http://127.0.0.1:5000}") String baseUrl) {
        this.mapper = mapper;
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.urlProcessar = URI.create(base + "/ocr/processar");
        this.urlSaude = URI.create(base + "/health");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /** Lê a nota fiscal da foto e devolve os itens sugeridos. */
    public OcrResposta lerNota(MultipartFile arquivo) {
        validar(arquivo);

        byte[] imagem;
        try {
            imagem = arquivo.getBytes();
        } catch (IOException e) {
            throw new RegraDeNegocioException("Não foi possível ler o arquivo enviado. Tente enviar a foto de novo.");
        }

        String corpo;
        try {
            corpo = mapper.writeValueAsString(
                    Map.of("imagem", Base64.getEncoder().encodeToString(imagem)));
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao montar a requisição do OCR", e);
        }

        HttpRequest requisicao = HttpRequest.newBuilder(urlProcessar)
                .timeout(Duration.ofSeconds(60))              // Tesseract em foto grande pode levar alguns segundos
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resposta;
        try {
            resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OcrIndisponivelException("A leitura da nota foi interrompida. Tente de novo.", e);
        } catch (IOException e) {
            throw new OcrIndisponivelException(
                    "O leitor de notas (OCR) não respondeu. Confira se o serviço Python está ligado "
                  + "(python ocr_service.py) e tente de novo.", e);
        }

        int status = resposta.statusCode();
        if (status == 400) {
            // Imagem ilegível / base64 inválido: culpa da foto, não do servidor.
            throw new RegraDeNegocioException(
                    "Não consegui abrir essa imagem. Envie uma foto em JPG ou PNG.");
        }
        if (status != 200) {
            log.error("OCR respondeu {} : {}", status, resposta.body());
            throw new OcrIndisponivelException(
                    "O leitor de notas falhou ao processar a foto. O detalhe foi gravado no log do servidor.");
        }

        try {
            return mapper.readValue(resposta.body(), OcrResposta.class);
        } catch (IOException e) {
            log.error("Resposta do OCR em formato inesperado: {}", resposta.body(), e);
            throw new OcrIndisponivelException(
                    "O leitor de notas devolveu uma resposta que o sistema não entendeu. "
                  + "O detalhe foi gravado no log do servidor.", e);
        }
    }

    /** true se o Python responde em /health. Usado para avisar na tela antes de enviar a foto. */
    public boolean disponivel() {
        try {
            HttpRequest req = HttpRequest.newBuilder(urlSaude)
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) return false;
            JsonNode json = mapper.readTree(r.body());
            return "ok".equals(json.path("status").asText());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraDeNegocioException("Escolha a foto da nota fiscal.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new RegraDeNegocioException("A foto passa de 10 MB. Tire de novo com qualidade menor.");
        }
        String tipo = arquivo.getContentType() == null ? "" : arquivo.getContentType().toLowerCase();
        if (!TIPOS_ACEITOS.contains(tipo)) {
            throw new RegraDeNegocioException("Formato não aceito. Envie a nota como foto JPG, PNG ou WEBP.");
        }
    }
}
