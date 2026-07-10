package com.bat.uberlandia.dashboard.repository;

import com.bat.uberlandia.dashboard.model.Setor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SetorRepository extends JpaRepository<Setor, Long> {
    Optional<Setor> findByNome(String nome);
}