package com.mecanicadbo.service;

import com.mecanicadbo.dto.AtendimentoRequest;
import com.mecanicadbo.dto.ClienteRequest;
import com.mecanicadbo.dto.CriarOsRequest;
import com.mecanicadbo.dto.VeiculoRequest;
import com.mecanicadbo.exception.RegraDeNegocioException;
import com.mecanicadbo.model.Cliente;
import com.mecanicadbo.model.OrdemServico;
import com.mecanicadbo.model.Veiculo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** Quais combinações o POST /api/atendimentos aceita e em que ordem grava. */
@ExtendWith(MockitoExtension.class)
class AtendimentoServiceTest {

    @Mock ClienteService clienteService;
    @Mock VeiculoService veiculoService;
    @Mock OrdemServicoService ordemServicoService;
    @InjectMocks AtendimentoService service;

    private static final ClienteRequest CLIENTE = new ClienteRequest("Teste Transacao", null, "61900000002", null, null);
    private static final VeiculoRequest VEICULO = new VeiculoRequest("TST1A23", "Fiat", "Uno", null, "Branco", "FLEX", null);
    private static final CriarOsRequest OS = new CriarOsRequest(45000, null, null, "Barulho", null);

    @Test
    void tudoNovoGravaClienteDepoisVeiculoDepoisOs() {
        when(clienteService.criar(CLIENTE)).thenReturn(Cliente.builder().id(9L).build());
        when(veiculoService.criar(9L, VEICULO)).thenReturn(Veiculo.builder().id(6L).build());
        when(ordemServicoService.criar(6L, OS)).thenReturn(OrdemServico.builder().id(21L).build());

        var r = service.registrar(new AtendimentoRequest(null, CLIENTE, null, VEICULO, OS));

        assertThat(r).isEqualTo(new AtendimentoService.Resultado(6L, 21L));
        InOrder ordem = inOrder(clienteService, veiculoService, ordemServicoService);
        ordem.verify(clienteService).criar(CLIENTE);
        ordem.verify(veiculoService).criar(9L, VEICULO);
        ordem.verify(ordemServicoService).criar(6L, OS);
    }

    @Test
    void semOsGravaSoClienteEVeiculo() {
        when(veiculoService.criar(7L, VEICULO)).thenReturn(Veiculo.builder().id(8L).build());

        var r = service.registrar(new AtendimentoRequest(7L, null, null, VEICULO, null));

        assertThat(r).isEqualTo(new AtendimentoService.Resultado(8L, null));
        verifyNoInteractions(clienteService, ordemServicoService);
    }

    @Test
    void veiculoExistenteAbreSoAOs() {
        when(ordemServicoService.criar(3L, OS)).thenReturn(OrdemServico.builder().id(11L).build());

        var r = service.registrar(new AtendimentoRequest(null, null, 3L, null, OS));

        assertThat(r.osId()).isEqualTo(11L);
        verifyNoInteractions(clienteService, veiculoService);
    }

    @Test
    void combinacoesInvalidasNaoGravamNada() {
        // os dois tipos de cliente
        assertRecusa(new AtendimentoRequest(7L, CLIENTE, null, VEICULO, OS), "dono do veículo");
        // nenhum cliente
        assertRecusa(new AtendimentoRequest(null, null, null, VEICULO, OS), "dono do veículo");
        // veículo existente e novo juntos
        assertRecusa(new AtendimentoRequest(null, null, 3L, VEICULO, OS), "não os dois");
        // veículo existente sem OS
        assertRecusa(new AtendimentoRequest(null, null, 3L, null, null), "ordem de serviço");
        // sem veículo nenhum
        assertRecusa(new AtendimentoRequest(7L, null, null, null, OS), "Informe o veículo");
        // relato em branco
        assertRecusa(new AtendimentoRequest(null, null, 3L, null, new CriarOsRequest(null, null, null, "  ", null)), "relato");

        verifyNoInteractions(clienteService, veiculoService, ordemServicoService);
    }

    @Test
    void erroNoVeiculoPropagaParaATransacaoDesfazerOCliente() {
        when(clienteService.criar(CLIENTE)).thenReturn(Cliente.builder().id(9L).build());
        when(veiculoService.criar(anyLong(), any())).thenThrow(new RegraDeNegocioException("Placa já cadastrada: TST1A23"));

        assertThatThrownBy(() -> service.registrar(new AtendimentoRequest(null, CLIENTE, null, VEICULO, OS)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Placa já cadastrada");
        verifyNoInteractions(ordemServicoService);
    }

    private void assertRecusa(AtendimentoRequest pedido, String trechoDaMensagem) {
        assertThatThrownBy(() -> service.registrar(pedido))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining(trechoDaMensagem);
    }
}
