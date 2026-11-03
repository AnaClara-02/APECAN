package com.aclg.apecan.despesa.dto;

import com.aclg.apecan.despesa.entity.BancoPagamentoDespesa;
import com.aclg.apecan.shared.validation.CnpjFormatter;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DespesaDto(Long id, String tipo, String descricao, String nomeEmitente, String cnpjEmitente,
		String numeroNotaFiscal, LocalDate dataEmissao, BancoPagamentoDespesa bancoPagamento,
		String outroBancoPagamento, BigDecimal valor, LocalDate dataPagamento, String destino) {

	public boolean possuiNotaFiscal() {
		return numeroNotaFiscal != null && !numeroNotaFiscal.isBlank();
	}

	public boolean dadosFiscaisAntigosIncompletos() {
		return possuiNotaFiscal() && (nomeEmitente == null || cnpjEmitente == null || dataEmissao == null
				|| bancoPagamento == null);
	}

	public String nomeEmitenteExibido() {
		if (nomeEmitente != null) return nomeEmitente;
		return dadosFiscaisAntigosIncompletos() ? "Não informado — registro anterior à atualização" : "—";
	}

	public String cnpjFormatado() {
		if (cnpjEmitente != null) return CnpjFormatter.formatar(cnpjEmitente);
		return dadosFiscaisAntigosIncompletos() ? "Não informado — registro anterior à atualização" : "—";
	}

	public String numeroNotaExibido() {
		return possuiNotaFiscal() ? numeroNotaFiscal : "Sem nota fiscal";
	}

	public String dataEmissaoExibida() {
		if (dataEmissao != null) return dataEmissao.toString();
		return dadosFiscaisAntigosIncompletos() ? "Não informado — registro anterior à atualização" : "—";
	}

	public String bancoPagamentoDescricao() {
		if (bancoPagamento == null)
			return dadosFiscaisAntigosIncompletos() ? "Não informado — registro anterior à atualização" : "—";
		return bancoPagamento == BancoPagamentoDespesa.OUTRO ? outroBancoPagamento : bancoPagamento.getDescricao();
	}

	public boolean isPossuiNotaFiscal() { return possuiNotaFiscal(); }
	public boolean isDadosFiscaisAntigosIncompletos() { return dadosFiscaisAntigosIncompletos(); }
	public String getNomeEmitenteExibido() { return nomeEmitenteExibido(); }
	public String getCnpjFormatado() { return cnpjFormatado(); }
	public String getNumeroNotaExibido() { return numeroNotaExibido(); }
	public String getDataEmissaoExibida() { return dataEmissaoExibida(); }
	public String getBancoPagamentoDescricao() { return bancoPagamentoDescricao(); }
}
