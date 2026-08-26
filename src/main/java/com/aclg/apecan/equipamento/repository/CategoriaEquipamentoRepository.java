package com.aclg.apecan.equipamento.repository;

import com.aclg.apecan.equipamento.entity.CategoriaEquipamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoriaEquipamentoRepository extends JpaRepository<CategoriaEquipamento, Long> {

	boolean existsByNomeIgnoreCase(String nome);

	List<CategoriaEquipamento> findAllByOrderByNomeAsc();

}
