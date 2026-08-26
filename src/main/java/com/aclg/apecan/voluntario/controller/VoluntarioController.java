package com.aclg.apecan.voluntario.controller;

import com.aclg.apecan.shared.exception.RegraNegocioException;
import com.aclg.apecan.voluntario.dto.VoluntarioForm;
import com.aclg.apecan.voluntario.entity.StatusVoluntario;
import com.aclg.apecan.voluntario.service.VoluntarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/voluntarios")
public class VoluntarioController {

	private final VoluntarioService service;

	public VoluntarioController(VoluntarioService s) {
		service = s;
	}

	@GetMapping
	String listar(@RequestParam(defaultValue = "") String nome, @RequestParam(required = false) StatusVoluntario status,
			@RequestParam(defaultValue = "0") int pagina, Model m) {
		m.addAttribute("pagina", service.listar(nome, status, pagina));
		m.addAttribute("nome", nome);
		m.addAttribute("statusDisponiveis", StatusVoluntario.values());
		m.addAttribute("statusSelecionado", status);
		return "voluntarios/lista";
	}

	@GetMapping("/novo")
	String novo(Model m) {
		m.addAttribute("voluntarioForm", new VoluntarioForm());
		return "voluntarios/formulario";
	}

	@PostMapping
	String criar(@Valid @ModelAttribute VoluntarioForm voluntarioForm, BindingResult b, RedirectAttributes r) {
		if (b.hasErrors())
			return "voluntarios/formulario";
		try {
			Long id = service.cadastrar(voluntarioForm);
			r.addFlashAttribute("sucesso", "Voluntario cadastrado.");
			return "redirect:/voluntarios/" + id;
		}
		catch (RegraNegocioException e) {
			b.reject(e.getCodigo(), e.getMessage());
			return "voluntarios/formulario";
		}
	}

	@GetMapping("/{id}")
	String detalhe(@PathVariable Long id, Model m) {
		m.addAttribute("voluntario", service.buscar(id));
		return "voluntarios/detalhe";
	}

	@GetMapping("/{id}/editar")
	String editar(@PathVariable Long id, Model m) {
		m.addAttribute("voluntario", service.buscar(id));
		m.addAttribute("voluntarioForm", service.formulario(id));
		return "voluntarios/formulario";
	}

	@PostMapping("/{id}/editar")
	String editar(@PathVariable Long id, @Valid @ModelAttribute VoluntarioForm voluntarioForm, BindingResult b, Model m,
			RedirectAttributes r) {
		if (b.hasErrors()) {
			m.addAttribute("voluntario", service.buscar(id));
			return "voluntarios/formulario";
		}
		service.atualizar(id, voluntarioForm);
		r.addFlashAttribute("sucesso", "Voluntario atualizado.");
		return "redirect:/voluntarios/" + id;
	}

	@PostMapping("/{id}/status")
	String status(@PathVariable Long id, RedirectAttributes r) {
		service.alterarStatus(id);
		r.addFlashAttribute("sucesso", "Status atualizado.");
		return "redirect:/voluntarios/" + id;
	}

}
