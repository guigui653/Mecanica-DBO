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
 * Todas usam JOIN FETCH do veículo e do cliente: a lista da tela mostra os
 * dois, e com JOIN simples (que só filtra) cada OS dispararia mais dois
 * SELECTs — o problema N+1. Com o fetch, a lista inteira é um SELECT só.
 *
 * Por isso até as consultas que poderiam ser derivadas do nome do método
 * (findByVeiculoId..., findByStatus...) são @Query: derivação não declara fetch.
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
     * (Não use findAll() aqui: ele não faz fetch e cai no N+1.)
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
     * a final deixa de filtrar, e vice-versa. (Um BETWEEN com um dos lados
     * null devolveria lista vazia sem erro.)
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

    /** Quantas OS (de qualquer status) existem nos veículos deste cliente. */
    long countByVeiculoClienteId(Long clienteId);
}
