package com.aclg.apecan.despesa.repository;

import com.aclg.apecan.despesa.entity.Despesa;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {

	@Query(value = """
			select d from Despesa d join fetch d.movimentacaoFinanceira m
			where (:inicio is null or m.dataMovimentacao >= :inicio)
			  and (:fim is null or m.dataMovimentacao <= :fim)
			order by m.dataMovimentacao desc, d.id desc
			""",
		countQuery = """
			select count(d) from Despesa d join d.movimentacaoFinanceira m
			where (:inicio is null or m.dataMovimentacao >= :inicio)
			  and (:fim is null or m.dataMovimentacao <= :fim)
			""")
	Page<Despesa> pesquisar(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim, Pageable pageable);

	@Query("""
			select coalesce(sum(m.valor), 0) from Despesa d
			join d.movimentacaoFinanceira m
			where (:inicio is null or m.dataMovimentacao >= :inicio)
			  and (:fim is null or m.dataMovimentacao <= :fim)
			""")
	BigDecimal somarNoPeriodo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

	@Query("""
			select count(d) from Despesa d
			join d.movimentacaoFinanceira m
			where (:inicio is null or m.dataMovimentacao >= :inicio)
			  and (:fim is null or m.dataMovimentacao <= :fim)
			""")
	long contarNoPeriodo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

	@Query("select d from Despesa d join fetch d.movimentacaoFinanceira where d.id = :id")
	Optional<Despesa> buscarDetalhe(@Param("id") Long id);
}
