package com.aclg.apecan.shared.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfValidationTests {

    private static Validator validator;

    @BeforeAll
    static void prepararValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void deveAceitarCpfValidoComOuSemMascara() {
        assertThat(validator.validate(new FormularioCpf("52998224725"))).isEmpty();
        assertThat(validator.validate(new FormularioCpf("529.982.247-25"))).isEmpty();
    }

    @Test
    void deveRejeitarDigitosIncorretosESequenciasRepetidas() {
        assertThat(validator.validate(new FormularioCpf("52998224724"))).isNotEmpty();
        assertThat(validator.validate(new FormularioCpf("111.111.111-11"))).isNotEmpty();
    }

    @Test
    void deveRejeitarCpfVazio() {
        assertThat(validator.validate(new FormularioCpf(""))).isNotEmpty();
    }

    @Test
    void deveNormalizarEFormatarCpf() {
        assertThat(CpfNormalizer.normalizar("529.982.247-25"))
            .isEqualTo("52998224725");
        assertThat(CpfFormatter.formatar("52998224725"))
            .isEqualTo("529.982.247-25");
    }

    @Test
    void deveRejeitarQuantidadeDiferenteDeOnzeDigitos() {
        assertThatThrownBy(() -> CpfNormalizer.normalizar("123"))
            .hasMessage("O CPF deve possuir 11 dígitos.");
    }

    private record FormularioCpf(
        @NotBlank(message = "O CPF é obrigatório")
        @CpfValido
        String cpf
    ) {
    }
}
