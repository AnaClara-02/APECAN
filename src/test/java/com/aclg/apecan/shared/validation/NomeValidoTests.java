package com.aclg.apecan.shared.validation;

import com.aclg.apecan.paciente.dto.EditarPacienteForm;
import com.aclg.apecan.paciente.dto.NovoPacienteForm;
import com.aclg.apecan.usuario.dto.EditarUsuarioForm;
import com.aclg.apecan.usuario.dto.NovoUsuarioForm;
import com.aclg.apecan.voluntario.dto.VoluntarioForm;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NomeValidoTests {

    private static Validator validator;

    @BeforeAll
    static void prepararValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void deveAceitarLetrasAcentosEspacosApostrofosEHifens() {
        assertThat(validator.validate(new Formulario("Ana Cláudia"))).isEmpty();
        assertThat(validator.validate(new Formulario("João D'Ávila"))).isEmpty();
        assertThat(validator.validate(new Formulario("Maria-Luísa"))).isEmpty();
    }

    @Test
    void deveRejeitarNumerosESimbolosNoNome() {
        assertThat(validator.validate(new Formulario("Ana 2"))).hasSize(1);
        assertThat(validator.validate(new Formulario("João@Silva"))).hasSize(1);
    }

    @Test
    void deveEstarAplicadoAosFormulariosDePessoas() {
        NovoPacienteForm novoPaciente = new NovoPacienteForm();
        novoPaciente.setNome("Paciente 2");
        EditarPacienteForm editarPaciente = new EditarPacienteForm();
        editarPaciente.setNome("Paciente 2");
        VoluntarioForm voluntario = new VoluntarioForm();
        voluntario.setNome("Voluntário 2");
        NovoUsuarioForm novoUsuario = new NovoUsuarioForm();
        novoUsuario.setNome("Usuário 2");
        EditarUsuarioForm editarUsuario = new EditarUsuarioForm();
        editarUsuario.setNome("Usuário 2");

        assertThat(validator.validateProperty(novoPaciente, "nome")).hasSize(1);
        assertThat(validator.validateProperty(editarPaciente, "nome")).hasSize(1);
        assertThat(validator.validateProperty(voluntario, "nome")).hasSize(1);
        assertThat(validator.validateProperty(novoUsuario, "nome")).hasSize(1);
        assertThat(validator.validateProperty(editarUsuario, "nome")).hasSize(1);
    }

    private record Formulario(@NomeValido String nome) {
    }
}
