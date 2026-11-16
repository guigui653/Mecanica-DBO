package com.mecanicadbo.model;

import com.mecanicadbo.service.CpfCripto;
import jakarta.persistence.PostLoad;
import org.springframework.stereotype.Component;

/**
 * Ao carregar um Cliente do banco, decifra o CPF para o campo em memória.
 * Assim todo código que lê cliente.getCpf() (DTOs, recibo, telas) continua
 * funcionando sem saber da criptografia. A gravação cifrada é feita no
 * ClienteService.
 *
 * É um bean do Spring (o Spring Boot entrega os listeners JPA ao Hibernate
 * já com as dependências injetadas).
 */
@Component
public class ClienteCpfListener {

    private final CpfCripto cripto;

    public ClienteCpfListener(CpfCripto cripto) {
        this.cripto = cripto;
    }

    @PostLoad
    public void aoCarregar(Cliente cliente) {
        cliente.setCpf(cripto.decifrar(cliente.getCpfCifrado()));
    }
}
