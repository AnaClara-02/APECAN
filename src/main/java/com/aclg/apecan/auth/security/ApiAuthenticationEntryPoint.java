package com.aclg.apecan.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiSecurityErrorWriter errorWriter;

    public ApiAuthenticationEntryPoint(ApiSecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception)
            throws IOException, ServletException {
        errorWriter.escrever(
            request,
            response,
            HttpStatus.UNAUTHORIZED,
            "NAO_AUTENTICADO",
            "Autenticação necessária",
            "É necessário entrar no sistema para acessar este recurso."
        );
    }
}
