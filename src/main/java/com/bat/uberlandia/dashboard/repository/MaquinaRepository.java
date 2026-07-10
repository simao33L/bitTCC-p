package com.bat.uberlandia.dashboard.repository;

import com.bat.uberlandia.dashboard.model.Maquina;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MaquinaRepository extends JpaRepository<Maquina, Long> {
    List<Maquina> findBySetorId(Long setorId);
    Optional<Maquina> findByNome(String nome);
}