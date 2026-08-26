package com.aclg.apecan.paciente.repository;

import com.aclg.apecan.paciente.entity.Paciente;
import com.aclg.apecan.paciente.entity.StatusPaciente;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

	List<Paciente> findAllByStatusOrderByNomeAsc(StatusPaciente status);

	boolean existsByCpf(String cpf);

	long countByStatus(StatusPaciente status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Paciente p where p.id = :id")
	Optional<Paciente> findByIdForUpdate(@Param("id") Long id);

	@Query("""
			select p from Paciente p
			 where (:nome = '' or lower(p.nome) like lower(concat('%', :nome, '%')))
			   and (:cpf = '' or p.cpf like concat('%', :cpf, '%'))
			   and (:status is null or p.status = :status)
			""")
	Page<Paciente> pesquisar(@Param("nome") String nome, @Param("cpf") String cpf,
			@Param("status") StatusPaciente status, Pageable pageable);

}
