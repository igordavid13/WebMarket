package br.gov.caixa.webmarket.gravador.domain.entity;

import br.gov.caixa.webmarket.gravador.domain.entity.ProdutoEntity;


import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "item_pedido")
public class ItemPedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoEntity pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private ProdutoEntity produto;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private Integer quantidade;

    protected ItemPedidoEntity() {}

    public ItemPedidoEntity(PedidoEntity pedido, ProdutoEntity produto, BigDecimal precoUnitario, Integer quantidade) {
        this.pedido = pedido;
        this.produto = produto;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
    }

    public Long getId() {
        return id;
    }

    public PedidoEntity getPedido() {
        return pedido;
    }

    public ProdutoEntity getProduto() {
        return produto;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoTotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
