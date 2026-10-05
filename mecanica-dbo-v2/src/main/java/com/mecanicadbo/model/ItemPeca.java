package com.mecanicadbo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * O QUE MUDOU AQUI
 * ----------------
 * Saiu o @PositiveOrZero do valorUnitario.
 *
 * Lembra da linha "Crédito mecânico -258,51" da planilha do seu pai? Ela é um
 * abatimento: valor que o cliente já tinha em crédito e que entra na OS com
 * sinal negativo. Você já removeu o CHECK do banco por causa disso:
 *
 *     ALTER TABLE item_peca DROP CONSTRAINT item_peca_valor_unitario_check;
 *
 * Só que a anotação continuou aqui, e o Bean Validation roda ANTES do INSERT,
 * dentro do Hibernate. Ou seja: o banco já aceitava o valor negativo, mas a
 * aplicação nunca deixava a linha chegar até ele. Derrubar a constraint sem
 * tirar a anotação não resolve nada — é preciso tirar as duas.
 *
 * Em item_servico o @PositiveOrZero FOI MANTIDO de propósito: lá o CHECK do
 * banco continua de pé, e serviço com valor negativo não faz sentido no
 * negócio. Crédito é peça, não mão de obra.
 *
 * O resto do arquivo está igual ao seu — nada mais foi tocado.
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
