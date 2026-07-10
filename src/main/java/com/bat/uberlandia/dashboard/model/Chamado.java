package com.bat.uberlandia.dashboard.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "chamados")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    @Column(length = 500)
    private String caminhoFoto;

    public enum Status {
        ABERTO, EM_ANDAMENTO, PAUSADO, ESCALADO, CONCLUIDO
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ABERTO;

    public enum MotivoFalha {
        ELETRICA, MECANICA, PNEUMATICA, HIDRAULICA,
        ELETRONICA, SOFTWARE, OPERACIONAL, DESGASTE, OUTROS
    }

    @Enumerated(EnumType.STRING)
    private MotivoFalha motivoFalha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maquina_id", nullable = false)
    @ToString.Exclude
    private Maquina maquina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    @ToString.Exclude
    private Usuario tecnico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialista_id")
    @ToString.Exclude
    private Usuario especialista;
    
    private LocalDateTime inicioReparo;
    private LocalDateTime inicioPausa;
    private Long tempoReparoAcumulado;

    private LocalDateTime inicioLocomocao;
    private LocalDateTime fimLocomocao;
    private Long tempoLocomocaoSegundos;

    private LocalDateTime dataAbertura;
    private LocalDateTime dataConclusao;
    private LocalDateTime dataEscalacao;

    @Builder.Default
    private Boolean alerta30MinEnviado = false;

        public long getTempoReparoSegundos() {
        if (inicioReparo == null) return 0L;
        long acumulado = tempoReparoAcumulado != null ? tempoReparoAcumulado : 0L;

        if ((status == Status.PAUSADO || status == Status.ESCALADO)
                && inicioPausa != null) {
            return acumulado;
        }
        LocalDateTime referencia = (inicioPausa != null) ? inicioPausa : inicioReparo;
        long segundosCorridos = Duration
                .between(referencia, LocalDateTime.now()).getSeconds();
        return acumulado + segundosCorridos;
    }
    
    public boolean isAtrasado() {
        if (status != Status.EM_ANDAMENTO && status != Status.ESCALADO)
            return false;
        return getTempoReparoSegundos() > 1800;  // 30 min
    }

    @PrePersist
    public void prePersist() {
        if (dataAbertura == null) {
            dataAbertura = LocalDateTime.now();
        }
    }
}