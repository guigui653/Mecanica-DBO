package com.mecanicadbo.repository;

import com.mecanicadbo.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Consultas de veículo.
 *
 * Todas as consultas de leitura usam JOIN FETCH v.cliente porque o DTO
 * (VeiculoResumoDTO) sempre mostra o nome do dono. Sem o fetch, cada veículo
 * da lista disparava um SELECT extra só para buscar o cliente.
 */
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    @Query("SELECT v FROM Veiculo v JOIN FETCH v.cliente c WHERE v.id = :id")
    Optional<Veiculo> buscarComCliente(@Param("id") Long id);

    @Query("""
        SELECT v FROM Veiculo v
        JOIN FETCH v.cliente c
        WHERE UPPER(v.placa) = UPPER(:placa)
    """)
    Optional<Veiculo> findByPlacaIgnoreCase(@Param("placa") String placa);

    @Query("""
        SELECT v FROM Veiculo v
        JOIN FETCH v.cliente c
        WHERE c.id = :clienteId AND v.ativo = true
        ORDER BY v.placa
    """)
    List<Veiculo> findByClienteIdAndAtivoTrue(@Param("clienteId") Long clienteId);

    /**
     * Busca da tela de consulta de veículo.
     *
     * Substitui findByPlacaContainingIgnoreCaseOrModeloContainingIgnoreCase,
     * que existia no repositório mas nunca era chamado porque nenhum endpoint
     * expunha a listagem. Era por isso que a tela de busca do front trabalhava
     * com dados mockados e o fluxo de nova OS precisava tentar variações da
     * placa uma a uma.
     *
     * Agora procura por placa, modelo, marca e nome do dono — o mecânico
     * costuma lembrar "o Gol do seu Antônio" antes de lembrar a placa.
     *
     * @param placa o termo já normalizado por Placas.paraBusca ("jeh-30" → "JEH30"),
     *              porque a placa fica gravada sem hífen.
     */
    @Query("""
        SELECT v FROM Veiculo v
        JOIN FETCH v.cliente c
        WHERE v.ativo = true
          AND (v.placa         LIKE CONCAT('%', :placa, '%')
            OR LOWER(v.modelo) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(v.marca)  LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(c.nome)   LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY v.placa
    """)
    List<Veiculo> buscarPorTermo(@Param("q") String termo, @Param("placa") String placa);

    @Query("""
        SELECT v FROM Veiculo v
        JOIN FETCH v.cliente c
        WHERE v.ativo = true
        ORDER BY v.placa
    """)
    List<Veiculo> listarAtivos();
}
