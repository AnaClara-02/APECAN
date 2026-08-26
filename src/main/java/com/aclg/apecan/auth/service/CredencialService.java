package com.aclg.apecan.auth.service;

import com.aclg.apecan.auth.dto.AlterarSenhaForm;
import com.aclg.apecan.auth.dto.RedefinirSenhaForm;
import com.aclg.apecan.auth.security.SessaoUsuarioService;
import com.aclg.apecan.auth.security.UsuarioAtual;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.shared.exception.RecursoNaoEncontradoException;
import com.aclg.apecan.usuario.entity.FinalidadeTokenCredencial;
import com.aclg.apecan.usuario.entity.HistoricoAdministracaoUsuario;
import com.aclg.apecan.usuario.entity.StatusUsuario;
import com.aclg.apecan.usuario.entity.TipoEventoAdministracaoUsuario;
import com.aclg.apecan.usuario.entity.TokenCredencial;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.HistoricoAdministracaoUsuarioRepository;
import com.aclg.apecan.usuario.repository.TokenCredencialRepository;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Validated
public class CredencialService {

	private static final FinalidadeTokenCredencial FINALIDADE = FinalidadeTokenCredencial.REDEFINICAO_SENHA;

	private final UsuarioRepository usuarioRepository;

	private final TokenCredencialRepository tokenRepository;

	private final HistoricoAdministracaoUsuarioRepository historicoRepository;

	private final GeradorTokenSeguro geradorToken;

	private final PoliticaSenha politicaSenha;

	private final PasswordEncoder passwordEncoder;

	private final EntregaRedefinicaoSenha entrega;

	private final UsuarioAtual usuarioAtual;

	private final SessaoUsuarioService sessaoService;

	private final Clock clock;

	private final Duration validade;

	public CredencialService(UsuarioRepository usuarioRepository, TokenCredencialRepository tokenRepository,
			HistoricoAdministracaoUsuarioRepository historicoRepository, GeradorTokenSeguro geradorToken,
			PoliticaSenha politicaSenha, PasswordEncoder passwordEncoder, EntregaRedefinicaoSenha entrega,
			UsuarioAtual usuarioAtual, SessaoUsuarioService sessaoService, Clock clock,
			@Value("${apecan.redefinicao.validade:1h}") Duration validade) {
		this.usuarioRepository = usuarioRepository;
		this.tokenRepository = tokenRepository;
		this.historicoRepository = historicoRepository;
		this.geradorToken = geradorToken;
		this.politicaSenha = politicaSenha;
		this.passwordEncoder = passwordEncoder;
		this.entrega = entrega;
		this.usuarioAtual = usuarioAtual;
		this.sessaoService = sessaoService;
		this.clock = clock;
		this.validade = validade;
	}

	@Transactional
	public RedefinicaoEmitida solicitar(String email) {
		String normalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
		Usuario usuario = usuarioRepository.findByEmail(normalizado).orElse(null);
		if (usuario == null || usuario.getStatus() != StatusUsuario.ATIVO || !usuario.estaAtivado()) {
			return new RedefinicaoEmitida(null);
		}

		LocalDateTime agora = LocalDateTime.now(clock);
		tokenRepository.findAllByUsuarioAndFinalidadeAndUtilizadoEmIsNull(usuario, FINALIDADE)
			.forEach(token -> token.invalidar(agora));
		tokenRepository.flush();
		String original = geradorToken.gerar();
		tokenRepository.saveAndFlush(new TokenCredencial(usuario, geradorToken.calcularHash(original), FINALIDADE,
				agora, agora.plus(validade)));
		return entrega.entregar(usuario, original);
	}

	@Transactional(readOnly = true)
	public TokenRedefinicaoInfo consultar(String original) {
		return new TokenRedefinicaoInfo(localizar(original, false).getUsuario().getNome());
	}

	@Transactional
	public void redefinir(@Valid RedefinirSenhaForm form) {
		politicaSenha.validar(form.getSenha(), form.getConfirmacaoSenha());
		TokenCredencial token = localizar(form.getToken(), true);
		Usuario usuario = token.getUsuario();
		LocalDateTime agora = LocalDateTime.now(clock);
		usuario.alterarSenha(passwordEncoder.encode(form.getSenha()));
		token.marcarComoUtilizado(agora);
		registrar(usuario, TipoEventoAdministracaoUsuario.REDEFINICAO_SENHA, "Senha redefinida por token de uso unico.",
				agora);
		usuarioRepository.flush();
		sessaoService.encerrarSessoes(usuario.getId());
	}

	@Transactional
	public void alterar(@Valid AlterarSenhaForm form) {
		politicaSenha.validar(form.getNovaSenha(), form.getConfirmacaoSenha());
		Usuario usuario = usuarioRepository.findByIdForUpdate(usuarioAtual.exigirId())
			.orElseThrow(() -> new RecursoNaoEncontradoException("USUARIO_ATUAL_NAO_ENCONTRADO",
					"Usuario autenticado nao encontrado."));
		if (!passwordEncoder.matches(form.getSenhaAtual(), usuario.getSenhaHash())) {
			throw new OperacaoInvalidaException("SENHA_ATUAL_INVALIDA", "A senha atual e invalida.");
		}
		if (passwordEncoder.matches(form.getNovaSenha(), usuario.getSenhaHash())) {
			throw new OperacaoInvalidaException("SENHA_NAO_ALTERADA", "A nova senha deve ser diferente da atual.");
		}
		LocalDateTime agora = LocalDateTime.now(clock);
		usuario.alterarSenha(passwordEncoder.encode(form.getNovaSenha()));
		registrar(usuario, TipoEventoAdministracaoUsuario.ALTERACAO_SENHA, "Senha alterada pelo proprio usuario.",
				agora);
		usuarioRepository.flush();
		sessaoService.encerrarSessoes(usuario.getId());
	}

	private TokenCredencial localizar(String original, boolean bloquear) {
		if (original == null || original.isBlank()) {
			throw tokenInvalido();
		}
		String hash = geradorToken.calcularHash(original);
		TokenCredencial token = (bloquear ? tokenRepository.findForUpdateByTokenHashAndFinalidade(hash, FINALIDADE)
				: tokenRepository.findByTokenHashAndFinalidade(hash, FINALIDADE))
			.orElseThrow(this::tokenInvalido);
		Usuario usuario = token.getUsuario();
		if (token.foiUtilizado() || token.estaExpirado(LocalDateTime.now(clock))
				|| usuario.getStatus() != StatusUsuario.ATIVO || !usuario.estaAtivado()) {
			throw tokenInvalido();
		}
		return token;
	}

	private void registrar(Usuario usuario, TipoEventoAdministracaoUsuario evento, String justificativa,
			LocalDateTime agora) {
		historicoRepository.save(new HistoricoAdministracaoUsuario(usuario, usuario, evento, usuario.getTipoPerfil(),
				usuario.getTipoPerfil(), usuario.getStatus(), usuario.getStatus(), justificativa, agora));
	}

	private OperacaoInvalidaException tokenInvalido() {
		return new OperacaoInvalidaException("TOKEN_REDEFINICAO_INVALIDO",
				"O link de redefinicao e invalido, expirou ou ja foi utilizado.");
	}

}
