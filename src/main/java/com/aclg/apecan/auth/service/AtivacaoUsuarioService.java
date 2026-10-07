package com.aclg.apecan.auth.service;

import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.usuario.entity.FinalidadeTokenCredencial;
import com.aclg.apecan.usuario.entity.HistoricoAdministracaoUsuario;
import com.aclg.apecan.usuario.entity.StatusUsuario;
import com.aclg.apecan.usuario.entity.TipoEventoAdministracaoUsuario;
import com.aclg.apecan.usuario.entity.TokenCredencial;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.HistoricoAdministracaoUsuarioRepository;
import com.aclg.apecan.usuario.repository.TokenCredencialRepository;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AtivacaoUsuarioService {

    private static final FinalidadeTokenCredencial FINALIDADE =
        FinalidadeTokenCredencial.ATIVACAO;

    private final TokenCredencialRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoricoAdministracaoUsuarioRepository historicoRepository;
    private final GeradorTokenSeguro geradorToken;
    private final PoliticaSenha politicaSenha;
    private final PasswordEncoder passwordEncoder;
    private final EntregaAtivacao entregaAtivacao;
    private final Clock clock;
    private final Duration validade;

    public AtivacaoUsuarioService(
            TokenCredencialRepository tokenRepository,
            UsuarioRepository usuarioRepository,
            HistoricoAdministracaoUsuarioRepository historicoRepository,
            GeradorTokenSeguro geradorToken,
            PoliticaSenha politicaSenha,
            PasswordEncoder passwordEncoder,
            EntregaAtivacao entregaAtivacao,
            Clock clock,
            @Value("${apecan.ativacao.validade:24h}") Duration validade) {
        this.tokenRepository = tokenRepository;
        this.usuarioRepository = usuarioRepository;
        this.historicoRepository = historicoRepository;
        this.geradorToken = geradorToken;
        this.politicaSenha = politicaSenha;
        this.passwordEncoder = passwordEncoder;
        this.entregaAtivacao = entregaAtivacao;
        this.clock = clock;
        this.validade = validade;
    }

    @Transactional
    public AtivacaoEmitida emitir(Usuario usuario) {
        usuario = usuarioRepository.findByIdForUpdate(usuario.getId()).orElseThrow(this::tokenInvalido);
        if (usuario.getStatus() != StatusUsuario.ATIVO
                || !usuario.isPrimeiroAcessoPendente()) {
            throw new OperacaoInvalidaException(
                "USUARIO_NAO_AGUARDA_ATIVACAO",
                "A conta informada nao esta aguardando ativacao."
            );
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        tokenRepository
            .findAllByUsuarioAndFinalidadeAndUtilizadoEmIsNull(usuario, FINALIDADE)
            .forEach(token -> token.invalidar(agora));
        tokenRepository.flush();

        String tokenOriginal = geradorToken.gerar();
        LocalDateTime expiraEm = agora.plus(validade);
        TokenCredencial token = new TokenCredencial(
            usuario,
            geradorToken.calcularHash(tokenOriginal),
            FINALIDADE,
            agora,
            expiraEm
        );
        tokenRepository.saveAndFlush(token);

        return entregaAtivacao.entregar(usuario, tokenOriginal, expiraEm);
    }

    @Transactional(readOnly = true)
    public TokenAtivacaoInfo consultar(String tokenOriginal) {
        TokenCredencial token = localizarValido(tokenOriginal, false);
        return new TokenAtivacaoInfo(
            token.getUsuario().getNome(),
            token.getExpiraEm()
        );
    }

    @Transactional
    public void ativar(String tokenOriginal, String senha, String confirmacaoSenha) {
        politicaSenha.validar(senha, confirmacaoSenha);
        TokenCredencial token = localizarValido(tokenOriginal, true);
        Usuario usuario = token.getUsuario();

        usuario.definirSenhaDefinitiva(
            passwordEncoder.encode(senha),
            LocalDateTime.now(clock)
        );
        token.marcarComoUtilizado(LocalDateTime.now(clock));

        historicoRepository.save(new HistoricoAdministracaoUsuario(
            usuario,
            usuario,
            TipoEventoAdministracaoUsuario.ATIVACAO,
            usuario.getTipoPerfil(),
            usuario.getTipoPerfil(),
            usuario.getStatus(),
            usuario.getStatus(),
            "Ativacao da propria conta.",
            LocalDateTime.now(clock)
        ));
    }

    private TokenCredencial localizarValido(String tokenOriginal, boolean bloquear) {
        if (tokenOriginal == null || tokenOriginal.isBlank()) {
            throw tokenInvalido();
        }

        String hash = geradorToken.calcularHash(tokenOriginal);
        if (bloquear) {
            Long usuarioId = tokenRepository.findUsuarioId(hash, FINALIDADE).orElseThrow(this::tokenInvalido);
            // Mesma ordem usada na emissão e nas mudanças administrativas.
            usuarioRepository.findByIdForUpdate(usuarioId).orElseThrow(this::tokenInvalido);
        }
        TokenCredencial token = (bloquear
            ? tokenRepository.findForUpdateByTokenHashAndFinalidade(hash, FINALIDADE)
            : tokenRepository.findByTokenHashAndFinalidade(hash, FINALIDADE))
            .orElseThrow(this::tokenInvalido);

        Usuario usuario = token.getUsuario();
        if (token.foiUtilizado()
                || token.estaExpirado(LocalDateTime.now(clock))
                || usuario.getStatus() != StatusUsuario.ATIVO
                || !usuario.isPrimeiroAcessoPendente()) {
            throw tokenInvalido();
        }
        return token;
    }

    private OperacaoInvalidaException tokenInvalido() {
        return new OperacaoInvalidaException(
            "TOKEN_ATIVACAO_INVALIDO",
            "O link de ativacao e invalido, expirou ou ja foi utilizado."
        );
    }
}
