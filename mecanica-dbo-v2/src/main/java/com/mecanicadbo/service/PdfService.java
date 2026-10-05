package com.mecanicadbo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mecanicadbo.exception.GeracaoPdfException;
import com.mecanicadbo.model.*;
import com.mecanicadbo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    /** Tempo máximo que o script Python pode levar para gerar o recibo. */
    private static final long TEMPO_LIMITE_SEGUNDOS = 30;

    private final OrdemServicoService osService;
    private final ItemPecaRepository itemPecaRepo;
    private final ItemServicoRepository itemServicoRepo;

    /**
     * Executável do Python que tem o reportlab instalado.
     * Configure em application.properties (app.pdf.python=...). Padrão: "python" do PATH.
     */
    @Value("${app.pdf.python:python}")
    private String python;

    @Transactional(readOnly = true)
    public byte[] gerarRecibo(Long osId) throws Exception {
        OrdemServico os = osService.buscarPorId(osId);
        Veiculo veiculo = os.getVeiculo();
        Cliente cliente = veiculo.getCliente();

        // Monta o mapa manualmente para evitar @JsonIgnore
        Map<String, Object> osMap = new LinkedHashMap<>();
        osMap.put("id", os.getId());
        osMap.put("dataEntrada", os.getDataEntrada());
        osMap.put("dataSaidaReal", os.getDataSaidaReal());
        osMap.put("dataSaidaPrevista", os.getDataSaidaPrevista());
        osMap.put("kmEntrada", os.getKmEntrada());
        osMap.put("status", os.getStatus());
        osMap.put("reclamacoes", os.getReclamacoes());
        osMap.put("diagnostico", os.getDiagnostico());
        osMap.put("totalPecas", os.getTotalPecas());
        osMap.put("totalServicos", os.getTotalServicos());
        osMap.put("totalGeral", os.getTotalGeral());

        // Cliente
        Map<String, Object> clienteMap = new LinkedHashMap<>();
        clienteMap.put("nome", cliente.getNome());
        clienteMap.put("cpf", cliente.getCpf());
        clienteMap.put("telefone1", cliente.getTelefone1());

        // Veículo
        Map<String, Object> veiculoMap = new LinkedHashMap<>();
        veiculoMap.put("placa", veiculo.getPlaca());
        veiculoMap.put("marca", veiculo.getMarca());
        veiculoMap.put("modelo", veiculo.getModelo());
        veiculoMap.put("cor", veiculo.getCor());
        veiculoMap.put("combustivel", veiculo.getCombustivel());
        veiculoMap.put("anoFabricacao", veiculo.getAnoFabricacao());
        veiculoMap.put("cliente", clienteMap);
        osMap.put("veiculo", veiculoMap);

        // Peças
        List<Map<String, Object>> pecasList = new ArrayList<>();
        for (ItemPeca p : itemPecaRepo.findByOrdemServicoId(osId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("descricao", p.getDescricao());
            m.put("quantidade", p.getQuantidade());
            m.put("valorUnitario", p.getValorUnitario());
            m.put("valorTotal", p.getValorTotal());
            m.put("pagoPeloCliente", p.getPagoPeloCliente());
            pecasList.add(m);
        }
        osMap.put("itensPeca", pecasList);

        // Serviços
        List<Map<String, Object>> servicosList = new ArrayList<>();
        for (ItemServico s : itemServicoRepo.findByOrdemServicoId(osId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("descricao", s.getDescricao());
            m.put("valor", s.getValor());
            servicosList.add(m);
        }
        osMap.put("itensServico", servicosList);

        // Serializa para JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String osJson = mapper.writeValueAsString(osMap);

        // Salva JSON em arquivo temporário
        Path jsonTemp = Files.createTempFile("os_json_", ".json");
        Files.writeString(jsonTemp, osJson, StandardCharsets.UTF_8);

        String scriptPath = resolverCaminhoScript();
        Path pdfTemp = Files.createTempFile("recibo_os_" + osId + "_", ".pdf");
        // A saída do Python vai para um arquivo, e não para um pipe lido em
        // laço: assim o waitFor com tempo limite funciona mesmo se o script travar.
        Path saidaTemp = Files.createTempFile("recibo_saida_" + osId + "_", ".txt");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    python, scriptPath,
                    jsonTemp.toString(),
                    pdfTemp.toString()
            );
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.redirectErrorStream(true);
            pb.redirectOutput(saidaTemp.toFile());

            Process processo;
            try {
                processo = pb.start();
            } catch (IOException e) {
                throw new GeracaoPdfException("Não foi possível executar o Python '" + python
                        + "'. Verifique a propriedade app.pdf.python. Causa: " + e.getMessage(), e);
            }

            boolean terminou;
            try {
                terminou = processo.waitFor(TEMPO_LIMITE_SEGUNDOS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                processo.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new GeracaoPdfException("Geração do PDF da OS " + osId + " foi interrompida.", e);
            }

            String saida = lerSaida(saidaTemp);

            if (!terminou) {
                processo.destroyForcibly();
                log.error("Python excedeu {}s ao gerar o PDF da OS {}. Saída:\n{}",
                        TEMPO_LIMITE_SEGUNDOS, osId, saida);
                throw new GeracaoPdfException("Tempo limite de " + TEMPO_LIMITE_SEGUNDOS
                        + "s excedido ao gerar o PDF da OS " + osId + ".");
            }

            int exitCode = processo.exitValue();
            if (exitCode != 0) {
                log.error("Python terminou com código {} ao gerar o PDF da OS {}. Saída:\n{}",
                        exitCode, osId, saida);
                throw new GeracaoPdfException("Erro ao gerar PDF da OS " + osId
                        + " (código de saída " + exitCode + ").");
            }

            log.debug("Saída do gerador de PDF da OS {}: {}", osId, saida);
            return Files.readAllBytes(pdfTemp);

        } finally {
            Files.deleteIfExists(pdfTemp);
            Files.deleteIfExists(jsonTemp);
            Files.deleteIfExists(saidaTemp);
        }
    }

    private String lerSaida(Path arquivo) {
        try {
            return Files.readString(arquivo, StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            return "(não foi possível ler a saída do Python: " + e.getMessage() + ")";
        }
    }

    private String resolverCaminhoScript() throws IOException {
        String[] candidatos = {
                "target/classes/scripts/gerar_recibo.py",
                "src/main/resources/scripts/gerar_recibo.py"
        };
        for (String path : candidatos) {
            File f = new File(path);
            if (f.exists()) return f.getAbsolutePath();
        }
        try (InputStream is = getClass().getResourceAsStream("/scripts/gerar_recibo.py")) {
            if (is == null) throw new IOException("Script nao encontrado");
            Path temp = Files.createTempFile("gerar_recibo_", ".py");
            Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp.toString();
        }
    }
}