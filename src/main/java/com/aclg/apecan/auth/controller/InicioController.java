package com.aclg.apecan.auth.controller;

import com.aclg.apecan.usuario.service.UsuarioService;
import com.aclg.apecan.relatorio.service.RelatorioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

	private final UsuarioService usuarioService;
	private final RelatorioService relatorioService;

	public InicioController(UsuarioService usuarioService, RelatorioService relatorioService) {
		this.usuarioService = usuarioService;
		this.relatorioService = relatorioService;
    }

    @GetMapping("/")
    String raiz() {
        return "redirect:/inicio";
    }

    @GetMapping("/inicio")
    String inicio(Model model) {
		model.addAttribute(
			"quantidadeAdministradores",
			usuarioService.quantidadeAdministradoresAtivos()
		);
		model.addAttribute("resumo", relatorioService.resumoInicio());
		return "inicio";
    }
}
