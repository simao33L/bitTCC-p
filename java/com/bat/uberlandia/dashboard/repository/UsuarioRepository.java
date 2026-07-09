package com.bat.uberlandia.dashboard.repository;

import com.bat.uberlandia.dashboard.model.Usuario;
import com.bat.uberlandia.dashboard.model.Usuario.Tipo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByLogin(String login);
    List<Usuario> findByTipo(Tipo tipo);
    List<Usuario> findBySetorIdAndTipo(Long setorId, Tipo tipo);
}