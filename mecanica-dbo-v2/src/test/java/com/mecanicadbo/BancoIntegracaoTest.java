package com.mecanicadbo;

import com.mecanicadbo.dto.*;
import com.mecanicadbo.exception.RegraDeNegocioException;
import com.mecanicadbo.model.NotaFiscal;
import com.mecanicadbo.model.OrdemServico;
import com.mecanicadbo.repository.ClienteRepository;
import com.mecanicadbo.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes contra um PostgreSQL de verdade: Flyway, trigger dos totais, coluna
 * gerada, enums nativos e rollback do atendimento — o que mock nenhum cobre.
 *
 * Só rodam com a variável TESTE_DB_URL apontando para um banco DESCARTÁVEL
 * (eles gravam dados). NUNCA aponte para o mecanica_dbo. Exemplo:
 *
 *   createdb -U postgres mecanica_dbo_teste
 *   set TESTE_DB_URL=jdbc:postgresql://localhost:5432/mecanica_dbo_teste
 *   mvn test
 *
 * Sem a variável, o `mvn test` comum pula esta classe.
 */
@EnabledIfEnvironmentVariable(named = "TESTE_DB_URL", matches = "jdbc:postgresql:.*(?<!/mecanica_dbo)$")
@SpringBootTest(properties = {
        "spring.datasource.url=${TESTE_DB_URL}",
        "app.admin.password=teste-integracao",
        "spring.jpa.show-sql=false"
})
class BancoIntegracaoTest {

    @Autowired AtendimentoService atendimento;
    @Autowired OrdemServicoService ordens;
    @Autowired OrdemServicoConsultaService consulta;
    @Autowired ClienteService clientes;
    @Autowired DashboardService dashboard;
    @Autowired ClienteRepository clienteRepo;
    @Autowired EntityManager em;
    @Autowired TransactionTemplate tx;

    /** Placa única por execução, para o teste poder rodar várias vezes no mesmo banco. */
    private static String placaNova() {
        return "TST" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private Long abrirOs(String nomeCliente, String placa) {
        var r = atendimento.registrar(new AtendimentoRequest(
                null, new ClienteRequest(nomeCliente, null, "61900000000", null, null),
                null, new VeiculoRequest(placa, "Fiat", "Uno", null, "Branco", "HIBRIDO", null),
                new CriarOsRequest(1000, null, null, "teste de integração", null)));
        return r.osId();
    }

    @Test
    void triggerCalculaTotaisComCreditoNegativoEPecaDoCliente() {
        Long osId = abrirOs("Integração Totais", placaNova());

        ordens.adicionarPeca(osId, new ItemPecaRequest("Filtro", new BigDecimal("2"), new BigDecimal("10.00"), false));
        ordens.adicionarPeca(osId, new ItemPecaRequest("Crédito mecânico", null, new BigDecimal("-5.00"), false));
        ordens.adicionarPeca(osId, new ItemPecaRequest("Pneu do cliente", null, new BigDecimal("100.00"), true));
        ordens.adicionarServico(osId, new ItemServicoRequest("Mão de obra", new BigDecimal("30.00")));

        OrdemServicoDTO os = consulta.buscarCompleta(osId);
        assertThat(os.totalPecas()).isEqualByComparingTo("15.00");     // 20 - 5 (o pneu fica fora)
        assertThat(os.totalServicos()).isEqualByComparingTo("30.00");
        assertThat(os.totalGeral()).isEqualByComparingTo("45.00");
        assertThat(os.itensPeca()).extracting(ItemPecaDTO::valorTotal)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(new BigDecimal("20"), new BigDecimal("-5"), new BigDecimal("100"));
    }

    @Test
    void atendimentoComPlacaRepetidaNaoDeixaClienteOrfao() {
        String placa = placaNova();
        abrirOs("Integração Dono Original", placa);
        long antes = clienteRepo.count();

        assertThatThrownBy(() -> abrirOs("Integração Órfão", placa.substring(0, 3) + "-" + placa.substring(3)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Placa já cadastrada");

        assertThat(clienteRepo.count()).isEqualTo(antes);
    }

    @Test
    void notaFiscalGravaComOEnumNativoDoBanco() {
        Long osId = abrirOs("Integração Nota", placaNova());

        Long nfId = tx.execute(s -> {
            NotaFiscal nf = NotaFiscal.builder()
                    .ordemServico(em.getReference(OrdemServico.class, osId))
                    .fornecedor("Auto Peças Teste")
                    .ocrStatus(NotaFiscal.StatusOcr.PROCESSADO)
                    .build();
            em.persist(nf);
            em.flush();
            return nf.getId();
        });

        NotaFiscal lida = tx.execute(s -> em.find(NotaFiscal.class, nfId));
        assertThat(lida.getOcrStatus()).isEqualTo(NotaFiscal.StatusOcr.PROCESSADO);
    }

    @Test
    void clienteComHistoricoNaoEApagado() {
        Long osId = abrirOs("Integração Histórico", placaNova());
        Long clienteId = consulta.buscarCompleta(osId).veiculo().cliente().id();

        assertThatThrownBy(() -> clientes.excluir(clienteId))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Inativar");

        clientes.inativar(clienteId);
        assertThat(clienteRepo.findById(clienteId)).get()
                .extracting(c -> c.getAtivo()).isEqualTo(false);
    }

    @Test
    void painelContaOsDoPatio() {
        Long osId = abrirOs("Integração Painel", placaNova());

        DashboardResumoDTO r = dashboard.resumo(LocalDate.now());

        assertThat(r.noPatio()).isGreaterThanOrEqualTo(1);
        assertThat(r.patio()).extracting(OrdemServicoDTO::id).contains(osId);
        assertThat(r.faturamentoMes()).isNotNull();
    }
}
