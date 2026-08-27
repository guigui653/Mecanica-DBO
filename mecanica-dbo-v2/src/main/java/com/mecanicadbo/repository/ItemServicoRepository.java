package com.mecanicadbo.repository;

import com.mecanicadbo.model.ItemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemServicoRepository extends JpaRepository<ItemServico, Long> {
    List<ItemServico> findByOrdemServicoId(Long osId);
}
