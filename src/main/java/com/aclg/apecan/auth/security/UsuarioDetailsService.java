package com.aclg.apecan.auth.security;

import com.aclg.apecan.usuario.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) {
        String loginNormalizado = login == null ? "" : login.trim().toLowerCase();
        return usuarioRepository.findByLogin(loginNormalizado)
            .map(UsuarioPrincipal::de)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário ou senha inválidos."));
    }
}
