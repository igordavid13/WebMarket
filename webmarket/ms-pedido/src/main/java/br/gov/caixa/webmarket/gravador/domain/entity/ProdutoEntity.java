package br.gov.caixa.webmarket.gravador.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "produto")
public class ProdutoEntity {
    @Id
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Float preco;

    @Column(nullable = false)
    private Integer quantidade;

    protected ProdutoEntity() {}

    private ProdutoEntity(Integer id, String nome, Float preco, Integer quantidade) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
        this.quantidade = quantidade;
    }

    public static ProdutoEntity de(Integer id, String nome, Float preco, Integer quantidade) {
        return new br.gov.caixa.webmarket.gravador.domain.entity.ProdutoEntity(id, nome, preco, quantidade);
    }

    public Integer getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Float getPreco() {
        return preco;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void retirarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade do item deve ser maior que zero");
        }
        if (this.quantidade == null || quantidade > this.quantidade) {
            throw new IllegalArgumentException(
                    "Estoque insuficiente para o produto: " + id);
        }
        this.quantidade -= quantidade;
    }

}
