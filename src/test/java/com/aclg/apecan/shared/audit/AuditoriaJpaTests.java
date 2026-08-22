package com.aclg.apecan.shared.audit;

import com.aclg.apecan.shared.config.AuditoriaConfig;
import com.aclg.apecan.usuario.entity.TipoPerfil;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Import(AuditoriaJpaTests.ConfiguracaoDeRelogio.class)
class AuditoriaJpaTests {

    private static final Instant INSTANTE_INICIAL = Instant.parse("2026-08-22T12:00:00Z");
    private static final Instant INSTANTE_ATUALIZACAO = Instant.parse("2026-08-22T13:00:00Z");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RelogioMutavel relogio;

    @Test
    void deveRegistrarCriacaoEAtualizacaoComRelogioControlado() {
        Usuario usuario = new Usuario(
            "Usuário de Teste",
            "usuario.teste",
            "52998224725",
            "teste@apecan.org.br",
            "sem-foto.png",
            "14999999999",
            TipoPerfil.USUARIO,
            null
        );
        usuario.definirSenhaDefinitiva(
            "{bcrypt}hash-de-teste",
            LocalDateTime.ofInstant(
                INSTANTE_INICIAL,
                AuditoriaConfig.FUSO_HORARIO_APECAN
            )
        );

        usuarioRepository.saveAndFlush(usuario);

        LocalDateTime criacaoEsperada = LocalDateTime.ofInstant(
            INSTANTE_INICIAL,
            AuditoriaConfig.FUSO_HORARIO_APECAN
        );
        assertThat(usuario.getCriadoEm()).isEqualTo(criacaoEsperada);
        assertThat(usuario.getAtualizadoEm()).isNull();

        relogio.avancarPara(INSTANTE_ATUALIZACAO);
        usuario.atualizarDadosPessoais(
            "Usuário Atualizado",
            "usuario.teste",
            "atualizado@apecan.org.br",
            "sem-foto.png",
            "14988888888"
        );
        usuarioRepository.saveAndFlush(usuario);

        LocalDateTime atualizacaoEsperada = LocalDateTime.ofInstant(
            INSTANTE_ATUALIZACAO,
            AuditoriaConfig.FUSO_HORARIO_APECAN
        );
        assertThat(usuario.getCriadoEm()).isEqualTo(criacaoEsperada);
        assertThat(usuario.getAtualizadoEm()).isEqualTo(atualizacaoEsperada);
    }

    @TestConfiguration
    static class ConfiguracaoDeRelogio {

        @Bean
        @Primary
        RelogioMutavel relogioMutavel() {
            return new RelogioMutavel(
                INSTANTE_INICIAL,
                AuditoriaConfig.FUSO_HORARIO_APECAN
            );
        }
    }
}
