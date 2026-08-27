package com.mecanicadbo.repository;

import com.mecanicadbo.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    Optional<Veiculo> findByPlacaIgnoreCase(String placa);

    List<Veiculo> findByClienteIdAndAtivoTrue(Long clienteId);
}
