package com.aclg.apecan.shared.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<Object> tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException exception, WebRequest request) {
        return resposta(
            exception.getCodigo(),
            "Recurso não encontrado",
            exception.getMessage(),
            HttpStatus.NOT_FOUND,
            request,
            null
        );
    }

    @ExceptionHandler(ConflitoNegocioException.class)
    ResponseEntity<Object> tratarConflito(
            ConflitoNegocioException exception, WebRequest request) {
        return resposta(
            exception.getCodigo(),
            "Conflito de dados",
            exception.getMessage(),
            HttpStatus.CONFLICT,
            request,
            null
        );
    }

    @ExceptionHandler(OperacaoInvalidaException.class)
    ResponseEntity<Object> tratarOperacaoInvalida(
            OperacaoInvalidaException exception, WebRequest request) {
        return resposta(
            exception.getCodigo(),
            "Operação inválida",
            exception.getMessage(),
            HttpStatusCode.valueOf(422),
            request,
            null
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> tratarRestricoes(
            ConstraintViolationException exception, WebRequest request) {
        List<ErroCampo> campos = exception.getConstraintViolations().stream()
            .map(violacao -> new ErroCampo(
                violacao.getPropertyPath().toString(),
                violacao.getMessage()
            ))
            .toList();

        return resposta(
            "DADOS_INVALIDOS",
            "Dados inválidos",
            "Verifique os dados informados.",
            HttpStatus.BAD_REQUEST,
            request,
            campos
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> tratarIntegridadeDeDados(
            DataIntegrityViolationException exception, WebRequest request) {
        return resposta(
            "CONFLITO_DE_DADOS",
            "Conflito de dados",
            "A operação entra em conflito com dados já cadastrados.",
            HttpStatus.CONFLICT,
            request,
            null
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> tratarErroInesperado(Exception exception, WebRequest request) {
        String erroId = UUID.randomUUID().toString();
        LOG.error(
            "Erro interno não tratado. erroId={}, tipo={}",
            erroId,
            exception.getClass().getName()
        );
        return resposta(
            "ERRO_INTERNO",
            "Erro interno",
            "Não foi possível concluir a operação.",
            HttpStatus.INTERNAL_SERVER_ERROR,
            request,
            null,
            erroId
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ErroCampo> campos = exception.getBindingResult().getFieldErrors().stream()
            .map(this::paraErroCampo)
            .toList();

        return resposta(
            "DADOS_INVALIDOS",
            "Dados inválidos",
            "Verifique os dados informados.",
            HttpStatus.BAD_REQUEST,
            request,
            campos
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return resposta(
            "REQUISICAO_INVALIDA",
            "Requisição inválida",
            "O conteúdo enviado não pôde ser interpretado.",
            HttpStatus.BAD_REQUEST,
            request,
            null
        );
    }

    private ErroCampo paraErroCampo(FieldError erro) {
        String mensagem = erro.getDefaultMessage() == null
            ? "Valor inválido"
            : erro.getDefaultMessage();
        return new ErroCampo(erro.getField(), mensagem);
    }

    private ResponseEntity<Object> resposta(
            String codigo,
            String titulo,
            String detalhe,
            HttpStatusCode status,
            WebRequest request,
            List<ErroCampo> campos) {
        return resposta(
            codigo,
            titulo,
            detalhe,
            status,
            request,
            campos,
            UUID.randomUUID().toString()
        );
    }

    private ResponseEntity<Object> resposta(
            String codigo,
            String titulo,
            String detalhe,
            HttpStatusCode status,
            WebRequest request,
            List<ErroCampo> campos,
            String erroId) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setType(URI.create("urn:apecan:erro:" + codigo));
        problema.setInstance(instancia(request));
        problema.setProperty("codigo", codigo);
        problema.setProperty("erroId", erroId);
        if (campos != null && !campos.isEmpty()) {
            problema.setProperty("campos", campos);
        }

        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problema);
    }

    private URI instancia(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return URI.create(servletWebRequest.getRequest().getRequestURI());
        }
        return URI.create("/");
    }
}
