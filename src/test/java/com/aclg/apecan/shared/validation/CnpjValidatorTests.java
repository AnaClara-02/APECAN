package com.aclg.apecan.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CnpjValidatorTests {

    @Test
    void aceitaCnpjNumericoEAlfanumericoComOuSemMascara() {
        assertThat(CnpjValidator.valido("11.222.333/0001-81")).isTrue();
        assertThat(CnpjValidator.valido("ABC12345678923")).isTrue();
        assertThat(CnpjValidator.valido("abc.123.456/78923")).isTrue();
        assertThat(CnpjValidator.normalizar(" abc.123.456/78923 ")).isEqualTo("ABC12345678923");
    }

    @Test
    void rejeitaDigitosVerificadoresIncorretosRepeticoesEFormatosInvalidos() {
        assertThat(CnpjValidator.valido("11.222.333/0001-82")).isFalse();
        assertThat(CnpjValidator.valido("00.000.000/0000-00")).isFalse();
        assertThat(CnpjValidator.valido("ABC1234567892A")).isFalse();
        assertThat(CnpjValidator.valido("ABC1234567892!")).isFalse();
        assertThat(CnpjValidator.valido("")).isFalse();
        assertThat(CnpjValidator.valido(null)).isFalse();
    }

    @Test
    void formataCnpjNumericoOuAlfanumericoSemAlterarOsDadosPersistidos() {
        assertThat(CnpjFormatter.formatar("11222333000181")).isEqualTo("11.222.333/0001-81");
        assertThat(CnpjFormatter.formatar("ABC12345678923")).isEqualTo("AB.C12.345/6789-23");
    }
}
