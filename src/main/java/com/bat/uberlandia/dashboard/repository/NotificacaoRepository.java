package com.bat.uberlandia.dashboard.repository;

import com.bat.uberlandia.dashboard.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findByChamadoIdOrderByDataEnvioDesc(Long chamadoId);
    List<Notificacao> findByUsuarioIdAndLidaFalse(Long usuarioId);
    List<Notificacao> findByLidaFalseOrderByDataEnvioDesc();
    List<Notificacao> findTop10ByOrderByDataEnvioDesc();

    @Modifying
    @Transactional
    @Query("UPDATE Notificacao n SET n.lida = true WHERE n.lida = false")
    int marcarTodasComoLidas();

    @Modifying
    @Transactional
    @Query("UPDATE Notificacao n SET n.lida = true WHERE n.id = :id")
    int marcarComoLida(@Param("id") Long id);
}