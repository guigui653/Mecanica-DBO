package com.mecanicadbo.repository;

import com.mecanicadbo.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Busca por CPF SEM filtrar por ativo.
     *
     * O findByCpfAndAtivoTrue continua existindo logo abaixo, mas ele não
     * servia para checar duplicidade: o índice único da coluna cpf vale para
     * a tabela inteira, inclusive para clientes inativos. Então, se o CPF
     * pertencesse a um cliente desativado, a checagem passava limpa e o
     * INSERT estourava no banco — 500 sem explicação nenhuma na tela.
     *
     * Com este método o ClienteService detecta o caso e responde
     * "CPF já cadastrado para <nome> (cliente inativo)".
     */
    Optional<Cliente> findByCpf(String cpf);

    Optional<Cliente> findByCpfAndAtivoTrue(String cpf);

    /**
     * Listagem padrão da tela de clientes.
     *
     * Antes o service usava findAll() aqui, que traz também os inativos —
     * enquanto a busca com termo filtrava ativo = true. Resultado: o cliente
     * aparecia na lista cheia e sumia ao digitar o nome dele.
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
