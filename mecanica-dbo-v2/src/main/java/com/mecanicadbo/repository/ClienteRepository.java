package com.mecanicadbo.repository;

import com.mecanicadbo.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Busca pelo hash do CPF (CpfCripto.hash) SEM filtrar por ativo, para
     * checar duplicidade: o índice único vale para a tabela inteira, inclusive
     * inativos. Assim o ClienteService responde "CPF já cadastrado para <nome>"
     * em vez de o INSERT estourar no banco.
     */
    Optional<Cliente> findByCpfHash(String cpfHash);

    /**
     * Listagem padrão da tela de clientes: só ativos, igual à busca com termo
     * (senão o cliente apareceria na lista cheia e sumiria ao digitar o nome).
     */
    List<Cliente> findByAtivoTrueOrderByNome();

    /** Contador do painel. */
    long countByAtivoTrue();

    /** "Clientes novos" do painel: cadastrados a partir de uma data, mais recentes primeiro. */
    List<Cliente> findByAtivoTrueAndCriadoEmGreaterThanEqualOrderByCriadoEmDesc(LocalDateTime desde);

    /**
     * Nome e telefone por pedaço; CPF só pelo número completo, comparando o
     * hash (o CPF está cifrado no banco). cpfHash "" = o termo não é um CPF.
     */
    @Query("""
        SELECT c FROM Cliente c
        WHERE c.ativo = true
          AND (LOWER(c.nome)  LIKE LOWER(CONCAT('%', :q, '%'))
            OR c.cpfHash      = :cpfHash
            OR c.telefone1    LIKE CONCAT('%', :q, '%')
            OR c.telefone2    LIKE CONCAT('%', :q, '%'))
        ORDER BY c.nome
    """)
    List<Cliente> buscarPorTermo(@Param("q") String termo, @Param("cpfHash") String cpfHash);
}
