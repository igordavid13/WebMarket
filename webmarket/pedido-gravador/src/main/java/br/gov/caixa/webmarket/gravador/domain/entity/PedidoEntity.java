package br.gov.caixa.webmarket.gravador.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
public class PedidoEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="cliente_id", nullable = false)
    private ClienteEntity cliente;

    @OneToMany(mappedBy = "pedido")
    private List<ItemPedidoEntity> itens = new ArrayList<>();

    @OneToMany(mappedBy = "pedido")
    private List<SituacaoPedidoEntity> situacao = new ArrayList<>();

    @Column(nullable = false)
    private Float valorTotal;

    @Column(name = "data_pedido")
    private LocalDateTime dataPedido;

    protected PedidoEntity() {}

    private PedidoEntity(UUID id, PedidoRequest pedidoRequest) {
        this.id = id;
        this.cliente = pedidoRequest.getCliente();
        this.itens = pedidoRequest.getItens();
        this.situacao = pedidoRequest.getSituacao();
        this.valorTotal = pedidoRequest.getValorTotal();
        this.dataPedido = pedidoRequest.getDataPedido();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ClienteEntity getCliente() {
        return cliente;
    }

    public void setCliente(ClienteEntity cliente) {
        this.cliente = cliente;
    }

    public List<ItemPedidoEntity> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedidoEntity> itens) {
        this.itens = itens;
    }

    public List<SituacaoPedidoEntity> getSituacao() {
        return situacao;
    }

    public void setSituacao(List<SituacaoPedidoEntity> situacao) {
        this.situacao = situacao;
    }

    public Float getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(Float valorTotal) {
        this.valorTotal = valorTotal;
    }

    public LocalDateTime getDataPedido() {
        return dataPedido;
    }

    public void setDataPedido(LocalDateTime dataPedido) {
        this.dataPedido = dataPedido;
    }
}
