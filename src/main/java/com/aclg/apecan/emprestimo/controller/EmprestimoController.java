package com.aclg.apecan.emprestimo.controller;

import com.aclg.apecan.emprestimo.dto.*;
import com.aclg.apecan.emprestimo.service.EmprestimoService;
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
@RequestMapping("/emprestimos")
public class EmprestimoController {

	private final EmprestimoService service;

	public EmprestimoController(EmprestimoService s) {
		service = s;
	}

	@GetMapping
	String listar(@RequestParam(defaultValue = "true") boolean abertos, @RequestParam(defaultValue = "0") int pagina,
			Model m) {
		m.addAttribute("pagina", service.listar(abertos, pagina));
		m.addAttribute("abertos", abertos);
		return "emprestimos/lista";
	}

	@GetMapping("/novo")
	String novo(Model m) {
		EmprestimoForm f = new EmprestimoForm();
		f.setDataEmprestimo(LocalDate.now());
		preparar(f, m);
		return "emprestimos/novo";
	}

	@PostMapping
	String criar(@Valid @ModelAttribute EmprestimoForm emprestimoForm, BindingResult b, Model m, RedirectAttributes r) {
		if (b.hasErrors()) {
			preparar(emprestimoForm, m);
			return "emprestimos/novo";
		}
		try {
			Long id = service.emprestar(emprestimoForm);
			r.addFlashAttribute("sucesso", "Emprestimo registrado.");
			return "redirect:/emprestimos/" + id;
		}
		catch (RegraNegocioException e) {
			b.reject(e.getCodigo(), e.getMessage());
			preparar(emprestimoForm, m);
			return "emprestimos/novo";
		}
	}

	@GetMapping("/{id}")
	String detalhe(@PathVariable Long id, Model m) {
		m.addAttribute("emprestimo", service.buscar(id));
		DevolucaoForm f = new DevolucaoForm();
		f.setDataDevolucao(LocalDate.now());
		m.addAttribute("devolucaoForm", f);
		m.addAttribute("conservacoes", EstadoConservacao.values());
		return "emprestimos/detalhe";
	}

	@PostMapping("/{id}/devolucao")
	String devolver(@PathVariable Long id, @Valid @ModelAttribute DevolucaoForm devolucaoForm, BindingResult b,
			RedirectAttributes r) {
		if (b.hasErrors())
			r.addFlashAttribute("erro", "Revise os dados da devolucao.");
		else
			try {
				service.devolver(id, devolucaoForm);
				r.addFlashAttribute("sucesso", "Devolucao registrada.");
			}
			catch (RegraNegocioException e) {
				r.addFlashAttribute("erro", e.getMessage());
			}
		return "redirect:/emprestimos/" + id;
	}

	private void preparar(EmprestimoForm f, Model m) {
		m.addAttribute("emprestimoForm", f);
		m.addAttribute("equipamentos", service.disponiveis());
		m.addAttribute("pacientes", service.pacientesAtivos());
	}

}
