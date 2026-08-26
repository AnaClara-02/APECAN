package com.aclg.apecan.despesa.repository;

import com.aclg.apecan.despesa.entity.Despesa;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {

	Page<Despesa> findAllByOrderByMovimentacaoFinanceiraDataMovimentacaoDesc(Pageable p);

}
