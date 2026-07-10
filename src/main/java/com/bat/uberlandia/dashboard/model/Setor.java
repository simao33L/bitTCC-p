package com.bat.uberlandia.dashboard.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "setores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Setor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(length = 200)
    private String descricao;

    @OneToMany(mappedBy = "setor", cascade = CascadeType.ALL)
    @ToString.Exclude
    @Builder.Default
    private List<Maquina> maquinas = new ArrayList<>();


}
