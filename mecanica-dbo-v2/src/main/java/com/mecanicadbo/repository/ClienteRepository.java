package com.mecanicadbo.repository;

import com.mecanicadbo.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Busca por CPF SEM filtrar por ativo, para checar duplicidade: o índice
     * único da coluna cpf vale para a tabela inteira, inclusive inativos.
     * Assim o ClienteService responde "CPF já cadastrado para <nome> (cliente
     * inativo)" em vez de o INSERT estourar no banco.
     */
    Optional<Cliente> findByCpf(String cpf);

    /**
     * Listagem padrão da tela de clientes: só ativos, igual à busca com termo
     * (senão o cliente apareceria na lista cheia e sumiria ao digitar o nome).
     */
    List<Cliente> findByAtivoTrueOrderByNome();

    @Query("""
        SELECT c FROM Cliente c
        WHERE c.ativo = true
          AND (LOWER(c.nome)  LIKE LOWER(CONCAT('%', :q, '%'))
            OR c.cpf          LIKE CONCAT('%', :q, '%')
            OR c.telefone1    LIKE CONCAT('%', :q, '%')
            OR c.telefone2    LIKE CONCAT('%', :q, '%'))
        ORDER BY c.nome
    """)
    List<Cliente> buscarPorTermo(@Param("q") String termo);
}
