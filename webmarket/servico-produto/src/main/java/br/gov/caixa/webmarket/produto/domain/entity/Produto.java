package br.gov.caixa.webmarket.produto.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "Produto")
public class Produto extends PanacheEntityBase {

    @Id
    @SequenceGenerator(
            name = "produtoSeq",
            sequenceName = "produto_id_seq",
            allocationSize = 1,
            initialValue = 10 // Inicia em 10 para não colidir com os IDs 1, 2 e 3 do import.sql
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "produtoSeq")
    public Long id;

    @NotBlank(message = "O nome do produto é obrigatório")
    @Column(nullable = false)
    public String nome;

    @NotNull(message = "O preço é obrigatório")
    @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal preco;

    @NotNull(message = "A quantidade em estoque é obrigatória")
    @Min(value = 0, message = "O estoque não pode ser negativo")
    @Column(name = "quantidade_estoque", nullable = false)
    public Integer quantidadeEstoque;

    // =========================================================================
    // Consultas além do CRUD básico (Critério 3 - Persistência com Panache)
    // =========================================================================

    public static List<Produto> findComEstoqueDisponivel() {
        return find("quantidadeEstoque > 0").list();
    }

    public static List<Produto> findByNome(String nome) {
        return find("LOWER(nome) LIKE LOWER(?1)", "%" + nome + "%").list();
    }

    public static Optional<Produto> findDisponivelParaVenda(Long id, Integer quantidade) {
        return find("id = ?1 and quantidadeEstoque >= ?2", id, quantidade).firstResultOptional();
    }
}