package com.aclg.apecan.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		String idParaCodificar = "bcrypt";
		Map<String, PasswordEncoder> codificadores = Map.of(idParaCodificar, new BCryptPasswordEncoder(12));
		return new DelegatingPasswordEncoder(idParaCodificar, codificadores);
	}

	@Bean
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	SecurityFilterChain securityFilterChain(HttpSecurity http, ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
			ApiAccessDeniedHandler apiAccessDeniedHandler, SessionRegistry sessionRegistry) throws Exception {
		RequestMatcher requisicaoApi = request -> {
			String caminho = request.getRequestURI().substring(request.getContextPath().length());
			return caminho.equals("/api") || caminho.startsWith("/api/");
		};

		http.authorizeHttpRequests(autorizacao -> autorizacao
			.requestMatchers("/login", "/ativar-conta", "/esqueci-senha", "/redefinir-senha", "/error", "/error/**",
					"/css/**", "/js/**", "/images/**", "/actuator/health", "/actuator/health/**")
			.permitAll()
			.requestMatchers("/usuarios/**", "/configuracoes/**", "/api/usuarios/**", "/api/configuracoes/**",
					"/actuator/info")
			.hasRole("ADMINISTRADOR")
			.anyRequest()
			.authenticated())
			.formLogin(formulario -> formulario.loginPage("/login")
				.loginProcessingUrl("/login")
				.usernameParameter("login")
				.passwordParameter("senha")
				.defaultSuccessUrl("/inicio", true)
				.failureUrl("/login?erro")
				.permitAll())
			.logout(logout -> logout
				.logoutRequestMatcher(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/logout"))
				.logoutSuccessUrl("/login?logout")
				.invalidateHttpSession(true)
				.clearAuthentication(true)
				.deleteCookies("APECAN_SESSION")
				.permitAll())
			.sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
				.sessionFixation(fixacao -> fixacao.changeSessionId())
				.maximumSessions(-1)
				.sessionRegistry(sessionRegistry))
			.csrf(Customizer.withDefaults())
			.exceptionHandling(
					excecoes -> excecoes.defaultAuthenticationEntryPointFor(apiAuthenticationEntryPoint, requisicaoApi)
						.defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"),
								AnyRequestMatcher.INSTANCE)
						.defaultAccessDeniedHandlerFor(apiAccessDeniedHandler, requisicaoApi))
			.headers(
					headers -> headers
						.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; " + "base-uri 'self'; "
								+ "form-action 'self'; " + "frame-ancestors 'none'; " + "object-src 'none'; "
								+ "img-src 'self' data:; " + "style-src 'self'; " + "script-src 'self'"))
						.frameOptions(frame -> frame.deny())
						.addHeaderWriter(new StaticHeadersWriter("Referrer-Policy", "no-referrer"))
						.addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
								"camera=(), microphone=(), geolocation=()")))
			.httpBasic(AbstractHttpConfigurer::disable)
			.rememberMe(AbstractHttpConfigurer::disable)
			.cors(AbstractHttpConfigurer::disable);

		return http.build();
	}

	@Bean
	SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	static HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

}
