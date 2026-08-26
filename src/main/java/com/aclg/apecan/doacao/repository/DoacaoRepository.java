package com.aclg.apecan.doacao.repository;

import com.aclg.apecan.doacao.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

public interface DoacaoRepository extends JpaRepository<Doacao, Long> {

	@Query("select d from Doacao d where (:tipo is null or d.tipo=:tipo) and (:inicio is null or d.dataDoacao>=:inicio) and (:fim is null or d.dataDoacao<=:fim)")
	Page<Doacao> pesquisar(@Param("tipo") TipoDoacao tipo, @Param("inicio") LocalDate inicio,
			@Param("fim") LocalDate fim, Pageable p);

	@Query("select count(d) from Doacao d where d.tipo=:tipo and (:inicio is null or d.dataDoacao>=:inicio) and (:fim is null or d.dataDoacao<=:fim)")
	long contar(@Param("tipo") TipoDoacao tipo, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

}
