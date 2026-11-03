package com.aclg.apecan.despesa.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.aclg.apecan.despesa.dto.DespesaForm;
import com.aclg.apecan.despesa.entity.BancoPagamentoDespesa;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DespesaFormValidationTests {
    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void iniciarValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        factory.close();
    }

    @Test
    void permiteCadastroSemNotaComBancoSelecionado() {
        assertThat(validator.validate(formularioBase())).isEmpty();
    }

    @Test
    void exigeDadosFiscaisQuandoNotaEstaMarcadaEValidaCnpj() {
        DespesaForm form = formularioBase();
        form.setPossuiNotaFiscal(true);
        var campos = validator.validate(form).stream()
                .map(violacao -> violacao.getPropertyPath().toString()).toList();
        assertThat(campos).contains("nomeEmitente", "cnpjEmitente", "numeroNotaFiscal", "dataEmissao");

        form.setNomeEmitente("Fornecedor de teste");
        form.setCnpjEmitente("ABC12345678923");
        form.setNumeroNotaFiscal("NF-TESTE-15");
        form.setDataEmissao(LocalDate.now().minusDays(1));
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void exigeNomeDoBancoSomenteQuandoOutroForSelecionado() {
        DespesaForm form = formularioBase();
        form.setBancoPagamento(BancoPagamentoDespesa.OUTRO);
        assertThat(validator.validate(form).stream()
                .map(violacao -> violacao.getPropertyPath().toString()))
                .contains("outroBancoPagamento");
        form.setOutroBancoPagamento("Banco de teste");
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void rejeitaValoresNegativosZeroEAcimaDaPrecisaoMonetaria() {
        DespesaForm form = formularioBase();
        form.setValor(new BigDecimal("-0.01"));
        assertThat(validator.validate(form).stream()
                .map(violacao -> violacao.getPropertyPath().toString()))
                .contains("valor");
        form.setValor(BigDecimal.ZERO);
        assertThat(validator.validate(form).stream()
                .map(violacao -> violacao.getPropertyPath().toString()))
                .contains("valor");
        form.setValor(new BigDecimal("10000000000.00"));
        assertThat(validator.validate(form).stream()
                .map(violacao -> violacao.getPropertyPath().toString()))
                .contains("valor");
        form.setValor(new BigDecimal("1.001"));
        assertThat(validator.validate(form).stream()
                .map(violacao -> violacao.getPropertyPath().toString()))
                .contains("valor");
    }

    @Test
    void rejeitaDataDeEmissaoFutura() {
        DespesaForm form = formularioBase();
        form.setPossuiNotaFiscal(true);
        form.setNomeEmitente("Fornecedor de teste");
        form.setCnpjEmitente("11222333000181");
        form.setNumeroNotaFiscal("NF-TESTE-15");
        form.setDataEmissao(LocalDate.now().plusDays(1));
        Set<?> erros = validator.validate(form);
        assertThat(erros).isNotEmpty();
    }

    private DespesaForm formularioBase() {
        DespesaForm form = new DespesaForm();
        form.setTipo("Energia");
        form.setDescricao("Conta mensal");
        form.setValor(new BigDecimal("50.00"));
        form.setDataPagamento(LocalDate.now());
        form.setDestino("Concessionária");
        form.setBancoPagamento(BancoPagamentoDespesa.SICOOB);
        return form;
    }
}
