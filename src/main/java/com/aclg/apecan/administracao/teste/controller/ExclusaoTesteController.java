package com.aclg.apecan.administracao.teste.controller;

import com.aclg.apecan.administracao.teste.entity.TipoRegistroExclusaoTeste;
import com.aclg.apecan.administracao.teste.service.ExclusaoTesteService;
import com.aclg.apecan.shared.exception.RegraNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/administracao/testes/exclusoes")
public class ExclusaoTesteController {

    private final ExclusaoTesteService service;

    public ExclusaoTesteController(ExclusaoTesteService service) {
        this.service = service;
    }

    @GetMapping("/{tipo}/{id}")
    String confirmar(@PathVariable TipoRegistroExclusaoTeste tipo, @PathVariable Long id,
            @RequestParam(required = false) String voltar, Model model) {
        model.addAttribute("previa", service.analisar(tipo, id));
        model.addAttribute("voltar", destino(tipo, voltar));
        return "administracao/testes/confirmar-exclusao";
    }

    @PostMapping("/{tipo}/{id}")
    String excluir(@PathVariable TipoRegistroExclusaoTeste tipo, @PathVariable Long id,
            @RequestParam(required = false) String voltar, RedirectAttributes redirect) {
        String destino = destino(tipo, voltar);
        try {
            service.excluir(tipo, id);
            redirect.addFlashAttribute("sucesso", tipo.rotulo() + " removido definitivamente.");
        }
        catch (RegraNegocioException exception) {
            redirect.addFlashAttribute("erro", exception.getMessage());
        }
        return "redirect:" + destino;
    }

    private String destino(TipoRegistroExclusaoTeste tipo, String solicitado) {
        String padrao = switch (tipo) {
            case PACIENTE -> "/pacientes";
            case VOLUNTARIO -> "/voluntarios";
            case EQUIPAMENTO, CATEGORIA -> "/equipamentos";
            case EMPRESTIMO -> "/emprestimos";
            case DOACAO -> "/doacoes";
            case DESPESA -> "/financeiro/despesas";
            case MOVIMENTACAO -> "/financeiro";
        };
        if (solicitado == null || solicitado.length() > 1500 || !solicitado.startsWith("/")
                || solicitado.startsWith("//")) return padrao;
        try {
            java.net.URI uri = java.net.URI.create(solicitado);
            if (uri.isAbsolute() || uri.getRawAuthority() != null || !padrao.equals(uri.getPath())) return padrao;
            java.util.Set<String> permitidos = switch (tipo) {
                case PACIENTE -> java.util.Set.of("pagina", "nome", "cpf", "status", "todos");
                case VOLUNTARIO -> java.util.Set.of("pagina", "nome", "status", "todos");
                case EQUIPAMENTO -> java.util.Set.of("pagina", "categoriaId", "status", "todos");
                case CATEGORIA -> java.util.Set.of();
                case EMPRESTIMO -> java.util.Set.of("pagina", "status");
                case DOACAO -> java.util.Set.of("pagina", "tipo", "inicio", "fim");
                case DESPESA -> java.util.Set.of("pagina");
                case MOVIMENTACAO -> java.util.Set.of("pagina", "tipo", "inicio", "fim");
            };
            String query = uri.getRawQuery();
            if (query == null) return uri.getRawPath();
            for (String par : query.split("&")) {
                String[] partes = par.split("=", 2);
                if (partes.length != 2 || !permitidos.contains(partes[0])
                        || !partes[1].matches("[A-Za-z0-9%._,+-]*")) return padrao;
            }
            return uri.toASCIIString();
        }
        catch (IllegalArgumentException exception) {
            return padrao;
        }
    }
}
