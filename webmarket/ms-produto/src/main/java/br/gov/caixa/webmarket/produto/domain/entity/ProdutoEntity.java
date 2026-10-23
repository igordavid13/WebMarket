package br.gov.caixa.webmarket.produto.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "produto")
public class ProdutoEntity {
    @Id
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal preco;

    @Column(name = "quantidade_estoque", nullable = false)
    private Integer quantidadeEstoque;

    @Column(nullable = false)
    private boolean ativo;

    protected ProdutoEntity() {}

    private ProdutoEntity(Integer id, String nome, BigDecimal preco, Integer quantidadeEstoque) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
        this.ativo = true;
    }

    public static ProdutoEntity criar(Integer id, String nome, BigDecimal preco, Integer quantidadeEstoque) {
        if (id == null || nome == null || nome.isBlank() || preco == null || preco.signum() <= 0
                || quantidadeEstoque == null || quantidadeEstoque < 0) {
            throw new IllegalArgumentException("Dados do produto inválidos");
        }
        return new ProdutoEntity(id, nome, preco, quantidadeEstoque);
    }

    public void retirarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade de movimentação deve ser maior que zero");
        }
        if (!ativo) {
            throw new IllegalArgumentException("Produto inativo não aceita baixa de estoque: " + id);
        }
        if (quantidadeEstoque < quantidade) {
            throw new IllegalArgumentException("Estoque insuficiente para o produto: " + id);
        }
        quantidadeEstoque -= quantidade;
    }

    public Integer getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public Integer getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
