package com.aclg.apecan.usuario.mapper;

import com.aclg.apecan.shared.validation.CpfFormatter;
import com.aclg.apecan.shared.validation.TelefoneFormatter;
import com.aclg.apecan.usuario.dto.EditarUsuarioForm;
import com.aclg.apecan.usuario.dto.UsuarioResumoDto;
import com.aclg.apecan.usuario.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioResumoDto paraResumo(Usuario usuario) {
        return new UsuarioResumoDto(
            usuario.getId(),
            usuario.getNome(),
            usuario.getLogin(),
            CpfFormatter.formatar(usuario.getCpf()),
            usuario.getEmail(),
            TelefoneFormatter.formatar(usuario.getTelefone()),
            usuario.getTipoPerfil(),
            usuario.getStatus(),
            usuario.isPrimeiroAcessoPendente()
        );
    }

    public EditarUsuarioForm paraFormularioEdicao(Usuario usuario) {
        EditarUsuarioForm formulario = new EditarUsuarioForm();
        formulario.setNome(usuario.getNome());
        formulario.setLogin(usuario.getLogin());
        formulario.setEmail(usuario.getEmail());
        formulario.setTelefone(TelefoneFormatter.formatar(usuario.getTelefone()));
        return formulario;
    }
}
