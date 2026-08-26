package com.aclg.apecan.equipamento.repository;

import com.aclg.apecan.equipamento.dto.ResumoCategoriaEquipamento;
import com.aclg.apecan.equipamento.entity.Equipamento;
import com.aclg.apecan.equipamento.entity.StatusEquipamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {

	@EntityGraph(attributePaths = "categoria")
	List<Equipamento> findAllByStatusOrderByCategoriaNomeAscIdAsc(StatusEquipamento status);

	long countByDoacaoId(Long doacaoId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Equipamento e where e.id=:id")
	Optional<Equipamento> findByIdForUpdate(@Param("id") Long id);

	@Query("""
		select e from Equipamento e
		 where (:categoriaId is null or e.categoria.id = :categoriaId)
		   and (:status is null or e.status = :status)
		""")
	Page<Equipamento> pesquisar(@Param("categoriaId") Long categoriaId, @Param("status") StatusEquipamento status,
			Pageable p);

	@Query(value = """
		SELECT id_categoria AS idCategoria,
		       categoria,
		       quantidade_total AS quantidadeTotal,
		       quantidade_ativa AS quantidadeAtiva,
		       quantidade_inativa AS quantidadeInativa,
		       quantidade_emprestada AS quantidadeEmprestada
		  FROM vw_resumo_equipamentos_por_categoria
		 ORDER BY categoria
		""", nativeQuery = true)
	List<ResumoCategoriaEquipamento> resumirPorCategoria();

}
