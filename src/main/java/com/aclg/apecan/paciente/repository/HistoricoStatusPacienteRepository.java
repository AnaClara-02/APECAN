package com.aclg.apecan.paciente.repository;

import com.aclg.apecan.paciente.entity.HistoricoStatusPaciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoStatusPacienteRepository extends JpaRepository<HistoricoStatusPaciente, Long> {

	List<HistoricoStatusPaciente> findAllByPacienteIdOrderByAlteradoEmDesc(Long pacienteId);

}
