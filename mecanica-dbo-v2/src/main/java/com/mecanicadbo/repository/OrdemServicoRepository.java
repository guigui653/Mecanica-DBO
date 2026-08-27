package com.mecanicadbo.repository;

import com.mecanicadbo.model.OrdemServico;
import com.mecanicadbo.model.StatusOs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    List<OrdemServico> findByVeiculoIdOrderByDataEntradaDesc(Long veiculoId);

    List<OrdemServico> findByStatusOrderByDataEntradaDesc(StatusOs status);

    @Query("""
        SELECT os FROM OrdemServico os
        JOIN os.veiculo v
        JOIN v.cliente c
        WHERE os.dataEntrada BETWEEN :inicio AND :fim
        ORDER BY os.dataEntrada DESC
    """)
    List<OrdemServico> findByPeriodo(
        @Param("inicio") LocalDate inicio,
        @Param("fim")    LocalDate fim
    );

    @Query("""
        SELECT os FROM OrdemServico os
        JOIN os.veiculo v
        JOIN v.cliente c
        WHERE LOWER(c.nome)  LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(v.placa) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY os.dataEntrada DESC
    """)
    List<OrdemServico> buscarPorClienteOuPlaca(@Param("q") String termo);
}
