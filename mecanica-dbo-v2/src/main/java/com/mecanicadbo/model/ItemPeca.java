package com.mecanicadbo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Peça lançada numa OS.
 *
 * valorUnitario aceita valor NEGATIVO de propósito: é assim que entra a linha
 * de crédito (ex.: "Crédito mecânico -258,51"), um abatimento que o cliente já
 * tinha. Por isso não há @PositiveOrZero aqui nem CHECK no banco — o Bean
 * Validation roda antes do INSERT, então a anotação sozinha já barraria.
 *
 * Em item_servico o valor continua sem poder ser negativo (anotação e CHECK):
 * crédito é peça, não mão de obra.
 */
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

    /**
     * Sem @PositiveOrZero: aceita valor negativo para linhas de crédito
     * ("Crédito mecânico", desconto de peça devolvida, etc.).
     */
    @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;

    // Gerado pelo banco (GENERATED ALWAYS AS ... STORED) — somente leitura
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
