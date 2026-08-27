package com.mecanicadbo.repository;

import com.mecanicadbo.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCpfAndAtivoTrue(String cpf);

    @Query("""
        SELECT c FROM Cliente c
        WHERE c.ativo = true AND (
            LOWER(c.nome)    LIKE LOWER(CONCAT('%', :q, '%')) OR
            c.cpf            LIKE CONCAT('%', :q, '%')        OR
            c.telefone1      LIKE CONCAT('%', :q, '%')
        )
        ORDER BY c.nome
    """)
    List<Cliente> buscarPorTermo(@Param("q") String termo);
}
