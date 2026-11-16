package com.mecanicadbo.service;

import com.mecanicadbo.dto.ClienteRequest;
import com.mecanicadbo.exception.RegraDeNegocioException;
import com.mecanicadbo.model.Cliente;
import com.mecanicadbo.repository.ClienteRepository;
import com.mecanicadbo.repository.OrdemServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Apagar de verdade só cliente sem histórico; quem tem OS é inativado.
 * CPF gravado só cifrado, com o hash conferindo CPF repetido.
 */
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock ClienteRepository repo;
    @Mock OrdemServicoRepository osRepo;
    @Spy CpfCripto cripto = new CpfCripto(CpfCriptoTest.CHAVE_TESTE);
    @InjectMocks ClienteService service;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder().id(8L).nome("Diogo").ativo(true).build();
        lenient().when(repo.findById(8L)).thenReturn(Optional.of(cliente));
        lenient().when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void cpfEGravadoSoCifradoEComOHash() {
        when(repo.findByCpfHash(any())).thenReturn(Optional.empty());

        Cliente novo = service.criar(new ClienteRequest("Bete", "123.456.789-00", "61900000000", null, null));

        assertThat(novo.getCpf()).isEqualTo("12345678900");                       // só em memória
        assertThat(novo.getCpfCifrado()).startsWith("v1:").doesNotContain("123456");
        assertThat(cripto.decifrar(novo.getCpfCifrado())).isEqualTo("12345678900");
        assertThat(novo.getCpfHash()).isEqualTo(cripto.hash("12345678900"));
    }

    @Test
    void cpfRepetidoEmOutroFormatoEBarradoPeloHash() {
        when(repo.findByCpfHash(cripto.hash("12345678900"))).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> service.criar(new ClienteRequest("Outro", "12345678900", "6100", null, null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CPF já cadastrado para: Diogo");
        verify(repo, never()).save(any());
    }

    @Test
    void cpfIncompletoEsemCpfSaoTratados() {
        assertThatThrownBy(() -> service.criar(new ClienteRequest("X", "123.456", "6100", null, null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("11 números");

        Cliente semCpf = service.criar(new ClienteRequest("Sem CPF", "  ", "6100", null, null));
        assertThat(semCpf.getCpfCifrado()).isNull();
        assertThat(semCpf.getCpfHash()).isNull();
    }

    @Test
    void buscaPorCpfSoComONumeroCompleto() {
        service.buscar("123.456.789-00");
        verify(repo).buscarPorTermo("123.456.789-00", cripto.hash("12345678900"));

        service.buscar("456");
        verify(repo).buscarPorTermo("456", "");
    }

    @Test
    void clienteComOsNaoEApagado() {
        when(osRepo.countByVeiculoClienteId(8L)).thenReturn(2L);

        assertThatThrownBy(() -> service.excluir(8L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("2 ordens de serviço")
                .hasMessageContaining("Inativar");
        verify(repo, never()).delete(any());
    }

    @Test
    void clienteSemOsEApagado() {
        when(osRepo.countByVeiculoClienteId(8L)).thenReturn(0L);

        service.excluir(8L);

        verify(repo).delete(cliente);
    }

    @Test
    void inativarMantemOCadastro() {
        service.inativar(8L);

        assertThat(cliente.getAtivo()).isFalse();
        verify(repo).save(cliente);
        verify(repo, never()).delete(any());
    }
}
