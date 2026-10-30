package br.gov.caixa.webmarket.gravador.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "item_pedido")
public class ItemPedidoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoEntity pedido;
    @Column(name = "produto_id", nullable = false)
    private Integer produto;
    @Column(nullable = false)
    private Integer quantidade;

    protected ItemPedidoEntity() {}

    public ItemPedidoEntity(PedidoEntity pedido, Integer produto, Integer quantidade) {
        this.pedido = pedido;
        this.produto = produto;
        this.quantidade = quantidade;
    }

    public Long getId() { return id; }
    public PedidoEntity getPedido() { return pedido; }
    public Integer getProduto() { return produto; }
    public Integer getQuantidade() { return quantidade; }
}