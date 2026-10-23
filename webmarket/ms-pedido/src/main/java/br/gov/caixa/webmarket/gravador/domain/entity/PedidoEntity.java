package br.gov.caixa.webmarket.gravador.domain.entity;

import br.gov.caixa.webmarket.gravador.domain.enums.SituacaoPedidoEnum;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "pedido")
public class PedidoEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="cliente_id", nullable = false)
    private ClienteEntity cliente;

    @OrderBy("dataHora ASC, id ASC")
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedidoEntity> itens = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SituacaoPedidoEntity> situacao = new ArrayList<>();

    @Column(nullable = false)
    private Float valorTotal;

    @Column(name = "data_pedido")
    private LocalDateTime dataPedido;

    protected PedidoEntity() {}

    private PedidoEntity(UUID id, ClienteEntity cliente) {
        this.id = id;
        this.cliente = cliente;
        this.situacao.add(new SituacaoPedidoEntity(this, SituacaoPedidoEnum.CRIADO));
        this.dataPedido = LocalDateTime.now();
    }

    public static PedidoEntity de(UUID id, ClienteEntity cliente) {
        return new PedidoEntity(id, cliente);
    }

    public void adicionarItem(ItemPedidoEntity item) {
        itens.add(item);
        valorTotal = itens.stream()
                .map(ItemPedidoEntity::getPrecoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .floatValue();
    }

    public void confirmar() {
        if (!situacao.isEmpty()
                && situacao.get(situacao.size() - 1).getSituacao() == SituacaoPedidoEnum.CONFIRMADO) {
            return;
        }
        situacao.add(new SituacaoPedidoEntity(this, SituacaoPedidoEnum.CONFIRMADO));
    }

    public UUID getId() {
        return id;
    }

    public ClienteEntity getCliente() {
        return cliente;
    }

    public List<ItemPedidoEntity> getItens() {
        return itens;
    }

    public List<SituacaoPedidoEntity> getSituacao() {
        return situacao;
    }

    public Float getValorTotal() {
        return valorTotal;
    }

    public LocalDateTime getDataPedido() {
        return dataPedido;
    }

}
