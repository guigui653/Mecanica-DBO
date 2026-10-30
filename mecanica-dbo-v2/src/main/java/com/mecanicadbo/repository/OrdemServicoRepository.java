package com.mecanicadbo.repository;

import com.mecanicadbo.model.OrdemServico;
import com.mecanicadbo.model.StatusOs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Consultas de OS.
 *
 * O QUE MUDOU AQUI E POR QUÊ
 * --------------------------
 * A versão anterior usava JOIN simples:
 *
 *     SELECT os FROM OrdemServico os JOIN os.veiculo v JOIN v.cliente c
 *
 * JOIN sozinho serve só para filtrar. Ele NÃO traz o veículo nem o cliente
 * junto no resultado — as duas associações continuam lazy. Então, ao montar
 * a lista da tela, cada OS disparava dois SELECTs extras (um do veículo, um
 * do cliente). Uma lista de 50 OS = 1 + 100 consultas. É o clássico N+1, e
 * era ele o motivo da busca de veículo/OS demorar.
 *
 * Trocando por JOIN FETCH, o Hibernate traz OS + veículo + cliente na mesma
 * consulta. A lista inteira passa a ser 1 SELECT.
 *
 * Por isso as duas consultas derivadas (findByVeiculoId... e findByStatus...)
 * viraram @Query: derivação por nome de método não tem como declarar fetch.
 */
public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    /**
     * Uma OS com veículo e cliente já carregados.
     * Usado pela tela de detalhe e pela geração de PDF, que precisam do
     * nome do cliente fora da transação.
     */
    @Query("""
        SELECT os FROM OrdemServico os
        JOIN FETCH os.veiculo v
        JOIN FETCH v.cliente c
        WHERE os.id = :id
    """)
    Optional<OrdemServico> buscarCompletaPorId(@Param("id") Long id);

    /**
     * Listagem sem filtro, para GET /api/ordens sem parâmetro q.
     *
     * Antes o service chamava findAll() nesse caso, que não faz fetch
     * nenhum — a listagem mais usada do sistema era justamente a mais lenta.
     */
    @Query("""
        SELECT os FROM OrdemServico os
        JOIN FETCH os.veiculo v
        JOIN FETCH v.cliente c
        ORDER BY os.dataEntrada DESC, os.id DESC
    """)
    List<OrdemServico> buscarTodas();

    @Query("""
        SELECT os FROM OrdemServico os
        JOIN FETCH os.veiculo v
        JOIN FETCH v.cliente c
        WHERE v.id = :veiculoId
        ORDER BY os.dataEntrada DESC, os.id DESC
    """)
    List<OrdemServico> findByVeiculoIdOrderByDataEntradaDesc(@Param("veiculoId") Long veiculoId);

    @Query("""
        SELECT os FROM OrdemServico os
        JOIN FETCH os.veiculo v
        JOIN FETCH v.cliente c
        WHERE os.status = :status
        ORDER BY os.dataEntrada DESC, os.id DESC
    """)
    List<OrdemServico> findByStatusOrderByDataEntradaDesc(@Param("status") StatusOs status);

    /**
     * Período com limites opcionais: se o usuário mandar só a data inicial,
     * a final deixa de filtrar, e vice-versa.
     *
     * Antes, BETWEEN :inicio AND :fim com qualquer um dos dois null fazia a
     * consulta devolver lista vazia sem erro nenhum — parecia que não havia
     * OS no período.
     */
    @Query("""
        SELECT os FROM OrdemServico os
        JOIN FETCH os.veiculo v
        JOIN FETCH v.cliente c
        WHERE (:inicio IS NULL OR os.dataEntrada >= :inicio)
          AND (:fim    IS NULL OR os.dataEntrada <= :fim)
        ORDER BY os.dataEntrada DESC, os.id DESC
    """)
    List<OrdemServico> findByPeriodo(@Param("inicio") LocalDate inicio,
                                     @Param("fim") LocalDate fim);

    /**
     * Busca por nome do cliente, placa ou número da OS.
     *
     * O trecho do id foi acrescentado porque o mecânico procura pelo número
     * impresso no recibo ("a 12"), e antes isso não achava nada.
     *
     * @param placa o termo já normalizado por Placas.paraBusca ("jeh-30" → "JEH30"),
     *              porque a placa fica gravada sem hífen.
     */
    @Query("""
        SELECT os FROM OrdemServico os
        JOIN FETCH os.veiculo v
        JOIN FETCH v.cliente c
        WHERE LOWER(c.nome)  LIKE LOWER(CONCAT('%', :q, '%'))
           OR v.placa        LIKE CONCAT('%', :placa, '%')
           OR CAST(os.id AS string) = :q
        ORDER BY os.dataEntrada DESC, os.id DESC
    """)
    List<OrdemServico> buscarPorClienteOuPlaca(@Param("q") String termo, @Param("placa") String placa);

    /** Contadores do dashboard, sem carregar as OS. */
    long countByStatus(StatusOs status);
}
