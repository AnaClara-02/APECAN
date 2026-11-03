package com.aclg.apecan.despesa.validation;

import com.aclg.apecan.despesa.dto.DespesaForm;
import com.aclg.apecan.despesa.entity.BancoPagamentoDespesa;
import com.aclg.apecan.shared.validation.CnpjValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DadosDespesaValidosValidator implements ConstraintValidator<DadosDespesaValidos, DespesaForm> {
	@Override
	public boolean isValid(DespesaForm form, ConstraintValidatorContext contexto) {
		if (form == null) return true;
		boolean valido = true;
		contexto.disableDefaultConstraintViolation();

		if (form.isPossuiNotaFiscal()) {
			valido &= obrigatorio(form.getNomeEmitente(), "nomeEmitente", "Informe o nome ou a razão social do emitente.", contexto);
			valido &= obrigatorio(form.getCnpjEmitente(), "cnpjEmitente", "Informe o CNPJ do emitente.", contexto);
			if (form.getCnpjEmitente() != null && !form.getCnpjEmitente().isBlank()
					&& !CnpjValidator.valido(form.getCnpjEmitente())) {
				adicionar(contexto, "cnpjEmitente", "Informe um CNPJ válido.");
				valido = false;
			}
			valido &= obrigatorio(form.getNumeroNotaFiscal(), "numeroNotaFiscal", "Informe o número da nota fiscal.", contexto);
			if (form.getDataEmissao() == null) {
				adicionar(contexto, "dataEmissao", "Informe a data de emissão.");
				valido = false;
			}
		}

		if (form.getBancoPagamento() == BancoPagamentoDespesa.OUTRO
				&& (form.getOutroBancoPagamento() == null || form.getOutroBancoPagamento().isBlank())) {
			adicionar(contexto, "outroBancoPagamento", "Informe o nome do banco de pagamento.");
			valido = false;
		}
		return valido;
	}

	private boolean obrigatorio(String valor, String campo, String mensagem, ConstraintValidatorContext contexto) {
		if (valor != null && !valor.isBlank()) return true;
		adicionar(contexto, campo, mensagem);
		return false;
	}

	private void adicionar(ConstraintValidatorContext contexto, String campo, String mensagem) {
		contexto.buildConstraintViolationWithTemplate(mensagem).addPropertyNode(campo).addConstraintViolation();
	}
}
