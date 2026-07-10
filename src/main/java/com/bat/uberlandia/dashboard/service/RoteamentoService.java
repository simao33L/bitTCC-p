package com.bat.uberlandia.dashboard.service;

import com.bat.uberlandia.dashboard.model.*;
import com.bat.uberlandia.dashboard.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RoteamentoService {

    private final UsuarioRepository usuarioRepository;
    private final ChamadoRepository chamadoRepository;

    public static class SugestaoTecnico {
        private final Long tecnicoId;
        private final String tecnicoNome;
        private final String tecnicoSetor;
        private final int chamadosAtivos;
        private final int experienciaMotivo;
        private final double score;
        private final String justificativa;

        public SugestaoTecnico(Long tecnicoId, String tecnicoNome, String tecnicoSetor,
                               int chamadosAtivos, int experienciaMotivo, double score, String justificativa) {
            this.tecnicoId = tecnicoId;
            this.tecnicoNome = tecnicoNome;
            this.tecnicoSetor = tecnicoSetor;
            this.chamadosAtivos = chamadosAtivos;
            this.experienciaMotivo = experienciaMotivo;
            this.score = score;
            this.justificativa = justificativa;
        }

        public Long getTecnicoId() { return tecnicoId; }
        public String getTecnicoNome() { return tecnicoNome; }
        public String getTecnicoSetor() { return tecnicoSetor; }
        public int getChamadosAtivos() { return chamadosAtivos; }
        public int getExperienciaMotivo() { return experienciaMotivo; }
        public double getScore() { return score; }
        public String getJustificativa() { return justificativa; }
    }

    public SugestaoTecnico sugerirTecnico(Maquina maquina, Chamado.MotivoFalha motivo) {
        List<Usuario> tecnicos = usuarioRepository.findByTipo(Usuario.Tipo.TECNICO);
        if (tecnicos.isEmpty()) return null;

        List<Chamado.Status> statusAtivos = Arrays.asList(
                Chamado.Status.EM_ANDAMENTO, Chamado.Status.PAUSADO, Chamado.Status.ESCALADO);

        SugestaoTecnico melhor = null;
        double melhorScore = -1;

        for (Usuario tec : tecnicos) {
            int ativos = chamadoRepository.findByTecnicoIdAndStatusIn(tec.getId(), statusAtivos).size();
            int experiencia = 0;
            if (motivo != null) {
                experiencia = (int) chamadoRepository
                        .countByTecnicoIdAndMotivoFalhaAndStatus(tec.getId(), motivo, Chamado.Status.CONCLUIDO);
            }

            double score = 0;
            score -= ativos * 10;
            score += experiencia * 5;

            String setorTec = (tec.getSetor() != null) ? tec.getSetor().getNome() : "Sem setor";
            if (tec.getSetor() != null && maquina.getSetor() != null
                    && tec.getSetor().getId().equals(maquina.getSetor().getId())) {
                score += 15;
            }

            StringBuilder just = new StringBuilder();
            just.append("Carga atual: ").append(ativos).append(" chamados ativos; ");
            just.append("Experiencia com ").append(motivo != null ? motivo.name() : "motivo")
               .append(": ").append(experiencia).append(" resolucoes; ");
            just.append("Setor: ").append(setorTec);

            if (score > melhorScore) {
                melhorScore = score;
                melhor = new SugestaoTecnico(tec.getId(), tec.getNome(), setorTec,
                        ativos, experiencia, score, just.toString());
            }
        }

        return melhor;
    }
}