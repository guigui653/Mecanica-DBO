package com.mecanicadbo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanicadbo.dto.OrdemServicoDTO;
import com.mecanicadbo.service.pdf.ReciboMontador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * Gera o recibo da OS em PDF, só com Java. Sem Python, sem processo externo,
 * sem arquivo temporário.
 *
 * Fluxo:
 *   OS (banco) -> OrdemServicoDTO -> Map -> ReciboMontador (textos prontos)
 *      -> Thymeleaf aplica em templates/pdf/recibo.html -> XHTML
 *      -> Flying Saucer converte o XHTML em PDF.
 *
 * Para mudar o visual do recibo, edite src/main/resources/templates/pdf/recibo.html.
 *
 * Por que um TemplateEngine próprio e não o do Spring MVC: o recibo precisa de
 * XML bem formado (exigência do Flying Saucer), e este engine é só para isso.
 * Assim não se mistura com nenhuma tela HTML que o projeto venha a ter.
 */
@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final OrdemServicoConsultaService consulta;
    private final ObjectMapper mapper;
    private final TemplateEngine engine;
    private final Map<String, String> oficina;

    public PdfService(OrdemServicoConsultaService consulta,
                      ObjectMapper mapper,
                      @Value("${app.oficina.nome:Mecânica DBO}") String nome,
                      @Value("${app.oficina.endereco:}") String endereco,
                      @Value("${app.oficina.telefone:}") String telefone) {
        this.consulta = consulta;
        this.mapper = mapper;
        this.oficina = Map.of("nome", nome, "endereco", endereco, "telefone", telefone);

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/pdf/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);

        this.engine = new TemplateEngine();
        this.engine.setTemplateResolver(resolver);
    }

    /**
     * @throws com.mecanicadbo.exception.RecursoNaoEncontradoException se a OS não existir (vira 404)
     * @throws Exception qualquer falha de template ou de conversão (o PdfController devolve 500 com texto)
     */
    public byte[] gerarRecibo(Long osId) throws Exception {
        // Lança RecursoNaoEncontradoException se a OS não existir.
        OrdemServicoDTO dto = consulta.buscarCompleta(osId);

        // O mesmo JSON que o Angular recebe em GET /api/ordens/{id}.
        Map<String, Object> os = mapper.convertValue(dto, new TypeReference<Map<String, Object>>() {});

        Context ctx = new Context(PT_BR);
        ctx.setVariable("r", ReciboMontador.montar(os, oficina, LocalDateTime.now()));

        try {
            String xhtml = engine.process("recibo", ctx);

            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            renderer.createPDF(saida);
            return saida.toByteArray();

        } catch (Exception e) {
            log.error("Falha ao montar o PDF da OS {}", osId, e);
            throw e;
        }
    }
}
