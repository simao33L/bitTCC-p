package com.bat.uberlandia.dashboard.service;

import com.bat.uberlandia.dashboard.model.*;
import com.bat.uberlandia.dashboard.model.Chamado.Status;
import com.bat.uberlandia.dashboard.repository.*;
import com.bat.uberlandia.dashboard.repository.MotivoFalhaCount;
import com.bat.uberlandia.dashboard.repository.RankingTecnico;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final NotificacaoRepository notificacaoRepository;

    @Transactional
    public Chamado abrirChamado(Chamado chamado, Maquina maquina) {
        chamado.setMaquina(maquina);
        chamado.setStatus(Status.ABERTO);
        chamado.setDataAbertura(LocalDateTime.now());
        return chamadoRepository.save(chamado);
    }

    @Transactional
    public Chamado iniciarAtendimento(Long chamadoId, Usuario tecnico) {
        Chamado c = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new RuntimeException("Chamado nao encontrado"));
        c.setTecnico(tecnico);
        c.setStatus(Status.EM_ANDAMENTO);
        c.setInicioReparo(LocalDateTime.now());
        c.setTempoReparoAcumulado(0L);
        c.setAlerta30MinEnviado(false);
        return chamadoRepository.save(c);
    }

    @Transactional
    public Chamado iniciarLocomocao(Long chamadoId) {
        Chamado c = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new RuntimeException("Chamado nao encontrado"));
        c.setInicioLocomocao(LocalDateTime.now());
        return chamadoRepository.save(c);
    }

    @Transactional
    public Chamado finalizarLocomocao(Long chamadoId) {
        Chamado c = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new RuntimeException("Chamado nao encontrado"));
        if (c.getInicioLocomocao() != null) {
            long segundos = Duration.between(c.getInicioLocomocao(), LocalDateTime.now()).getSeconds();
            c.setTempoLocomocaoSegundos(segundos);
            c.setFimLocomocao(LocalDateTime.now());
        }
        return chamadoRepository.save(c);
    }

    @Transactional
    public Chamado pausarReparo(Long chamadoId) {
        Chamado c = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new RuntimeException("Chamado nao encontrado"));

        long corrido = Duration.between(c.getInicioReparo(), LocalDateTime.now()).getSeconds();
        long anterior = c.getTempoReparoAcumulado() != null ? c.getTempoReparoAcumulado() : 0L;
        c.setTempoReparoAcumulado(anterior + corrido);

        c.setInicioPausa(LocalDateTime.now());
        c.setStatus(Status.PAUSADO);
        return chamadoRepository.save(c);
    }

    @Transactional
    public Chamado retomarReparo(Long chamadoId) {
        Chamado c = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new RuntimeException("Chamado nao encontrado"));
        c.setInicioReparo(LocalDateTime.now());
        c.setInicioPausa(null);
        c.setStatus(Status.EM_ANDAMENTO);
        return chamadoRepository.save(c);
    }

    @Transactional
    public Chamado escalarParaEspecialista(Long chamadoId, Usuario especialista) {
        Chamado c = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new RuntimeException("Chamado nao encontrado"));

        long corrido = Duration.between(c.getInicioReparo(), LocalDateTime.now()).getSeconds();
        long anterior = c.getTempoReparoAcumulado() != null ? c.getTempoReparoAcumulado() : 0L;
        c.setTempoReparoAcumulado(anterior + corrido);

        c.setInicioPausa(LocalDateTime.now());
        c.setEspecialista(especialista);
        c.setDataEscalacao(LocalDateTime.now());
        c.setStatus(Status.ESCALADO);

        notificacaoRepository.save(Notificacao.builder()
                .tipo(Notificacao.TipoNotificacao.ESCALACAO)
                .mensagem("Chamado #" + c.getId() + " escalado para voce. Maquina: " + c.getMaquina().getNome() + ". Titulo: " + c.getTitulo())
                .dataEnvio(LocalDateTime.now())
                .chamado(c)
                .usuario(especialista)
                .build());

        return chamadoRepository.save(c);
    }

    @Transactional
    public Chamado concluirChamado(Long chamadoId) {
        Chamado c = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new RuntimeException("Chamado nao encontrado"));

        long corrido = Duration.between(c.getInicioReparo(), LocalDateTime.now()).getSeconds();
        long anterior = c.getTempoReparoAcumulado() != null ? c.getTempoReparoAcumulado() : 0L;
        c.setTempoReparoAcumulado(anterior + corrido);

        c.setDataConclusao(LocalDateTime.now());
        c.setStatus(Status.CONCLUIDO);

        if (c.getTecnico() != null) {
            notificacaoRepository.save(Notificacao.builder()
                    .tipo(Notificacao.TipoNotificacao.CONCLUSAO)
                    .mensagem("Chamado #" + c.getId() + " concluido. Tempo total: " + (c.getTempoReparoAcumulado() / 60) + " min")
                    .dataEnvio(LocalDateTime.now())
                    .chamado(c)
                    .usuario(c.getTecnico())
                    .build());
        }

        return chamadoRepository.save(c);
    }

    public double calcularMtbfPorMaquina(Long maquinaId) {
        List<Chamado> concluidos = chamadoRepository
                .findTop10ByMaquinaIdAndStatusOrderByDataConclusaoDesc(maquinaId, Status.CONCLUIDO);

        if (concluidos.size() < 2) return 0.0;

        long somaIntervalos = 0;
        for (int i = 0; i < concluidos.size() - 1; i++) {
            LocalDateTime falhaAnterior = concluidos.get(i + 1).getDataConclusao();
            LocalDateTime falhaAtual = concluidos.get(i).getDataConclusao();
            somaIntervalos += Duration.between(falhaAnterior, falhaAtual).getSeconds();
        }
        double mediaSegundos = (double) somaIntervalos / (concluidos.size() - 1);
        return mediaSegundos / 3600.0;
    }

    public double calcularMtbfPorSetor(Long setorId) {
        LocalDateTime fim = LocalDateTime.now();
        LocalDateTime inicio = fim.minusDays(30);
        List<Chamado> concluidos = chamadoRepository
                .findChamadosConcluidosPorSetorEPeriodo(setorId, Status.CONCLUIDO, inicio, fim);

        if (concluidos.size() < 2) return 0.0;

        long somaIntervalos = 0;
        for (int i = 1; i < concluidos.size(); i++) {
            somaIntervalos += Duration.between(
                    concluidos.get(i - 1).getDataConclusao(),
                    concluidos.get(i).getDataConclusao()
            ).getSeconds();
        }
        double mediaSegundos = (double) somaIntervalos / (concluidos.size() - 1);
        return mediaSegundos / 3600.0;
    }

    public double getTempoMedioReparoGeral() {
        Double media = chamadoRepository.findTempoMedioReparoGeral();
        return (media != null) ? media / 60.0 : 0.0;
    }

    public double getTempoMedioReparoPorSetor(Long setorId) {
        Double media = chamadoRepository.findTempoMedioReparoPorSetor(setorId);
        return (media != null) ? media / 60.0 : 0.0;
    }

    public Map<Chamado.MotivoFalha, Long> getDistribuicaoMotivosFalha(Long setorId) {
        List<MotivoFalhaCount> contagens = (setorId != null)
                ? chamadoRepository.countByMotivoFalhaAndSetor(setorId)
                : chamadoRepository.countByMotivoFalha();

        Map<Chamado.MotivoFalha, Long> resultado = new LinkedHashMap<>();
        for (Chamado.MotivoFalha m : Chamado.MotivoFalha.values()) {
            resultado.put(m, 0L);
        }
        for (MotivoFalhaCount c : contagens) {
            resultado.put(c.getMotivo(), c.getTotal());
        }
        return resultado;
    }

    public List<RankingTecnico> getRankingTecnicos() {
        return chamadoRepository.findRankingTecnicosConcluidos();
    }
}
