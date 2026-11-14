package com.mecanicadbo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "nota_fiscal")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotaFiscal {

    public enum StatusOcr { PENDENTE, PROCESSADO, ERRO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "os_id", nullable = false)
    private OrdemServico ordemServico;

    @Column(length = 120)
    private String fornecedor;

    @Column(name = "cnpj_fornecedor", length = 18)
    private String cnpjFornecedor;

    @Column(name = "numero_nf", length = 30)
    private String numeroNf;

    @Column(name = "data_emissao")
    private LocalDate dataEmissao;

    @Column(name = "valor_total_nf", precision = 10, scale = 2)
    private BigDecimal valorTotalNf;

    @Column(name = "imagem_path", length = 300)
    private String imagemPath;

    // ocr_status é o enum nativo status_ocr do PostgreSQL. Sem o NAMED_ENUM, o
    // Hibernate manda varchar e o INSERT falha (mesmo caso do status da OS).
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "ocr_status", nullable = false, columnDefinition = "status_ocr")
    @Builder.Default
    private StatusOcr ocrStatus = StatusOcr.PENDENTE;

    @Column(name = "importado_em")
    private LocalDateTime importadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }
}
