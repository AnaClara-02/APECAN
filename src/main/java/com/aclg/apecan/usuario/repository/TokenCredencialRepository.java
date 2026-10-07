package com.aclg.apecan.usuario.repository;

import com.aclg.apecan.usuario.entity.FinalidadeTokenCredencial;
import com.aclg.apecan.usuario.entity.TokenCredencial;
import com.aclg.apecan.usuario.entity.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface TokenCredencialRepository extends JpaRepository<TokenCredencial, Long> {
    List<TokenCredencial> findAllByUsuarioAndUtilizadoEmIsNull(Usuario usuario);

    @org.springframework.data.jpa.repository.Query("select t.usuario.id from TokenCredencial t where t.tokenHash = :hash and t.finalidade = :finalidade")
    Optional<Long> findUsuarioId(@org.springframework.data.repository.query.Param("hash") String hash,
        @org.springframework.data.repository.query.Param("finalidade") FinalidadeTokenCredencial finalidade);


    Optional<TokenCredencial> findByTokenHashAndFinalidade(
        String tokenHash,
        FinalidadeTokenCredencial finalidade
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TokenCredencial> findForUpdateByTokenHashAndFinalidade(
        String tokenHash,
        FinalidadeTokenCredencial finalidade
    );

    List<TokenCredencial> findAllByUsuarioAndFinalidadeAndUtilizadoEmIsNull(
        Usuario usuario,
        FinalidadeTokenCredencial finalidade
    );
}
