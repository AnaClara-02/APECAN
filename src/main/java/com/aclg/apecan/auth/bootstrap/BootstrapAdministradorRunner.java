package com.aclg.apecan.auth.bootstrap;

import com.aclg.apecan.usuario.dto.NovoUsuarioForm;
import com.aclg.apecan.usuario.dto.UsuarioCriadoResultado;
import com.aclg.apecan.usuario.service.UsuarioService;
import jakarta.validation.ConstraintViolationException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@Component
@Profile("bootstrap-admin")
public class BootstrapAdministradorRunner implements ApplicationRunner {

    private final UsuarioService usuarioService;
    private final ConfigurableApplicationContext applicationContext;

    public BootstrapAdministradorRunner(
            UsuarioService usuarioService,
            ConfigurableApplicationContext applicationContext) {
        this.usuarioService = usuarioService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        int codigoSaida = 0;
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            if (args.containsOption("reenviar-ativacao-inicial")) {
                var ativacao = usuarioService.reemitirPrimeiraAtivacao();
                System.out.println(ativacao.mensagem());
                if (ativacao.linkLocal() != null) System.out.println(ativacao.linkLocal());
                return;
            }
            System.out.println();
            System.out.println("=== Configuracao inicial do APECAN ===");
            System.out.println("Informe os dados do primeiro administrador.");

            NovoUsuarioForm formulario = new NovoUsuarioForm();
            formulario.setNome(ler(scanner, "Nome: "));
            formulario.setLogin(ler(scanner, "Login: "));
            formulario.setCpf(ler(scanner, "CPF: "));
            formulario.setEmail(ler(scanner, "E-mail: "));
            formulario.setTelefone(ler(scanner, "Telefone: "));

            UsuarioCriadoResultado resultado =
                usuarioService.cadastrarPrimeiroAdministrador(formulario);

            System.out.println();
            System.out.println("Administrador pendente criado com sucesso.");
            if (resultado.ativacao().linkLocal() != null) {
                System.out.println("Entregue o link abaixo ao administrador. Ele e exibido uma unica vez:");
                System.out.println(resultado.ativacao().linkLocal());
            } else {
                System.out.println(resultado.ativacao().mensagem());
            }
            System.out.println("Valido ate: " + resultado.ativacao().expiraEm());
        } catch (ConstraintViolationException exception) {
            codigoSaida = 1;
            System.err.println("Dados invalidos:");
            exception.getConstraintViolations().forEach(violacao ->
                System.err.println("- " + violacao.getMessage())
            );
        } catch (RuntimeException exception) {
            codigoSaida = 1;
            System.err.println("Nao foi possivel concluir a configuracao: " + exception.getMessage());
        } finally {
            int resultado = codigoSaida;
            SpringApplication.exit(applicationContext, () -> resultado);
        }
    }

    private String ler(Scanner scanner, String rotulo) {
        System.out.print(rotulo);
        if (!scanner.hasNextLine()) {
            throw new IllegalStateException("O terminal nao forneceu todos os dados.");
        }
        return scanner.nextLine();
    }
}
