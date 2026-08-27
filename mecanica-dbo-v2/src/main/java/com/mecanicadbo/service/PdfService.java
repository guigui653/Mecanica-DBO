package com.mecanicadbo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mecanicadbo.model.OrdemServico;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final OrdemServicoService osService;

    public byte[] gerarRecibo(Long osId) throws Exception {
        // 1. Busca a OS completa
        OrdemServico os = osService.buscarPorId(osId);

        // 2. Serializa para JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String osJson = mapper.writeValueAsString(os);

        // 3. Caminho do script Python (dentro do jar/resources)
        //    Em dev: src/main/resources/scripts/gerar_recibo.py
        String scriptPath = resolverCaminhoScript();

        // 4. Arquivo temporário de saída
        Path pdfTemp = Files.createTempFile("recibo_os_" + osId + "_", ".pdf");

        try {
            // 5. Executa o script Python
            C:\Users\gbeze\AppData\Local\Microsoft\WindowsApps\PythonSoftwareFoundation.Python.3.12_qbz5n2kfra8p0\python3.12.exe -c "import reportlab; print('ok')"
            pb.redirectErrorStream(true);
            Process processo = pb.start();

            // Captura saída para log
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(processo.getInputStream()))) {
                br.lines().forEach(System.out::println);
            }

            int exitCode = processo.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException(
                    "Erro ao gerar PDF (exit code: " + exitCode + ")");
            }

            // 6. Lê o PDF gerado e retorna como bytes
            return Files.readAllBytes(pdfTemp);

        } finally {
            // 7. Remove arquivo temporário
            Files.deleteIfExists(pdfTemp);
        }
    }

    private String resolverCaminhoScript() throws IOException {
        // Em desenvolvimento o Maven copia resources para target/classes
        String[] candidatos = {
                "target/classes/scripts/gerar_recibo.py",
                "src/main/resources/scripts/gerar_recibo.py",
        };

        for (String path : candidatos) {
            File f = new File(path);
            if (f.exists()) {
                System.out.println("Script encontrado: " + f.getAbsolutePath());
                return f.getAbsolutePath();
            }
        }

        // Fallback: extrai do classpath para arquivo temporário
        try (InputStream is = getClass()
                .getResourceAsStream("/scripts/gerar_recibo.py")) {
            if (is == null) throw new IOException(
                    "Script gerar_recibo.py não encontrado no classpath");
            Path temp = Files.createTempFile("gerar_recibo_", ".py");
            Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Script extraído para: " + temp);
            return temp.toString();
        }
    }
}
