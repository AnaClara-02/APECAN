package com.aclg.apecan.auth.security;

import com.aclg.apecan.usuario.entity.StatusUsuario;
import com.aclg.apecan.usuario.entity.TipoPerfil;
import com.aclg.apecan.usuario.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

public final class UsuarioPrincipal implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String login;
    private final String senhaHash;
    private final TipoPerfil tipoPerfil;
    private final boolean ativo;
    private final boolean ativado;

    private UsuarioPrincipal(Long id, String login, String senhaHash,
                             TipoPerfil tipoPerfil, boolean ativo,
                             boolean ativado) {
        this.id = id;
        this.login = login;
        this.senhaHash = senhaHash;
        this.tipoPerfil = tipoPerfil;
        this.ativo = ativo;
        this.ativado = ativado;
    }

    public static UsuarioPrincipal de(Usuario usuario) {
        return new UsuarioPrincipal(
            usuario.getId(),
            usuario.getLogin(),
            usuario.getSenhaHash(),
            usuario.getTipoPerfil(),
            usuario.getStatus() == StatusUsuario.ATIVO,
            usuario.estaAtivado()
        );
    }

    public Long getId() {
        return id;
    }

    public TipoPerfil getTipoPerfil() {
        return tipoPerfil;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + tipoPerfil.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isEnabled() {
        return ativo && ativado;
    }
}
