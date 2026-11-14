package com.mecanicadbo.service;

import com.mecanicadbo.exception.RegraDeNegocioException;
import com.mecanicadbo.model.Cliente;
import com.mecanicadbo.repository.ClienteRepository;
import com.mecanicadbo.repository.OrdemServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Apagar de verdade só cliente sem histórico; quem tem OS é inativado. */
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock ClienteRepository repo;
    @Mock OrdemServicoRepository osRepo;
    @InjectMocks ClienteService service;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder().id(8L).nome("Diogo").ativo(true).build();
        when(repo.findById(8L)).thenReturn(Optional.of(cliente));
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
