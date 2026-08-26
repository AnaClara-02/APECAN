package com.aclg.apecan.financeiro.repository;

import com.aclg.apecan.financeiro.entity.MovimentacaoFinanceira;
import com.aclg.apecan.financeiro.entity.TipoMovimentacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface MovimentacaoFinanceiraRepository extends JpaRepository<MovimentacaoFinanceira, Long> {

	@Query("""
		select m from MovimentacaoFinanceira m
		 where (:tipo is null or m.tipo = :tipo)
		   and (:inicio is null or m.dataMovimentacao >= :inicio)
		   and (:fim is null or m.dataMovimentacao <= :fim)
		""")
	Page<MovimentacaoFinanceira> pesquisar(@Param("tipo") TipoMovimentacao tipo, @Param("inicio") LocalDate inicio,
			@Param("fim") LocalDate fim, Pageable p);

	Optional<MovimentacaoFinanceira> findByDoacaoId(Long id);

	@Query(value = """
		SELECT COALESCE(SUM(
		         CASE WHEN tipo_movimentacao = 'ENTRADA' THEN valor ELSE -valor END
		       ), 0)
		  FROM movimentacoes_financeiras
		 WHERE (:inicio IS NULL OR data_movimentacao >= :inicio)
		   AND (:fim IS NULL OR data_movimentacao <= :fim)
		""", nativeQuery = true)
	BigDecimal saldo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

	@Query("""
		select coalesce(sum(m.valor), 0) from MovimentacaoFinanceira m
		 where m.tipo = :tipo
		   and (:inicio is null or m.dataMovimentacao >= :inicio)
		   and (:fim is null or m.dataMovimentacao <= :fim)
		""")
	BigDecimal total(@Param("tipo") TipoMovimentacao tipo, @Param("inicio") LocalDate inicio,
			@Param("fim") LocalDate fim);

}
