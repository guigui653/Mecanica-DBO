package com.mecanicadbo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mecanicadbo.model.*;
import com.mecanicadbo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final OrdemServicoService osService;
    private final ItemPecaRepository itemPecaRepo;
    private final ItemServicoRepository itemServicoRepo;

    private static final String PYTHON =
            "C:\\Users\\gbeze\\AppData\\Local\\Microsoft\\WindowsApps\\" +
                    "PythonSoftwareFoundation.Python.3.12_qbz5n2kfra8p0\\python3.12.exe";

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

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    PYTHON, scriptPath,
                    jsonTemp.toString(),
                    pdfTemp.toString()
            );
            pb.redirectErrorStream(true);
            Process processo = pb.start();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(processo.getInputStream()))) {
                br.lines().forEach(System.out::println);
            }

            int exitCode = processo.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Erro ao gerar PDF (exit code: " + exitCode + ")");
            }

            return Files.readAllBytes(pdfTemp);

        } finally {
            Files.deleteIfExists(pdfTemp);
            Files.deleteIfExists(jsonTemp);
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