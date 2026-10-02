package com.aclg.apecan.auth.security;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class RenderSegurancaValidatorTests {
    @Test void validaConfiguracaoAntesDaCriacaoDosServicos() {
        var runner = new org.springframework.boot.test.context.runner.ApplicationContextRunner()
            .withPropertyValues("spring.profiles.active=prod,render")
            .withUserConfiguration(RenderSegurancaValidator.class)
            .withPropertyValues("apecan.mail.transporte=brevo");
        runner.withPropertyValues("spring.datasource.url=jdbc:postgresql://ep-test.neon.tech/apecan?sslmode=verify-full&sslrootcert=/ca")
            .run(context -> assertThat(context).hasNotFailed());
        runner.withPropertyValues("spring.datasource.url=jdbc:postgresql://ep-test.neon.tech/apecan?sslmode=disable")
            .run(context -> assertThat(context).hasFailed());
    }
    @Test void exigeConexaoDiretaEValidacaoCompleta() {
        assertThatCode(() -> RenderSegurancaValidator.validarUrl(
            "jdbc:postgresql://ep-test.neon.tech/apecan?sslmode=verify-full&sslrootcert=/etc/ssl/certs/ca-certificates.crt"))
            .doesNotThrowAnyException();
        for (String query : new String[]{"sslmode=require","sslmode=verify-full",
                "sslmode=verify-full&sslrootcert=/ca&sslmode=disable",
                "sslmode=verify-full&sslrootcert=/ca&sslfactory=nao.validar",
                "sslmode=verify-full&sslrootcert=/ca&sslhostnameverifier=nao.validar",
                "sslmode=verify-full&sslrootcert=/ca&password=segredo"}) {
            assertThatThrownBy(() -> RenderSegurancaValidator.validarUrl("jdbc:postgresql://ep-test.neon.tech/apecan?"+query))
                .isInstanceOf(IllegalStateException.class).hasMessageNotContaining("segredo");
        }
    }
    @Test void recusaPerfisPerigososAntesDosRunners() {
        for (String proibido : new String[]{"bootstrap-admin","recovery"}) {
            var env=new MockEnvironment(); env.setActiveProfiles("prod","render",proibido);
            assertThatThrownBy(() -> new RenderSegurancaValidator(env).afterPropertiesSet())
                .hasMessageContaining("proibe");
        }
    }
}
