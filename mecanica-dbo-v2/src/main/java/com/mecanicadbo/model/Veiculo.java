package com.mecanicadbo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "veiculo")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @NotBlank(message = "Placa é obrigatória")
    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @NotBlank(message = "Marca é obrigatória")
    @Column(nullable = false, length = 60)
    private String marca;

    @NotBlank(message = "Modelo é obrigatório")
    @Column(nullable = false, length = 80)
    private String modelo;

    @Column(length = 20)
    private String chassi;

    @NotBlank(message = "Cor é obrigatória")
    @Column(nullable = false, length = 40)
    private String cor;

    @NotBlank(message = "Combustível é obrigatório")
    @Column(nullable = false, length = 20)
    private String combustivel;

    @Column(name = "ano_fabricacao")
    private Short anoFabricacao;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @JsonIgnore
    @OneToMany(mappedBy = "veiculo", fetch = FetchType.LAZY)
    private List<OrdemServico> ordensServico;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }
}