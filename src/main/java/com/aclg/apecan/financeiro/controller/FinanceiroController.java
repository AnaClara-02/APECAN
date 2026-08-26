package com.aclg.apecan.financeiro.controller;

import com.aclg.apecan.despesa.dto.DespesaForm;
import com.aclg.apecan.financeiro.dto.MovimentacaoForm;
import com.aclg.apecan.financeiro.entity.TipoMovimentacao;
import com.aclg.apecan.financeiro.service.FinanceiroService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
@RequestMapping("/financeiro")
public class FinanceiroController {

	private final FinanceiroService service;

	public FinanceiroController(FinanceiroService s) {
		service = s;
	}

	@GetMapping
	String listar(@RequestParam(required = false) TipoMovimentacao tipo,
			@RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim,
			@RequestParam(defaultValue = "0") int pagina, Model m) {
		m.addAttribute("pagina", service.listar(tipo, inicio, fim, pagina));
		m.addAttribute("saldo", service.saldo(inicio, fim));
		m.addAttribute("tipos", TipoMovimentacao.values());
		m.addAttribute("tipoSelecionado", tipo);
		m.addAttribute("inicio", inicio);
		m.addAttribute("fim", fim);
		return "financeiro/lista";
	}

	@GetMapping("/nova")
	String nova(Model m) {
		MovimentacaoForm f = new MovimentacaoForm();
		f.setData(LocalDate.now());
		m.addAttribute("movimentacaoForm", f);
		m.addAttribute("tipos", TipoMovimentacao.values());
		return "financeiro/formulario";
	}

	@PostMapping
	String criar(@Valid @ModelAttribute MovimentacaoForm movimentacaoForm, BindingResult b, Model m,
			RedirectAttributes r) {
		if (b.hasErrors()) {
			m.addAttribute("tipos", TipoMovimentacao.values());
			return "financeiro/formulario";
		}
		service.registrar(movimentacaoForm);
		r.addFlashAttribute("sucesso", "Movimentacao registrada.");
		return "redirect:/financeiro";
	}

	@GetMapping("/despesas")
	String despesas(@RequestParam(defaultValue = "0") int pagina, Model m) {
		m.addAttribute("pagina", service.despesas(pagina));
		return "despesas/lista";
	}

	@GetMapping("/despesas/nova")
	String novaDespesa(Model m) {
		DespesaForm f = new DespesaForm();
		f.setData(LocalDate.now());
		m.addAttribute("despesaForm", f);
		return "despesas/formulario";
	}

	@PostMapping("/despesas")
	String despesa(@Valid @ModelAttribute DespesaForm despesaForm, BindingResult b, RedirectAttributes r) {
		if (b.hasErrors())
			return "despesas/formulario";
		service.registrarDespesa(despesaForm);
		r.addFlashAttribute("sucesso", "Despesa e saida financeira registradas na mesma operacao.");
		return "redirect:/financeiro/despesas";
	}

}
