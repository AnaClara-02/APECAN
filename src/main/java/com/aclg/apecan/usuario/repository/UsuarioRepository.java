package com.aclg.apecan.usuario.repository;

import com.aclg.apecan.usuario.entity.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByLogin(String login);

	Optional<Usuario> findByEmail(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from Usuario u where u.email = :email")
	Optional<Usuario> findByEmailForUpdate(@Param("email") String email);

	boolean existsByLogin(String login);

	boolean existsByCpf(String cpf);

	boolean existsByEmail(String email);

	boolean existsByLoginAndIdNot(String login, Long id);

	boolean existsByEmailAndIdNot(String email, Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from Usuario u where u.id = :id")
	Optional<Usuario> findByIdForUpdate(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select u from Usuario u
			where u.tipoPerfil in (com.aclg.apecan.usuario.entity.TipoPerfil.ADM_DEV,
				com.aclg.apecan.usuario.entity.TipoPerfil.ADMINISTRADOR)
			order by u.id
			""")
	List<Usuario> findAdministradoresForUpdate();

}
