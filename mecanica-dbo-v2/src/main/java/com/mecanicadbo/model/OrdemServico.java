package com.mecanicadbo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ordem_servico")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @Column(name = "km_entrada")
    private Integer kmEntrada;

    @Column(name = "data_entrada", nullable = false)
    private LocalDate dataEntrada;

    @Column(name = "data_saida_prevista")
    private LocalDate dataSaidaPrevista;

    @Column(name = "data_saida_real")
    private LocalDate dataSaidaReal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusOs status = StatusOs.ABERTA;

    @Column(columnDefinition = "TEXT")
    private String reclamacoes;

    @Column(columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "total_pecas", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalPecas = BigDecimal.ZERO;

    @Column(name = "total_servicos", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalServicos = BigDecimal.ZERO;

    @Column(name = "total_geral", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalGeral = BigDecimal.ZERO;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @JsonIgnore
    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ItemPeca> itensPeca;

    @JsonIgnore
    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ItemServico> itensServico;

    @JsonIgnore
    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<NotaFiscal> notasFiscais;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        if (this.dataEntrada == null) this.dataEntrada = LocalDate.now();
    }
}