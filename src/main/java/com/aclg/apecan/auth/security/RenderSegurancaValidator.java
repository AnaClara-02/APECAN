package com.aclg.apecan.auth.security;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("render")
public class RenderSegurancaValidator implements org.springframework.beans.factory.config.BeanFactoryPostProcessor,
        org.springframework.context.EnvironmentAware {
    private Environment env;
    public RenderSegurancaValidator() {}
    public RenderSegurancaValidator(Environment env) { this.env = env; }
    @Override
    public void setEnvironment(Environment env) { this.env = env; }
    @Override
    public void postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory factory) {
        afterPropertiesSet();
    }
    public void afterPropertiesSet() {
        if (!env.acceptsProfiles(Profiles.of("prod")) ||
                env.acceptsProfiles(Profiles.of("bootstrap-admin", "recovery")) ||
                env.getProperty("apecan.recovery.enabled", Boolean.class, false)) {
            throw new IllegalStateException("Render exige prod e proibe bootstrap/recuperacao.");
        }
        validarUrl(env.getRequiredProperty("spring.datasource.url"));
        if (!"brevo".equals(env.getProperty("apecan.mail.transporte"))) {
            throw new IllegalStateException("Render exige a API HTTPS do Brevo.");
        }
    }
    static void validarUrl(String url) {
        try {
            if (!url.startsWith("jdbc:postgresql://")) throw new IllegalArgumentException();
            URI uri = URI.create(url.substring(5));
            if (uri.getHost()==null || uri.getHost().contains("-pooler.") ||
                    uri.getUserInfo()!=null || uri.getFragment()!=null) throw new IllegalArgumentException();
            Map<String,String> params = new HashMap<>();
            if (uri.getRawQuery()!=null) for (String item : uri.getRawQuery().split("&")) {
                String[] par=item.split("=",2);
                if (par.length!=2) throw new IllegalArgumentException();
                String nome=URLDecoder.decode(par[0],StandardCharsets.UTF_8);
                if (params.putIfAbsent(nome,URLDecoder.decode(par[1],StandardCharsets.UTF_8))!=null)
                    throw new IllegalArgumentException();
            }
            if (!"verify-full".equals(params.get("sslmode")) ||
                    params.getOrDefault("sslrootcert","").isBlank() ||
                    params.containsKey("sslfactory") || params.containsKey("sslhostnameverifier") ||
                    params.containsKey("user") || params.containsKey("password")) throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Use JDBC direto, verify-full, CA explicita e credenciais separadas.");
        }
    }
}
