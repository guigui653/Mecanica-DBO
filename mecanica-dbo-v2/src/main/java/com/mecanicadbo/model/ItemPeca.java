package com.mecanicadbo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "item_peca")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ItemPeca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "os_id", nullable = false)
    private OrdemServico ordemServico;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nf_id")
    private NotaFiscal notaFiscal;

    @NotBlank(message = "Descrição é obrigatória")
    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal quantidade = BigDecimal.ONE;

    @PositiveOrZero(message = "Valor unitário não pode ser negativo")
    @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;

    // Gerado pelo banco — somente leitura
    @Column(name = "valor_total", insertable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "pago_pelo_cliente", nullable = false)
    @Builder.Default
    private Boolean pagoPeloCliente = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }
}
