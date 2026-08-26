package com.aclg.apecan.voluntario.repository;

import com.aclg.apecan.voluntario.entity.StatusVoluntario;
import com.aclg.apecan.voluntario.entity.Voluntario;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VoluntarioRepository extends JpaRepository<Voluntario, Long> {

	boolean existsByCpf(String cpf);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select v from Voluntario v where v.id=:id")
	Optional<Voluntario> findByIdForUpdate(@Param("id") Long id);

	@Query("""
			select v from Voluntario v
			 where (:nome='' or lower(v.nome) like lower(concat('%',:nome,'%')))
			   and (:status is null or v.status=:status)
			""")
	Page<Voluntario> pesquisar(@Param("nome") String nome, @Param("status") StatusVoluntario status, Pageable pageable);

}
