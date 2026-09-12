package com.aclg.apecan.backup.service;

import com.aclg.apecan.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

@Service
public class RecuperacaoDisponibilidadeService {

	private final UsuarioRepository usuarioRepository;
	private final boolean modoRecuperacao;

	public RecuperacaoDisponibilidadeService(UsuarioRepository usuarioRepository,
			@Value("${apecan.recovery.enabled:false}") boolean modoRecuperacao) {
		this.usuarioRepository = usuarioRepository;
		this.modoRecuperacao = modoRecuperacao;
	}

	public boolean permitida(HttpServletRequest request) {
		return requisicaoLocal(request) && (modoRecuperacao || usuarioRepository.count() == 0);
	}

	public boolean modoRecuperacao() {
		return modoRecuperacao;
	}

	private boolean requisicaoLocal(HttpServletRequest request) {
		try {
			return InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress();
		}
		catch (Exception exception) {
			return false;
		}
	}
}
