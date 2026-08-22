package com.aclg.apecan.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ControllerAdvice(basePackages = {
    "com.aclg.apecan.auth.controller",
    "com.aclg.apecan.usuario.controller"
})
public class HtmlExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String recursoNaoEncontrado() {
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    String erroInesperado(Exception exception, Model model) {
        String erroId = UUID.randomUUID().toString();
        LOG.error(
            "Erro interno em navegacao HTML. erroId={}, tipo={}",
            erroId,
            exception.getClass().getName()
        );
        model.addAttribute("erroId", erroId);
        return "error/500";
    }
}
