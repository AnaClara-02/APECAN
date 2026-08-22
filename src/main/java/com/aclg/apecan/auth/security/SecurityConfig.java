package com.aclg.apecan.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        String idParaCodificar = "bcrypt";
        Map<String, PasswordEncoder> codificadores = Map.of(
            idParaCodificar,
            new BCryptPasswordEncoder(12)
        );
        return new DelegatingPasswordEncoder(idParaCodificar, codificadores);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
            ApiAccessDeniedHandler apiAccessDeniedHandler) throws Exception {
        RequestMatcher requisicaoApi = request -> {
            String caminho = request.getRequestURI().substring(request.getContextPath().length());
            return caminho.equals("/api") || caminho.startsWith("/api/");
        };

        http
            .authorizeHttpRequests(autorizacao -> autorizacao
                .requestMatchers(
                    "/login",
                    "/error",
                    "/error/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/actuator/health",
                    "/actuator/health/**"
                ).permitAll()
                .requestMatchers(
                    "/usuarios/**",
                    "/configuracoes/**",
                    "/api/usuarios/**",
                    "/api/configuracoes/**",
                    "/actuator/info"
                ).hasRole("ADMINISTRADOR")
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("APECAN_SESSION")
                .permitAll()
            )
            .sessionManagement(sessao -> sessao
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fixacao -> fixacao.changeSessionId())
            )
            .csrf(Customizer.withDefaults())
            .exceptionHandling(excecoes -> excecoes
                .defaultAuthenticationEntryPointFor(
                    apiAuthenticationEntryPoint,
                    requisicaoApi
                )
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    AnyRequestMatcher.INSTANCE
                )
                .defaultAccessDeniedHandlerFor(apiAccessDeniedHandler, requisicaoApi)
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; "
                        + "base-uri 'self'; "
                        + "form-action 'self'; "
                        + "frame-ancestors 'none'; "
                        + "object-src 'none'; "
                        + "img-src 'self' data:; "
                        + "style-src 'self'; "
                        + "script-src 'self'"
                ))
                .frameOptions(frame -> frame.deny())
                .addHeaderWriter(new StaticHeadersWriter(
                    "Referrer-Policy",
                    "strict-origin-when-cross-origin"
                ))
                .addHeaderWriter(new StaticHeadersWriter(
                    "Permissions-Policy",
                    "camera=(), microphone=(), geolocation=()"
                ))
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .rememberMe(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
