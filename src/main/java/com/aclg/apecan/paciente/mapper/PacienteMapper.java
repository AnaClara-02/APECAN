package com.aclg.apecan.paciente.mapper;

import com.aclg.apecan.paciente.dto.EditarPacienteForm;
import com.aclg.apecan.paciente.dto.HistoricoStatusPacienteDto;
import com.aclg.apecan.paciente.dto.PacienteDetalheDto;
import com.aclg.apecan.paciente.dto.PacienteResumoDto;
import com.aclg.apecan.paciente.entity.HistoricoStatusPaciente;
import com.aclg.apecan.paciente.entity.Paciente;
import com.aclg.apecan.shared.validation.CpfFormatter;
import com.aclg.apecan.shared.validation.TelefoneFormatter;
import org.springframework.stereotype.Component;

@Component
public class PacienteMapper {

	public PacienteResumoDto paraResumo(Paciente paciente) {
		return new PacienteResumoDto(paciente.getId(), paciente.getNome(), CpfFormatter.mascarar(paciente.getCpf()),
				paciente.getDataNascimento(), TelefoneFormatter.formatar(paciente.getTelefone()), paciente.getLocalTratamento(),
				paciente.getStatus());
	}

	public PacienteDetalheDto paraDetalhe(Paciente paciente) {
		return new PacienteDetalheDto(paciente.getId(), paciente.getNome(), CpfFormatter.formatar(paciente.getCpf()),
				paciente.getDataNascimento(), TelefoneFormatter.formatar(paciente.getTelefone()), paciente.getEndereco(),
				paciente.getLocalTratamento(), paciente.getStatus(), paciente.getCriadoPor().getNome(),
				paciente.getAtualizadoPor() == null ? null : paciente.getAtualizadoPor().getNome(),
				paciente.getCriadoEm(), paciente.getAtualizadoEm());
	}

	public EditarPacienteForm paraFormulario(Paciente paciente) {
		EditarPacienteForm form = new EditarPacienteForm();
		form.setNome(paciente.getNome());
		form.setDataNascimento(paciente.getDataNascimento());
		form.setTelefone(TelefoneFormatter.formatar(paciente.getTelefone()));
		form.setEndereco(paciente.getEndereco());
		form.setLocalTratamento(paciente.getLocalTratamento());
		return form;
	}

	public HistoricoStatusPacienteDto paraHistorico(HistoricoStatusPaciente historico) {
		return new HistoricoStatusPacienteDto(historico.getStatus(), historico.getAlteradoEm(),
				historico.getAlteradoPor().getNome(), historico.getObservacao());
	}

}
