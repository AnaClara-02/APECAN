package com.aclg.apecan.equipamento.repository;

import com.aclg.apecan.equipamento.dto.ResumoCategoriaEquipamento;
import com.aclg.apecan.equipamento.dto.QuantidadeEquipamentosPorDoacao;
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

	@Query("select e.doacao.id as doacaoId, count(e) as quantidade from Equipamento e where e.doacao.id in :ids group by e.doacao.id")
	List<QuantidadeEquipamentosPorDoacao> contarPorDoacoes(@Param("ids") List<Long> ids);

	long countByCategoriaId(Long categoriaId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Equipamento e where e.id=:id")
	Optional<Equipamento> findByIdForUpdate(@Param("id") Long id);

	@Query(value = """
		select e from Equipamento e join fetch e.categoria
		 where (:categoriaId is null or e.categoria.id = :categoriaId)
		   and (:status is null or e.status = :status)
		""", countQuery = """
		select count(e) from Equipamento e
		 where (:categoriaId is null or e.categoria.id = :categoriaId)
		   and (:status is null or e.status = :status)
		""")
	Page<Equipamento> pesquisar(@Param("categoriaId") Long categoriaId, @Param("status") StatusEquipamento status,
			Pageable p);

	@Query("""
		select c.id as idCategoria, c.nome as categoria,
		       count(e.id) as quantidadeTotal,
		       sum(case when e.status = com.aclg.apecan.equipamento.entity.StatusEquipamento.ATIVO then 1L else 0L end) as quantidadeAtiva,
		       sum(case when e.status = com.aclg.apecan.equipamento.entity.StatusEquipamento.INATIVO then 1L else 0L end) as quantidadeInativa,
		       sum(case when e.status = com.aclg.apecan.equipamento.entity.StatusEquipamento.EMPRESTADO then 1L else 0L end) as quantidadeEmprestada
		  from CategoriaEquipamento c left join Equipamento e on e.categoria = c
		 group by c.id, c.nome order by c.nome
		""")
	List<ResumoCategoriaEquipamento> resumirPorCategoria();

}
