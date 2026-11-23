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
        "app.cpf.chave=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=",   // chave só de teste
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
    void editarPecaEServicoRecalculaOsTotais() {
        Long osId = abrirOs("Integração Editar Itens", placaNova());
        ordens.adicionarPeca(osId, new ItemPecaRequest("Filtro", new BigDecimal("2"), new BigDecimal("10.00"), false));
        ordens.adicionarServico(osId, new ItemServicoRequest("Mão de obra", new BigDecimal("30.00")));
        OrdemServicoDTO antes = consulta.buscarCompleta(osId);
        Long pecaId = antes.itensPeca().get(0).id();
        Long servId = antes.itensServico().get(0).id();

        ordens.atualizarPeca(osId, pecaId, new ItemPecaRequest("Filtro de óleo", new BigDecimal("3"), new BigDecimal("12.50"), false));
        ordens.atualizarServico(osId, servId, new ItemServicoRequest("Troca de óleo", new BigDecimal("50.00")));

        OrdemServicoDTO os = consulta.buscarCompleta(osId);
        assertThat(os.itensPeca().get(0).descricao()).isEqualTo("Filtro de óleo");
        assertThat(os.itensPeca().get(0).valorTotal()).isEqualByComparingTo("37.50");
        assertThat(os.itensServico().get(0).descricao()).isEqualTo("Troca de óleo");
        assertThat(os.totalGeral()).isEqualByComparingTo("87.50");   // 37,50 + 50,00
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
    void cpfFicaIlegivelNoBancoEVoltaAoLerOCliente() {
        // CPF único por execução (11 números), para o teste poder rodar várias vezes.
        String sorteado;
        do {   // CPF aleatório com os dígitos verificadores certos
            sorteado = String.format("%011d", ThreadLocalRandom.current().nextLong(10_000_000_000L, 99_999_999_999L));
        } while (!com.mecanicadbo.service.Documentos.cpfValido(sorteado));
        final String cpf = sorteado;
        String formatado = cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);

        Long id = clientes.criar(new ClienteRequest("Integração CPF", formatado, "61900000000", null, null)).getId();

        // Direto no banco: só o texto cifrado e o hash, nenhum pedaço do número.
        Object[] linha = tx.execute(s -> (Object[]) em.createNativeQuery(
                "SELECT cpf_cifrado, cpf_hash FROM cliente WHERE id = :id").setParameter("id", id).getSingleResult());
        assertThat((String) linha[0]).startsWith("v1:").doesNotContain(cpf).doesNotContain(cpf.substring(3, 9));
        assertThat((String) linha[1]).hasSize(64).doesNotContain(cpf);

        // Pela aplicação: o CPF volta inteiro, e a busca pelo CPF completo acha o cliente.
        assertThat(clientes.buscarPorId(id).getCpf()).isEqualTo(cpf);
        assertThat(clientes.buscar(formatado)).extracting(c -> c.getId()).contains(id);

        // O mesmo CPF digitado sem pontos é barrado como repetido.
        assertThatThrownBy(() -> clientes.criar(new ClienteRequest("Integração CPF 2", cpf, "61900000001", null, null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CPF já cadastrado");
    }

    @Test
    void painelContaOsDoPatio() {
        Long osId = abrirOs("Integração Painel", placaNova());

        DashboardResumoDTO r = dashboard.resumo(LocalDate.now());

        assertThat(r.noPatio()).isGreaterThanOrEqualTo(1);
        assertThat(r.patio()).extracting(OrdemServicoDTO::id).contains(osId);
        assertThat(r.faturamentoMes()).isNotNull();

        assertThat(r.faturamentoMensal()).hasSize(6);
        var mesAtual = r.faturamentoMensal().get(5);
        assertThat(mesAtual.ano()).isEqualTo(LocalDate.now().getYear());
        assertThat(mesAtual.mes()).isEqualTo(LocalDate.now().getMonthValue());
        assertThat(mesAtual.total()).isEqualByComparingTo(r.faturamentoMes());
        assertThat(r.osPorStatus()).containsKeys(com.mecanicadbo.model.StatusOs.values());
        assertThat(r.osPorStatus().get(com.mecanicadbo.model.StatusOs.ABERTA)).isGreaterThanOrEqualTo(1);
        assertThat(r.ultimasOs()).hasSizeLessThanOrEqualTo(5).extracting(OrdemServicoDTO::id).first().isEqualTo(osId);
        assertThat(r.totalVeiculos()).isGreaterThanOrEqualTo(1);
    }
}
