package com.mecanicadbo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "cliente")
@EntityListeners(ClienteCpfListener.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nome;

    /**
     * CPF em números, SÓ EM MEMÓRIA: nunca vai para o banco. É preenchido ao
     * carregar (ClienteCpfListener) e gravado cifrado pelo ClienteService.
     * Vai no JSON para a tela, que o mostra mascarado.
     */
    @Transient
    private String cpf;

    /** CPF criptografado (AES-GCM). Só o CpfCripto lê. */
    @JsonIgnore
    @Column(name = "cpf_cifrado", columnDefinition = "text")
    private String cpfCifrado;

    /** Impressão digital do CPF: índice único e busca pelo CPF completo. */
    @JsonIgnore
    @Column(name = "cpf_hash", length = 64)
    private String cpfHash;

    @NotBlank(message = "Telefone é obrigatório")
    @Column(name = "telefone_1", nullable = false, length = 20)
    private String telefone1;

    @Column(name = "telefone_2", length = 20)
    private String telefone2;

    @Column(length = 100)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @JsonIgnore
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Veiculo> veiculos;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }
}