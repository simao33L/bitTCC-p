package com.bat.uberlandia.dashboard.service;

import com.bat.uberlandia.dashboard.model.*;
import com.bat.uberlandia.dashboard.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertaService {

    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoRepository notificacaoRepository;

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void verificarChamadosAtrasados() {
        List<Chamado> emAndamento = chamadoRepository
                .findByStatusAndAlerta30MinEnviadoFalse(Chamado.Status.EM_ANDAMENTO);

        for (Chamado c : emAndamento) {
            if (c.isAtrasado()) {
                enviarAlerta30Min(c);
            }
        }

        List<Chamado> escalados = chamadoRepository
                .findByStatusAndAlerta30MinEnviadoFalse(Chamado.Status.ESCALADO);

        for (Chamado c : escalados) {
            if (c.isAtrasado()) {
                enviarAlerta30MinEscalado(c);
            }
        }
    }

    private void enviarAlerta30Min(Chamado chamado) {
        List<Usuario> lideres = usuarioRepository
                .findBySetorIdAndTipo(chamado.getMaquina().getSetor().getId(), Usuario.Tipo.LIDER);

        for (Usuario lider : lideres) {
            notificacaoRepository.save(Notificacao.builder()
                    .tipo(Notificacao.TipoNotificacao.ALERTA_30MIN)
                    .mensagem("Chamado #" + chamado.getId() + " (" + chamado.getTitulo() + ") ultrapassou 30 minutos de reparo na maquina " + chamado.getMaquina().getNome())
                    .dataEnvio(LocalDateTime.now())
                    .chamado(chamado)
                    .usuario(lider)
                    .build());
        }

        List<Usuario> especialistas = usuarioRepository.findByTipo(Usuario.Tipo.ESPECIALISTA);
        for (Usuario esp : especialistas) {
            notificacaoRepository.save(Notificacao.builder()
                    .tipo(Notificacao.TipoNotificacao.ALERTA_30MIN)
                    .mensagem("Alerta: Chamado #" + chamado.getId() + " excedeu 30 min. Setor: " + chamado.getMaquina().getSetor().getNome())
                    .dataEnvio(LocalDateTime.now())
                    .chamado(chamado)
                    .usuario(esp)
                    .build());
        }

        chamado.setAlerta30MinEnviado(true);
        chamadoRepository.save(chamado);
    }

    private void enviarAlerta30MinEscalado(Chamado chamado) {
        List<Usuario> lideres = usuarioRepository
                .findBySetorIdAndTipo(chamado.getMaquina().getSetor().getId(), Usuario.Tipo.LIDER);

        for (Usuario lider : lideres) {
            notificacaoRepository.save(Notificacao.builder()
                    .tipo(Notificacao.TipoNotificacao.ALERTA_30MIN)
                    .mensagem("URGENTE: Chamado #" + chamado.getId() + " escalado e com mais de 30 min. Especialista: " + (chamado.getEspecialista() != null ? chamado.getEspecialista().getNome() : "N/A"))
                    .dataEnvio(LocalDateTime.now())
                    .chamado(chamado)
                    .usuario(lider)
                    .build());
        }

        chamado.setAlerta30MinEnviado(true);
        chamadoRepository.save(chamado);
    }

    public long contarAlertasNaoLidos() {
        return notificacaoRepository.findByLidaFalseOrderByDataEnvioDesc().size();
    }

    public List<Notificacao> listarUltimasNotificacoes() {
        List<Notificacao> todas = notificacaoRepository.findByLidaFalseOrderByDataEnvioDesc();
        return todas.size() > 20 ? todas.subList(0, 20) : todas;
    }
}
