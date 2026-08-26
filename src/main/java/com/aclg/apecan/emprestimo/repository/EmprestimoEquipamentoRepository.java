package com.aclg.apecan.emprestimo.repository;

import com.aclg.apecan.emprestimo.entity.EmprestimoEquipamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface EmprestimoEquipamentoRepository extends JpaRepository<EmprestimoEquipamento, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from EmprestimoEquipamento e where e.id=:id")
	Optional<EmprestimoEquipamento> findByIdForUpdate(@Param("id") Long id);

	boolean existsByEquipamentoIdAndDataDevolucaoIsNull(Long equipamentoId);

	Page<EmprestimoEquipamento> findAllByDataDevolucaoIsNull(Pageable p);

	long countByDataDevolucaoIsNull();

	long countByDataDevolucaoIsNotNull();

	@Query("select count(e) from EmprestimoEquipamento e where e.dataDevolucao is null and e.dataPrevistaDevolucao<:hoje")
	long countAtrasados(@Param("hoje") java.time.LocalDate hoje);

}
