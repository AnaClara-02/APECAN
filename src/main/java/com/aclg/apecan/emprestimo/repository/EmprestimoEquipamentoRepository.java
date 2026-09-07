package com.aclg.apecan.emprestimo.repository;

import com.aclg.apecan.emprestimo.entity.EmprestimoEquipamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;
import java.time.LocalDate;

public interface EmprestimoEquipamentoRepository extends JpaRepository<EmprestimoEquipamento, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from EmprestimoEquipamento e where e.id=:id")
	Optional<EmprestimoEquipamento> findByIdForUpdate(@Param("id") Long id);

	boolean existsByEquipamentoIdAndDataDevolucaoIsNull(Long equipamentoId);

	@EntityGraph(attributePaths = { "equipamento", "equipamento.categoria", "paciente" })
	Page<EmprestimoEquipamento> findAllByDataDevolucaoIsNull(Pageable p);

	@EntityGraph(attributePaths = { "equipamento", "equipamento.categoria", "paciente" })
	Page<EmprestimoEquipamento> findAllByDataDevolucaoIsNotNull(Pageable p);

	@EntityGraph(attributePaths = { "equipamento", "equipamento.categoria", "paciente" })
	@Query(value = "select e from EmprestimoEquipamento e", countQuery = "select count(e) from EmprestimoEquipamento e")
	Page<EmprestimoEquipamento> listarTodos(Pageable p);

	@EntityGraph(attributePaths = "paciente")
	Page<EmprestimoEquipamento> findAllByEquipamentoId(Long equipamentoId, Pageable pageable);

	@EntityGraph(attributePaths = { "equipamento", "equipamento.categoria", "paciente" })
	@Query("""
		select e from EmprestimoEquipamento e
		 where (:inicio is null or e.dataEmprestimo >= :inicio)
		   and (:fim is null or e.dataEmprestimo <= :fim)
		""")
	Page<EmprestimoEquipamento> pesquisarRelatorio(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim,
			Pageable pageable);

	long countByDataDevolucaoIsNull();

	long countByDataDevolucaoIsNotNull();

	@Query("select count(e) from EmprestimoEquipamento e where e.dataDevolucao is null and e.dataPrevistaDevolucao<:hoje")
	long countAtrasados(@Param("hoje") java.time.LocalDate hoje);

}
