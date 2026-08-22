package com.aclg.apecan.usuario.repository;

import com.aclg.apecan.usuario.entity.HistoricoAdministracaoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoAdministracaoUsuarioRepository
        extends JpaRepository<HistoricoAdministracaoUsuario, Long> {

    List<HistoricoAdministracaoUsuario> findAllByUsuarioIdOrderByOcorridoEmDesc(Long usuarioId);
}
