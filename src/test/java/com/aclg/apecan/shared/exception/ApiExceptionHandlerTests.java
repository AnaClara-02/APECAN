package com.aclg.apecan.shared.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void prepararMockMvc() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new ControladorDeTeste())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    }

    @Test
    void deveRetornarProblemaParaRecursoNaoEncontrado() throws Exception {
        mockMvc.perform(get("/api/teste/nao-encontrado"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.codigo").value("TESTE_NAO_ENCONTRADO"))
            .andExpect(jsonPath("$.erroId").isNotEmpty())
            .andExpect(jsonPath("$.instance").value("/api/teste/nao-encontrado"));
    }

    @Test
    void deveRetornarProblemaParaConflitoEOperacaoInvalida() throws Exception {
        mockMvc.perform(get("/api/teste/conflito"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("TESTE_CONFLITO"));

        mockMvc.perform(get("/api/teste/invalida"))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.codigo").value("TESTE_INVALIDO"));
    }

    @Test
    void erroInesperadoNaoDeveExporDetalhesInternos() throws Exception {
        mockMvc.perform(get("/api/teste/inesperado"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
            .andExpect(jsonPath("$.detail").value("Não foi possível concluir a operação."))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("segredo técnico")
            )));
    }

    @RestController
    @RequestMapping("/api/teste")
    static class ControladorDeTeste {

        @GetMapping("/{tipo}")
        void falhar(@PathVariable String tipo) {
            switch (tipo) {
                case "nao-encontrado" -> throw new RecursoNaoEncontradoException(
                    "TESTE_NAO_ENCONTRADO", "Recurso de teste não encontrado."
                );
                case "conflito" -> throw new ConflitoNegocioException(
                    "TESTE_CONFLITO", "Conflito de teste."
                );
                case "invalida" -> throw new OperacaoInvalidaException(
                    "TESTE_INVALIDO", "Operação de teste inválida."
                );
                default -> throw new IllegalStateException("segredo técnico");
            }
        }
    }
}
