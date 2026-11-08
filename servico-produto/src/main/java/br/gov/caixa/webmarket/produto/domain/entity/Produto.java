package br.gov.caixa.webmarket.produto.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(
        name = "Produto",
        uniqueConstraints = @UniqueConstraint(name = "uk_produto_nome", columnNames = {"nome"})
)
public class Produto {

    @Id
    @SequenceGenerator(
            name = "produtoSeq",
            sequenceName = "produto_id_seq",
            allocationSize = 1,
            initialValue = 10
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "produtoSeq")
    private Long id;

    @NotBlank(message = "O nome do produto é obrigatório")
    @Column(nullable = false, unique = true)
    private String nome;

    @NotNull(message = "O preço é obrigatório")
    @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @NotNull(message = "A quantidade em estoque é obrigatória")
    @Min(value = 0, message = "O estoque não pode ser negativo")
    @Column(name = "quantidade_estoque", nullable = false)
    private Integer quantidadeEstoque;

    public Produto() {
    }

    public Produto(Long id, String nome, BigDecimal preco, Integer quantidadeEstoque) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public Integer getQuantidadeEstoque() { return quantidadeEstoque; }
    public void setQuantidadeEstoque(Integer quantidadeEstoque) { this.quantidadeEstoque = quantidadeEstoque; }
}