package com.aclg.apecan.relatorio.controller;

import com.aclg.apecan.relatorio.service.RelatorioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@Controller
@RequestMapping("/relatorios")
public class RelatorioController {

	private final RelatorioService service;

	public RelatorioController(RelatorioService s) {
		service = s;
	}

	@GetMapping
	String relatorios(@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim,
			Model m) {
		m.addAttribute("relatorio", service.gerar(inicio, fim));
		m.addAttribute("inicio", inicio);
		m.addAttribute("fim", fim);
		return "relatorios/painel";
	}

}
