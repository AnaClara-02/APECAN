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
    void exportar(@PathVariable String tipo, @RequestParam FormatoRelatorio formato,
            @RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim,
            jakarta.servlet.http.HttpServletResponse resposta) throws java.io.IOException {
        try (RelatorioArquivo arquivo = exportacaoService.exportar(TipoRelatorio.deSlug(tipo), formato, inicio, fim)) {
            resposta.setContentType(arquivo.mimeType());
            resposta.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            resposta.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo.nomeArquivo() + "\"");
            resposta.setHeader("X-Content-Type-Options", "nosniff");
            resposta.setContentLengthLong(java.nio.file.Files.size(arquivo.caminho()));
            java.nio.file.Files.copy(arquivo.caminho(), resposta.getOutputStream());
        }
    }

    @ExceptionHandler(com.aclg.apecan.shared.exception.OperacaoInvalidaException.class)
    ResponseEntity<String> exportacaoRecusada(com.aclg.apecan.shared.exception.OperacaoInvalidaException e) {
        HttpStatus status = "EXPORTACAO_EM_ANDAMENTO".equals(e.getCodigo())
                ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).contentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore()).body(e.getMessage());
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
