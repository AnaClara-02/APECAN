package com.aclg.apecan.auth.controller;

import com.aclg.apecan.usuario.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    private final UsuarioService usuarioService;

    public InicioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/")
    String raiz() {
        return "redirect:/inicio";
    }

    @GetMapping("/inicio")
    String inicio(Model model) {
        model.addAttribute("usuario", usuarioService.buscarAtual());
        model.addAttribute(
            "quantidadeAdministradores",
            usuarioService.quantidadeAdministradoresAtivos()
        );
        return "inicio";
    }
}
