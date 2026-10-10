package com.aclg.apecan.usuario.controller;

import com.aclg.apecan.auth.service.AtivacaoEmitida;
import com.aclg.apecan.shared.exception.RegraNegocioException;
import com.aclg.apecan.usuario.dto.AlterarAcessoForm;
import com.aclg.apecan.usuario.dto.EditarUsuarioForm;
import com.aclg.apecan.usuario.dto.NovoUsuarioForm;
import com.aclg.apecan.usuario.dto.UsuarioCriadoResultado;
import com.aclg.apecan.usuario.entity.TipoPerfil;
import com.aclg.apecan.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.function.Consumer;

@Controller
@RequestMapping("/usuarios")
public class UsuarioAdminController {

    private final UsuarioService usuarioService;

    public UsuarioAdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute(
            "quantidadeAdministradores",
            usuarioService.quantidadeAdministradoresAtivos()
        );
        return "usuarios/lista";
    }

    @GetMapping("/novo")
    String novo(Model model) {
        if (!model.containsAttribute("novoUsuarioForm")) {
            model.addAttribute("novoUsuarioForm", new NovoUsuarioForm());
        }
        return "usuarios/novo";
    }

    @PostMapping
    String cadastrar(
            @Valid @ModelAttribute("novoUsuarioForm") NovoUsuarioForm formulario,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "usuarios/novo";
        }
        try {
            UsuarioCriadoResultado resultado = usuarioService.cadastrar(formulario);
            prepararEntregaAtivacao(resultado.usuario().nome(), resultado.ativacao(), model);
            return "usuarios/ativacao-local";
        } catch (RegraNegocioException exception) {
            bindingResult.reject(exception.getCodigo(), exception.getMessage());
            return "usuarios/novo";
        }
    }

    @GetMapping("/{id}")
    String detalhe(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", usuarioService.buscar(id));
        model.addAttribute("podeGerenciarUsuario", usuarioService.podeGerenciarUsuario(id));
        model.addAttribute("perfisDisponiveis", usuarioService.perfisDisponiveis(id));
        if (!model.containsAttribute("alterarAcessoForm")) {
            model.addAttribute("alterarAcessoForm", new AlterarAcessoForm());
        }
        return "usuarios/detalhe";
    }

    @PostMapping("/{id}/perfil")
    String alterarPerfil(
            @PathVariable Long id,
            @Valid @ModelAttribute AlterarAcessoForm formulario,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        TipoPerfil perfil = formulario.getNovoPerfil();
        if (perfil == null) {
            redirectAttributes.addFlashAttribute("erro", "Selecione o perfil de destino.");
            return "redirect:/usuarios/" + id;
        }
        return executarAlteracao(
            id,
            formulario,
            bindingResult,
            dados -> usuarioService.alterarPerfil(id, perfil, dados),
            "Perfil do usuário atualizado.",
            redirectAttributes
        );
    }

    @GetMapping("/{id}/editar")
    String editar(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", usuarioService.buscar(id));
        if (!model.containsAttribute("editarUsuarioForm")) {
            model.addAttribute("editarUsuarioForm", usuarioService.formularioEdicao(id));
        }
        return "usuarios/editar";
    }

    @PostMapping("/{id}/editar")
    String atualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("editarUsuarioForm") EditarUsuarioForm formulario,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("usuario", usuarioService.buscar(id));
            return "usuarios/editar";
        }
        try {
            usuarioService.atualizar(id, formulario);
            redirectAttributes.addFlashAttribute("sucesso", "Dados atualizados.");
            return "redirect:/usuarios/" + id;
        } catch (RegraNegocioException exception) {
            bindingResult.reject(exception.getCodigo(), exception.getMessage());
            model.addAttribute("usuario", usuarioService.buscar(id));
            return "usuarios/editar";
        }
    }

    @PostMapping("/{id}/reenviar-ativacao")
    String reemitir(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            AtivacaoEmitida ativacao = usuarioService.reemitirAtivacao(id);
            prepararEntregaAtivacao(usuarioService.buscar(id).nome(), ativacao, model);
            return "usuarios/ativacao-local";
        } catch (RegraNegocioException exception) {
            redirectAttributes.addFlashAttribute("erro", exception.getMessage());
            return "redirect:/usuarios/" + id;
        }
    }

    @PostMapping("/{id}/promover")
    String promover(
            @PathVariable Long id,
            @Valid @ModelAttribute AlterarAcessoForm formulario,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        return executarAlteracao(
            id,
            formulario,
            bindingResult,
            dados -> usuarioService.promover(id, dados),
            "Usuario promovido a administrador.",
            redirectAttributes
        );
    }

    @PostMapping("/{id}/rebaixar")
    String rebaixar(
            @PathVariable Long id,
            @Valid @ModelAttribute AlterarAcessoForm formulario,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        return executarAlteracao(
            id,
            formulario,
            bindingResult,
            dados -> usuarioService.rebaixar(id, dados),
            "Administrador alterado para usuario.",
            redirectAttributes
        );
    }

    @PostMapping("/{id}/desativar")
    String desativar(
            @PathVariable Long id,
            @Valid @ModelAttribute AlterarAcessoForm formulario,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        return executarAlteracao(
            id,
            formulario,
            bindingResult,
            dados -> usuarioService.desativar(id, dados),
            "Usuario desativado.",
            redirectAttributes
        );
    }

    @PostMapping("/{id}/reativar")
    String reativar(
            @PathVariable Long id,
            @Valid @ModelAttribute AlterarAcessoForm formulario,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        return executarAlteracao(
            id,
            formulario,
            bindingResult,
            dados -> usuarioService.reativar(id, dados),
            "Usuario reativado.",
            redirectAttributes
        );
    }

    private String executarAlteracao(
            Long id,
            AlterarAcessoForm formulario,
            BindingResult bindingResult,
            Consumer<AlterarAcessoForm> operacao,
            String mensagemSucesso,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                "erro",
                bindingResult.getAllErrors().getFirst().getDefaultMessage()
            );
            return "redirect:/usuarios/" + id;
        }
        try {
            operacao.accept(formulario);
            redirectAttributes.addFlashAttribute("sucesso", mensagemSucesso);
        } catch (RegraNegocioException exception) {
            redirectAttributes.addFlashAttribute("erro", exception.getMessage());
        }
        return "redirect:/usuarios/" + id;
    }

    private void prepararEntregaAtivacao(
            String nome,
            AtivacaoEmitida ativacao,
            Model model) {
        model.addAttribute("nomeUsuario", nome);
        model.addAttribute("ativacao", ativacao);
    }
}
