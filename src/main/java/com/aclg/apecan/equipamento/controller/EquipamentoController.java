package com.aclg.apecan.equipamento.controller;

import com.aclg.apecan.equipamento.dto.*;
import com.aclg.apecan.equipamento.entity.*;
import com.aclg.apecan.equipamento.service.EquipamentoService;
import com.aclg.apecan.emprestimo.service.EmprestimoService;
import com.aclg.apecan.shared.exception.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/equipamentos")
public class EquipamentoController {

	private final EquipamentoService service;
	private final EmprestimoService emprestimoService;

	public EquipamentoController(EquipamentoService s, EmprestimoService emprestimoService) {
		service = s;
		this.emprestimoService = emprestimoService;
	}

	@GetMapping
	String listar(@RequestParam(required = false) Long categoriaId,
			@RequestParam(required = false) StatusEquipamento status, @RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "false") boolean todos,
			Model m) {
		if (status == null && !todos)
			status = StatusEquipamento.ATIVO;
		m.addAttribute("pagina", service.listar(categoriaId, status, pagina));
		m.addAttribute("categorias", service.categorias());
		m.addAttribute("resumo", service.resumo());
		m.addAttribute("estados", StatusEquipamento.values());
		m.addAttribute("categoriaSelecionada", categoriaId);
		m.addAttribute("statusSelecionado", status);
		m.addAttribute("todosStatus", todos);
		return "equipamentos/lista";
	}

	@GetMapping("/novo")
	String novo(Model m) {
		preparar(new EquipamentoForm(), m);
		return "equipamentos/formulario";
	}

	@PostMapping
	String criar(@Valid @ModelAttribute EquipamentoForm equipamentoForm, BindingResult b, Model m,
			RedirectAttributes r) {
		if (b.hasErrors()) {
			preparar(equipamentoForm, m);
			return "equipamentos/formulario";
		}
		Long id = service.cadastrar(equipamentoForm);
		r.addFlashAttribute("sucesso", "Equipamento cadastrado.");
		return "redirect:/equipamentos/" + id;
	}

	@GetMapping("/{id}")
	String detalhe(@PathVariable Long id, @RequestParam(defaultValue = "0") int paginaHistorico, Model m) {
		m.addAttribute("equipamento", service.buscar(id));
		m.addAttribute("historico", emprestimoService.historicoEquipamento(id, paginaHistorico));
		return "equipamentos/detalhe";
	}

	@GetMapping("/{id}/editar")
	String editar(@PathVariable Long id, Model m) {
		m.addAttribute("equipamento", service.buscar(id));
		preparar(service.formulario(id), m);
		return "equipamentos/formulario";
	}

	@PostMapping("/{id}/editar")
	String editar(@PathVariable Long id, @Valid @ModelAttribute EquipamentoForm equipamentoForm, BindingResult b,
			Model m, RedirectAttributes r) {
		if (b.hasErrors()) {
			m.addAttribute("equipamento", service.buscar(id));
			preparar(equipamentoForm, m);
			return "equipamentos/formulario";
		}
		service.atualizar(id, equipamentoForm);
		r.addFlashAttribute("sucesso", "Equipamento atualizado.");
		return "redirect:/equipamentos/" + id;
	}

	@PostMapping("/{id}/status")
	String status(@PathVariable Long id, RedirectAttributes r) {
		try {
			service.alterarStatus(id);
			r.addFlashAttribute("sucesso", "Status atualizado.");
		}
		catch (RegraNegocioException e) {
			r.addFlashAttribute("erro", e.getMessage());
		}
		return "redirect:/equipamentos/" + id;
	}

	@PostMapping("/categorias")
	String categoria(@Valid @ModelAttribute CategoriaEquipamentoForm categoriaEquipamentoForm, BindingResult b,
			RedirectAttributes r) {
		if (b.hasErrors())
			r.addFlashAttribute("erro", "Revise os dados da categoria.");
		else
			try {
				service.cadastrarCategoria(categoriaEquipamentoForm);
				r.addFlashAttribute("sucesso", "Categoria cadastrada.");
			}
			catch (RegraNegocioException e) {
				r.addFlashAttribute("erro", e.getMessage());
			}
		return "redirect:/equipamentos";
	}

	@PostMapping("/categorias/{id}/excluir")
	String excluirCategoria(@PathVariable Long id, RedirectAttributes r) {
		try {
			service.excluirCategoria(id);
			r.addFlashAttribute("sucesso", "Categoria excluida.");
		}
		catch (RegraNegocioException e) {
			r.addFlashAttribute("erro", e.getMessage());
		}
		return "redirect:/equipamentos";
	}

	private void preparar(EquipamentoForm f, Model m) {
		m.addAttribute("equipamentoForm", f);
		m.addAttribute("categorias", service.categorias());
		m.addAttribute("conservacoes", EstadoConservacao.values());
	}

}
