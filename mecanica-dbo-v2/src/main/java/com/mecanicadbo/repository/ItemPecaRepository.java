package com.mecanicadbo.repository;

import com.mecanicadbo.model.ItemPeca;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemPecaRepository extends JpaRepository<ItemPeca, Long> {
    List<ItemPeca> findByOrdemServicoId(Long osId);
}
