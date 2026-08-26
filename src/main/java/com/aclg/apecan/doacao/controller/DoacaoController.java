package com.aclg.apecan.doacao.controller;

import com.aclg.apecan.doacao.dto.DoacaoForm;
import com.aclg.apecan.doacao.entity.TipoDoacao;
import com.aclg.apecan.doacao.service.DoacaoService;
import com.aclg.apecan.equipamento.entity.EstadoConservacao;
import com.aclg.apecan.shared.exception.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
@RequestMapping("/doacoes")
public class DoacaoController {

	private final DoacaoService service;

	public DoacaoController(DoacaoService s) {
		service = s;
	}

	@GetMapping
	String listar(@RequestParam(required = false) TipoDoacao tipo, @RequestParam(required = false) LocalDate inicio,
			@RequestParam(required = false) LocalDate fim, @RequestParam(defaultValue = "0") int pagina, Model m) {
		m.addAttribute("pagina", service.listar(tipo, inicio, fim, pagina));
		m.addAttribute("tipos", TipoDoacao.values());
		m.addAttribute("tipoSelecionado", tipo);
		m.addAttribute("inicio", inicio);
		m.addAttribute("fim", fim);
		return "doacoes/lista";
	}

	@GetMapping("/nova")
	String nova(Model m) {
		DoacaoForm f = new DoacaoForm();
		f.setDataDoacao(LocalDate.now());
		preparar(f, m);
		return "doacoes/formulario";
	}

	@PostMapping
	String criar(@Valid @ModelAttribute DoacaoForm doacaoForm, BindingResult b, Model m, RedirectAttributes r) {
		if (b.hasErrors()) {
			preparar(doacaoForm, m);
			return "doacoes/formulario";
		}
		try {
			service.registrar(doacaoForm);
			r.addFlashAttribute("sucesso", "Doacao registrada com seus efeitos de estoque ou financeiro.");
			return "redirect:/doacoes";
		}
		catch (RegraNegocioException e) {
			b.reject(e.getCodigo(), e.getMessage());
			preparar(doacaoForm, m);
			return "doacoes/formulario";
		}
	}

	private void preparar(DoacaoForm f, Model m) {
		m.addAttribute("doacaoForm", f);
		m.addAttribute("tipos", TipoDoacao.values());
		m.addAttribute("categorias", service.categorias());
		m.addAttribute("conservacoes", EstadoConservacao.values());
		m.addAttribute("voluntarios", service.voluntarios());
	}

}
