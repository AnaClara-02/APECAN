package com.aclg.apecan.paciente.controller;

import com.aclg.apecan.paciente.dto.AlterarStatusPacienteForm;
import com.aclg.apecan.paciente.dto.EditarPacienteForm;
import com.aclg.apecan.paciente.dto.NovoPacienteForm;
import com.aclg.apecan.paciente.dto.PacienteDetalheResultado;
import com.aclg.apecan.paciente.entity.StatusPaciente;
import com.aclg.apecan.paciente.service.PacienteService;
import com.aclg.apecan.shared.exception.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pacientes")
public class PacienteController {

	private final PacienteService pacienteService;

	public PacienteController(PacienteService pacienteService) {
		this.pacienteService = pacienteService;
	}

	@GetMapping
	String listar(@RequestParam(defaultValue = "") String nome, @RequestParam(defaultValue = "") String cpf,
			@RequestParam(required = false) StatusPaciente status, @RequestParam(defaultValue = "0") int pagina,
			Model model) {
		model.addAttribute("pagina", pacienteService.listar(nome, cpf, status, pagina));
		model.addAttribute("nome", nome);
		model.addAttribute("cpf", cpf);
		model.addAttribute("statusSelecionado", status);
		model.addAttribute("statusDisponiveis", StatusPaciente.values());
		return "pacientes/lista";
	}

	@GetMapping("/novo")
	String novo(Model model) {
		if (!model.containsAttribute("novoPacienteForm")) {
			model.addAttribute("novoPacienteForm", new NovoPacienteForm());
		}
		return "pacientes/novo";
	}

	@PostMapping
	String cadastrar(@Valid @ModelAttribute("novoPacienteForm") NovoPacienteForm formulario,
			BindingResult bindingResult, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return "pacientes/novo";
		}
		try {
			Long id = pacienteService.cadastrar(formulario);
			redirectAttributes.addFlashAttribute("sucesso", "Paciente cadastrado.");
			return "redirect:/pacientes/" + id;
		}
		catch (RegraNegocioException exception) {
			bindingResult.reject(exception.getCodigo(), exception.getMessage());
			return "pacientes/novo";
		}
	}

	@GetMapping("/{id}")
	String detalhe(@PathVariable Long id, Model model) {
		prepararDetalhe(id, model);
		return "pacientes/detalhe";
	}

	@GetMapping("/{id}/editar")
	String editar(@PathVariable Long id, Model model) {
		model.addAttribute("paciente", pacienteService.buscar(id).paciente());
		if (!model.containsAttribute("editarPacienteForm")) {
			model.addAttribute("editarPacienteForm", pacienteService.formularioEdicao(id));
		}
		return "pacientes/editar";
	}

	@PostMapping("/{id}/editar")
	String atualizar(@PathVariable Long id, @Valid @ModelAttribute("editarPacienteForm") EditarPacienteForm formulario,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("paciente", pacienteService.buscar(id).paciente());
			return "pacientes/editar";
		}
		try {
			pacienteService.atualizar(id, formulario);
			redirectAttributes.addFlashAttribute("sucesso", "Paciente atualizado.");
			return "redirect:/pacientes/" + id;
		}
		catch (RegraNegocioException exception) {
			bindingResult.reject(exception.getCodigo(), exception.getMessage());
			model.addAttribute("paciente", pacienteService.buscar(id).paciente());
			return "pacientes/editar";
		}
	}

	@PostMapping("/{id}/status")
	String alterarStatus(@PathVariable Long id,
			@Valid @ModelAttribute("alterarStatusPacienteForm") AlterarStatusPacienteForm formulario,
			BindingResult bindingResult, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("erro", bindingResult.getAllErrors().getFirst().getDefaultMessage());
			return "redirect:/pacientes/" + id;
		}
		try {
			pacienteService.alterarStatus(id, formulario);
			redirectAttributes.addFlashAttribute("sucesso", "Status atualizado.");
		}
		catch (RegraNegocioException exception) {
			redirectAttributes.addFlashAttribute("erro", exception.getMessage());
		}
		return "redirect:/pacientes/" + id;
	}

	private void prepararDetalhe(Long id, Model model) {
		PacienteDetalheResultado resultado = pacienteService.buscar(id);
		model.addAttribute("paciente", resultado.paciente());
		model.addAttribute("historico", resultado.historico());
		model.addAttribute("statusDisponiveis", StatusPaciente.values());
		if (!model.containsAttribute("alterarStatusPacienteForm")) {
			model.addAttribute("alterarStatusPacienteForm", new AlterarStatusPacienteForm());
		}
	}

}
