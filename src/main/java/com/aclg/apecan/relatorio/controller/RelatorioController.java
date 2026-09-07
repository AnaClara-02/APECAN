package com.aclg.apecan.relatorio.controller;

import com.aclg.apecan.relatorio.service.RelatorioService;
import com.aclg.apecan.relatorio.service.RelatorioExportacaoService;
import com.aclg.apecan.relatorio.service.RelatorioArquivo;
import com.aclg.apecan.relatorio.entity.FormatoRelatorio;
import com.aclg.apecan.relatorio.entity.TipoRelatorio;
import com.aclg.apecan.shared.exception.RegraNegocioException;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@Controller
@RequestMapping("/relatorios")
public class RelatorioController {

	private final RelatorioService service;
	private final RelatorioExportacaoService exportacaoService;

	public RelatorioController(RelatorioService s, RelatorioExportacaoService exportacaoService) {
		service = s;
		this.exportacaoService = exportacaoService;
	}

	@GetMapping("/{tipo}/exportar")
	ResponseEntity<byte[]> exportar(@PathVariable String tipo, @RequestParam FormatoRelatorio formato,
			@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim) {
		RelatorioArquivo arquivo = exportacaoService.exportar(TipoRelatorio.deSlug(tipo), formato, inicio, fim);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(arquivo.mimeType()))
			.cacheControl(CacheControl.noStore())
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo.nomeArquivo() + "\"")
			.header("X-Content-Type-Options", "nosniff")
			.body(arquivo.conteudo());
	}

	@GetMapping
	String relatorios(@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim,
			Model m) {
		try {
			m.addAttribute("relatorio", service.gerar(inicio, fim));
		}
		catch (RegraNegocioException exception) {
			m.addAttribute("erro", exception.getMessage());
			m.addAttribute("relatorio", service.gerar(null, null));
		}
		m.addAttribute("inicio", inicio);
		m.addAttribute("fim", fim);
		return "relatorios/painel";
	}

}
