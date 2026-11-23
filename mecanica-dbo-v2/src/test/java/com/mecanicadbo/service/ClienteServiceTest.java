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

        Cliente novo = service.criar(new ClienteRequest("Bete", "529.982.247-25", "61900000000", null, null));

        assertThat(novo.getCpf()).isEqualTo("52998224725");                       // só em memória
        assertThat(novo.getCpfCifrado()).startsWith("v1:").doesNotContain("529982");
        assertThat(cripto.decifrar(novo.getCpfCifrado())).isEqualTo("52998224725");
        assertThat(novo.getCpfHash()).isEqualTo(cripto.hash("52998224725"));
    }

    @Test
    void cpfRepetidoEmOutroFormatoEBarradoPeloHash() {
        when(repo.findByCpfHash(cripto.hash("52998224725"))).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> service.criar(new ClienteRequest("Outro", "52998224725", "6100", null, null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CPF já cadastrado para: Diogo");
        verify(repo, never()).save(any());
    }

    @Test
    void cpfIncompletoEsemCpfSaoTratados() {
        assertThatThrownBy(() -> service.criar(new ClienteRequest("X", "123.456", "6100", null, null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Documento inválido");

        Cliente semCpf = service.criar(new ClienteRequest("Sem CPF", "  ", "6100", null, null));
        assertThat(semCpf.getCpfCifrado()).isNull();
        assertThat(semCpf.getCpfHash()).isNull();
    }

    @Test
    void cnpjDeEmpresaInclusiveAlfanumerico() {
        when(repo.findByCpfHash(any())).thenReturn(Optional.empty());

        Cliente empresa = service.criar(new ClienteRequest("Transportes Ltda", "11.222.333/0001-81", "6100", null, null));
        assertThat(empresa.getCpf()).isEqualTo("11222333000181");
        assertThat(cripto.decifrar(empresa.getCpfCifrado())).isEqualTo("11222333000181");

        Cliente nova = service.criar(new ClienteRequest("Empresa Nova", "12.abc.345/01de-35", "6100", null, null));
        assertThat(nova.getCpf()).isEqualTo("12ABC34501DE35");
    }

    @Test
    void digitosVerificadoresErradosSaoRecusados() {
        assertThatThrownBy(() -> service.criar(new ClienteRequest("X", "529.982.247-24", "6100", null, null)))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("CPF inválido");
        assertThatThrownBy(() -> service.criar(new ClienteRequest("X", "11.222.333/0001-82", "6100", null, null)))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("CNPJ inválido");
        verify(repo, never()).save(any());
    }

    @Test
    void cnpjRepetidoAvisaComONomeCerto() {
        when(repo.findByCpfHash(cripto.hash("11222333000181"))).thenReturn(Optional.of(cliente));
        assertThatThrownBy(() -> service.criar(new ClienteRequest("Outra", "11222333000181", "6100", null, null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CNPJ já cadastrado para: Diogo");
    }

    @Test
    void edicaoSemMexerNoDocumentoAntigoNaoReconfere() {
        // Cadastro antigo, de antes da conferência dos dígitos.
        cliente.setCpf("12345678900");
        when(repo.findByCpfHash(any())).thenReturn(Optional.of(cliente));

        Cliente salvo = service.atualizar(8L, new ClienteRequest("Diogo Silva", "123.456.789-00", "6100", null, null));
        assertThat(salvo.getNome()).isEqualTo("Diogo Silva");
        assertThat(salvo.getCpf()).isEqualTo("12345678900");
    }

    @Test
    void buscaPorCpfSoComONumeroCompleto() {
        service.buscar("529.982.247-25");
        verify(repo).buscarPorTermo("529.982.247-25", cripto.hash("52998224725"));

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
