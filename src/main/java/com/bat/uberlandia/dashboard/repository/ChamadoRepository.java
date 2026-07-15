package com.bat.uberlandia.dashboard.repository;

import com.bat.uberlandia.dashboard.model.Chamado;
import com.bat.uberlandia.dashboard.model.Chamado.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import com.bat.uberlandia.dashboard.model.Chamado.MotivoFalha;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    List<Chamado> findByStatus(Status status);

    long countByStatus(Status status);

    List<Chamado> findByStatusIn(List<Status> statusList);

    List<Chamado> findByStatusAndAlerta30MinEnviadoFalse(Status status);

    List<Chamado> findByMaquinaSetorIdAndDataAberturaBetween(
            Long setorId, LocalDateTime inicio, LocalDateTime fim);

    List<Chamado> findByDataAberturaBetween(
            LocalDateTime inicio, LocalDateTime fim);

    List<Chamado> findByMaquinaId(Long maquinaId);

    List<Chamado> findByMaquinaIdAndStatusIn(Long maquinaId, List<Status> statusList);

    List<Chamado> findByMaquinaSetorId(Long setorId);

    List<Chamado> findTop10ByMaquinaIdAndStatusOrderByDataConclusaoDesc(
            Long maquinaId, Status status);

    @Query("SELECT c FROM Chamado c WHERE c.maquina.setor.id = :setorId " +
           "AND c.status = :status AND c.dataConclusao BETWEEN :inicio AND :fim " +
           "ORDER BY c.dataConclusao ASC")
    List<Chamado> findChamadosConcluidosPorSetorEPeriodo(
            @Param("setorId") Long setorId,
            @Param("status") Status status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("SELECT AVG(c.tempoReparoAcumulado) FROM Chamado c " +
           "WHERE c.status = 'CONCLUIDO'")
    Double findTempoMedioReparoGeral();

    @Query("SELECT AVG(c.tempoReparoAcumulado) FROM Chamado c " +
           "WHERE c.status = 'CONCLUIDO' AND c.maquina.setor.id = :setorId")
    Double findTempoMedioReparoPorSetor(@Param("setorId") Long setorId);

    List<Chamado> findByStatusAndMaquinaSetorId(Status status, Long setorId);

    List<Chamado> findByMaquinaSetorIdAndStatusIn(Long setorId, List<Status> statusList);

    @Query("SELECT c.motivoFalha AS motivo, COUNT(c) AS total " +
           "FROM Chamado c WHERE c.motivoFalha IS NOT NULL " +
           "GROUP BY c.motivoFalha ORDER BY total DESC")
    List<MotivoFalhaCount> countByMotivoFalha();

    @Query("SELECT c.motivoFalha AS motivo, COUNT(c) AS total " +
           "FROM Chamado c WHERE c.motivoFalha IS NOT NULL AND c.maquina.setor.id = :setorId " +
           "GROUP BY c.motivoFalha ORDER BY total DESC")
    List<MotivoFalhaCount> countByMotivoFalhaAndSetor(@Param("setorId") Long setorId);

    @Query("SELECT c.tecnico.id AS id, c.tecnico.nome AS nome, COUNT(c) AS total, " +
           "AVG(c.tempoReparoAcumulado) AS tempoMedioSegundos " +
           "FROM Chamado c WHERE c.status = 'CONCLUIDO' AND c.tecnico IS NOT NULL " +
           "GROUP BY c.tecnico.id, c.tecnico.nome ORDER BY total DESC")
    List<RankingTecnico> findRankingTecnicosConcluidos();

    List<Chamado> findByTecnicoIdAndStatusIn(Long tecnicoId, List<Status> statusList);

    long countByTecnicoIdAndMotivoFalhaAndStatus(
            Long tecnicoId, Chamado.MotivoFalha motivoFalha, Status status);

    @Query("SELECT COUNT(c) FROM Chamado c WHERE c.status = :status AND c.dataAbertura >= :inicio")
    long countByStatusAndDataAberturaAfter(
            @Param("status") Status status, @Param("inicio") LocalDateTime inicio);

    @Query("SELECT c.dataAbertura FROM Chamado c " +
           "WHERE c.status = 'CONCLUIDO' AND c.dataConclusao IS NOT NULL " +
           "AND c.dataAbertura >= :inicio ORDER BY c.dataConclusao ASC")
    List<LocalDateTime> findDatasConclusaoDesde(@Param("inicio") LocalDateTime inicio);

    @Query("SELECT c FROM Chamado c WHERE c.maquina.id = :maquinaId ORDER BY c.dataAbertura DESC")
    List<Chamado> findByMaquinaIdOrderByDataAberturaDesc(@Param("maquinaId") Long maquinaId);

    @Query("SELECT SUM(c.tempoReparoAcumulado) FROM Chamado c " +
           "WHERE c.maquina.id = :maquinaId AND c.status = 'CONCLUIDO'")
    Long sumTempoReparoByMaquinaId(@Param("maquinaId") Long maquinaId);

    @Query("SELECT DATE(c.dataConclusao) AS dia, COUNT(c) AS total FROM Chamado c " +
           "WHERE c.status = 'CONCLUIDO' AND c.dataConclusao IS NOT NULL " +
           "AND c.dataConclusao >= :inicio GROUP BY DATE(c.dataConclusao) ORDER BY dia ASC")
    List<TendenciaDiaria> findTendenciaDiaria(@Param("inicio") LocalDateTime inicio);

    @Query("SELECT DATE(c.dataConclusao) AS dia, c.motivoFalha AS motivo, COUNT(c) AS total " +
           "FROM Chamado c WHERE c.status = 'CONCLUIDO' AND c.dataConclusao IS NOT NULL " +
           "AND c.dataConclusao >= :inicio " +
           "GROUP BY DATE(c.dataConclusao), c.motivoFalha ORDER BY dia ASC")
    List<TendenciaMotivoDiaria> findTendenciaPorMotivo(@Param("inicio") LocalDateTime inicio);
}