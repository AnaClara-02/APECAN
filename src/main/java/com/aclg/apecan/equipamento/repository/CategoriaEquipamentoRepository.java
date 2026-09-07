package com.aclg.apecan.equipamento.repository;

import com.aclg.apecan.equipamento.entity.CategoriaEquipamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface CategoriaEquipamentoRepository extends JpaRepository<CategoriaEquipamento, Long> {

	boolean existsByNomeIgnoreCase(String nome);

	List<CategoriaEquipamento> findAllByOrderByNomeAsc();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from CategoriaEquipamento c where c.id=:id")
	Optional<CategoriaEquipamento> findByIdForUpdate(@Param("id") Long id);

}
