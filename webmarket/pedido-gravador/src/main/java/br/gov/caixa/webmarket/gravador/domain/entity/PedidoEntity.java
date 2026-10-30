package br.gov.caixa.webmarket.gravador.domain.entity;

import br.gov.caixa.webmarket.contracts.ItemPedido;
import br.gov.caixa.webmarket.contracts.PedidoRequest;
import br.gov.caixa.webmarket.contracts.PedidoView;
import br.gov.caixa.webmarket.gravador.domain.enums.SituacaoPedidoEnum;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedido")
public class PedidoEntity {
    @Id
    private UUID id;
    @Column(name = "cliente_id", nullable = false)
    private Integer cliente;
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "ordem")
    private List<ItemPedidoEntity> itens = new ArrayList<>();
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SituacaoPedidoEntity> situacao = new ArrayList<>();
    @Column(name = "data_pedido", nullable = false)
    private LocalDateTime dataPedido;

    protected PedidoEntity() {}

    public static PedidoEntity de(UUID id, PedidoRequest dados) {
        PedidoEntity pedido = new PedidoEntity();
        pedido.id = id;
        pedido.cliente = dados.cliente();
        pedido.dataPedido = dados.dataPedido();
        dados.itens().forEach(item -> pedido.itens.add(new ItemPedidoEntity(pedido, item.produto(), item.quantidade())));
        pedido.situacao.add(new SituacaoPedidoEntity(pedido, SituacaoPedidoEnum.CRIADO));
        return pedido;
    }

    public PedidoView toView() {
        return new PedidoView(id, cliente,
                itens.stream().map(item -> new ItemPedido(item.getProduto(), item.getQuantidade())).toList(), dataPedido);
    }

    public UUID getId() { return id; }
    public Integer getCliente() { return cliente; }
    public List<ItemPedidoEntity> getItens() { return itens; }
    public List<SituacaoPedidoEntity> getSituacao() { return situacao; }
    public LocalDateTime getDataPedido() { return dataPedido; }
}