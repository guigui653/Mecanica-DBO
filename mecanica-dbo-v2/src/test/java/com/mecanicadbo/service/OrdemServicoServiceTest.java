package com.mecanicadbo.service;

import com.mecanicadbo.dto.ItemPecaRequest;
import com.mecanicadbo.exception.RegraDeNegocioException;
import com.mecanicadbo.model.OrdemServico;
import com.mecanicadbo.model.StatusOs;
import com.mecanicadbo.repository.ItemPecaRepository;
import com.mecanicadbo.repository.ItemServicoRepository;
import com.mecanicadbo.repository.OrdemServicoRepository;
import com.mecanicadbo.repository.VeiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Regras de status e a trava de "OS entregue é documento fechado". */
@ExtendWith(MockitoExtension.class)
class OrdemServicoServiceTest {

    @Mock OrdemServicoRepository osRepo;
    @Mock VeiculoRepository veiculoRepo;
    @Mock ItemPecaRepository itemPecaRepo;
    @Mock ItemServicoRepository itemServicoRepo;
    @InjectMocks OrdemServicoService service;

    private OrdemServico os;

    @BeforeEach
    void setUp() {
        os = OrdemServico.builder().id(3L).status(StatusOs.ABERTA).dataEntrada(LocalDate.now()).build();
        lenient().when(osRepo.findById(3L)).thenReturn(Optional.of(os));
        lenient().when(osRepo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void naoEntregaSemConcluirAntes() {
        os.setStatus(StatusOs.EM_ANDAMENTO);
        assertThatThrownBy(() -> service.atualizarStatus(3L, StatusOs.ENTREGUE))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CONCLUIDA");
        assertThat(os.getStatus()).isEqualTo(StatusOs.EM_ANDAMENTO);
    }

    @Test
    void entregarGravaADataDeSaida() {
        os.setStatus(StatusOs.CONCLUIDA);
        service.atualizarStatus(3L, StatusOs.ENTREGUE);
        assertThat(os.getStatus()).isEqualTo(StatusOs.ENTREGUE);
        assertThat(os.getDataSaidaReal()).isEqualTo(LocalDate.now());
    }

    @Test
    void osEntregueSoVoltaParaConcluidaELimpaADataDeSaida() {
        os.setStatus(StatusOs.ENTREGUE);
        os.setDataSaidaReal(LocalDate.now());

        assertThatThrownBy(() -> service.atualizarStatus(3L, StatusOs.ABERTA))
                .isInstanceOf(RegraDeNegocioException.class);

        service.atualizarStatus(3L, StatusOs.CONCLUIDA);
        assertThat(os.getStatus()).isEqualTo(StatusOs.CONCLUIDA);
        assertThat(os.getDataSaidaReal()).isNull();
    }

    @Test
    void osEntregueNaoRecebePeca() {
        os.setStatus(StatusOs.ENTREGUE);
        ItemPecaRequest peca = new ItemPecaRequest("Filtro", BigDecimal.ONE, new BigDecimal("45.90"), false);

        assertThatThrownBy(() -> service.adicionarPeca(3L, peca))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("entregue");
        verify(itemPecaRepo, never()).save(any());
    }

    @Test
    void soApagaOsEntregue() {
        assertThatThrownBy(() -> service.excluir(3L)).isInstanceOf(RegraDeNegocioException.class);
        verify(osRepo, never()).delete(any());

        os.setStatus(StatusOs.ENTREGUE);
        service.excluir(3L);
        verify(osRepo).delete(os);
    }
}
