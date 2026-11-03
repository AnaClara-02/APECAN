package com.aclg.apecan.financeiro.controller;

import com.aclg.apecan.despesa.dto.DespesaForm;
import com.aclg.apecan.despesa.entity.BancoPagamentoDespesa;
import com.aclg.apecan.financeiro.dto.MovimentacaoForm;
import com.aclg.apecan.financeiro.entity.TipoMovimentacao;
import com.aclg.apecan.financeiro.service.FinanceiroService;
import com.aclg.apecan.shared.exception.RegraNegocioException;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
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
@RequestMapping("/financeiro")
public class FinanceiroController {
    private final FinanceiroService service;
    private final Clock clock;

    public FinanceiroController(FinanceiroService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    String listar(@RequestParam(required = false) TipoMovimentacao tipo,
                  @RequestParam(required = false) LocalDate inicio,
                  @RequestParam(required = false) LocalDate fim,
                  @RequestParam(defaultValue = "0") int pagina, Model model) {
        try {
            model.addAttribute("pagina", service.listar(tipo, inicio, fim, pagina));
            model.addAttribute("saldo", service.saldo(inicio, fim));
        }
        catch (RegraNegocioException exception) {
            model.addAttribute("erro", exception.getMessage());
            model.addAttribute("pagina", service.listar(tipo, null, null, 0));
            model.addAttribute("saldo", service.saldo(null, null));
        }
        model.addAttribute("tipos", TipoMovimentacao.values());
        model.addAttribute("tipoSelecionado", tipo);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        return "financeiro/lista";
    }

    @GetMapping("/nova")
    String nova(Model model) {
        MovimentacaoForm form = new MovimentacaoForm();
        form.setData(LocalDate.now(clock));
        model.addAttribute("movimentacaoForm", form);
        model.addAttribute("tipos", TipoMovimentacao.values());
        return "financeiro/formulario";
    }

    @PostMapping
    String criar(@Valid @ModelAttribute MovimentacaoForm movimentacaoForm, BindingResult erros,
                 Model model, RedirectAttributes redirect) {
        if (erros.hasErrors()) {
            model.addAttribute("tipos", TipoMovimentacao.values());
            return "financeiro/formulario";
        }
        service.registrar(movimentacaoForm);
        redirect.addFlashAttribute("sucesso", "Movimentação registrada.");
        return "redirect:/financeiro";
    }

    @GetMapping("/despesas")
    String despesas(@RequestParam(required = false) LocalDate inicio,
                    @RequestParam(required = false) LocalDate fim,
                    @RequestParam(defaultValue = "0") int pagina, Model model) {
        try {
            model.addAttribute("pagina", service.despesas(inicio, fim, pagina));
            model.addAttribute("totalDespesas", service.totalDespesas(inicio, fim));
        }
        catch (RegraNegocioException exception) {
            model.addAttribute("erro", exception.getMessage());
            inicio = null;
            fim = null;
            model.addAttribute("pagina", service.despesas(null, null, 0));
            model.addAttribute("totalDespesas", service.totalDespesas(null, null));
            pagina = 0;
        }
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("paginaAtual", pagina);
        return "despesas/lista";
    }

    @GetMapping("/despesas/nova")
    String novaDespesa(Model model) {
        model.addAttribute("despesaForm", formularioVazio());
        prepararFormularioDespesa(model);
        return "despesas/formulario";
    }

    @PostMapping("/despesas")
    String despesa(@Valid @ModelAttribute("despesaForm") DespesaForm formulario,
                   BindingResult erros, Model model, RedirectAttributes redirect) {
        if (erros.hasErrors()) {
            prepararFormularioDespesa(model);
            return "despesas/formulario";
        }
        service.registrarDespesa(formulario);
        redirect.addFlashAttribute("sucesso",
                "Despesa e saída financeira registradas na mesma operação.");
        return "redirect:/financeiro/despesas";
    }

    @GetMapping("/despesas/{id}")
    String detalheDespesa(@PathVariable Long id,
                          @RequestParam(required = false) LocalDate inicio,
                          @RequestParam(required = false) LocalDate fim,
                          @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("despesa", service.buscarDespesa(id));
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("paginaRetorno", Math.max(0, pagina));
        return "despesas/detalhe";
    }

    private DespesaForm formularioVazio() {
        DespesaForm form = new DespesaForm();
        form.setDataPagamento(LocalDate.now(clock));
        return form;
    }

    private void prepararFormularioDespesa(Model model) {
        model.addAttribute("bancosPagamento", BancoPagamentoDespesa.values());
        model.addAttribute("dataMaxima", LocalDate.now(clock));
    }
}
