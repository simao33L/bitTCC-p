package com.bat.uberlandia.dashboard.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 50)
    private String login;

    @NotBlank
    @Column(nullable = false)
    private String senha;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nome;

    public enum Tipo {
        OPERADOR, TECNICO, LIDER, ESPECIALISTA, VISUALIZADOR
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id")
    @ToString.Exclude
    private Setor setor;

    @OneToMany(mappedBy = "tecnico")
    @ToString.Exclude
    @Builder.Default
    private List<Chamado> chamadosAtendidos = new ArrayList<>();

    @OneToMany(mappedBy = "especialista")
    @ToString.Exclude
    @Builder.Default
    private List<Chamado> chamadosEscalados = new ArrayList<>();

    @OneToMany(mappedBy = "usuario")
    @ToString.Exclude
    @Builder.Default
    private List<Notificacao> notificacoes = new ArrayList<>();
}