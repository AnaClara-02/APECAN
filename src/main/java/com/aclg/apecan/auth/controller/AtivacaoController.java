package com.aclg.apecan.auth.controller;

import com.aclg.apecan.auth.dto.AtivacaoForm;
import com.aclg.apecan.auth.service.AtivacaoUsuarioService;
import com.aclg.apecan.shared.exception.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AtivacaoController {

    private final AtivacaoUsuarioService ativacaoService;

    public AtivacaoController(AtivacaoUsuarioService ativacaoService) {
        this.ativacaoService = ativacaoService;
    }

    @GetMapping("/ativar-conta")
    String formulario(@RequestParam(required = false) String token, Model model) {
        AtivacaoForm formulario = new AtivacaoForm();
        formulario.setToken(token);
        model.addAttribute("ativacaoForm", formulario);
        prepararToken(token, model);
        return "ativar-conta";
    }

    @PostMapping("/ativar-conta")
    String ativar(
            @Valid @ModelAttribute("ativacaoForm") AtivacaoForm formulario,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            prepararToken(formulario.getToken(), model);
            return "ativar-conta";
        }

        try {
            ativacaoService.ativar(
                formulario.getToken(),
                formulario.getSenha(),
                formulario.getConfirmacaoSenha()
            );
            return "redirect:/login?ativada";
        } catch (RegraNegocioException exception) {
            model.addAttribute("erroAtivacao", exception.getMessage());
            prepararToken(formulario.getToken(), model);
            return "ativar-conta";
        }
    }

    private void prepararToken(String token, Model model) {
        try {
            model.addAttribute("tokenInfo", ativacaoService.consultar(token));
            model.addAttribute("tokenValido", true);
        } catch (RegraNegocioException exception) {
            model.addAttribute("tokenValido", false);
            model.addAttribute("erroAtivacao", exception.getMessage());
        }
    }
}
