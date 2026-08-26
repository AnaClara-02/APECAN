package com.aclg.apecan.voluntario.dto;

import com.aclg.apecan.voluntario.entity.StatusVoluntario;
import java.time.LocalDate;

public record VoluntarioDto(Long id, String nome, String cpfMascarado, LocalDate dataNascimento, String telefone,
		String endereco, StatusVoluntario status) {
}
