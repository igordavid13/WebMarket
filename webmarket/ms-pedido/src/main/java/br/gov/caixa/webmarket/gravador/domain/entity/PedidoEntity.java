package br.gov.caixa.webmarket.gravador.domain.entity;

import br.gov.caixa.webmarket.gravador.domain.enums.SituacaoPedido;
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

    @Column(name = "id_cliente", nullable = false)
    private Long idCliente;

    @OrderBy("id ASC")
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedidoEntity> itens = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoPedido situacao;

    @Column(nullable = false)
    private Float valorTotal;

    @Column(name = "data_pedido")
    private LocalDateTime dataPedido;

    protected PedidoEntity() {}

    private PedidoEntity(UUID id, Long idCliente) {
        this.id = id;
        this.idCliente = idCliente;
        this.situacao = SituacaoPedido.CRIADO;
        this.dataPedido = LocalDateTime.now();
    }

    public static PedidoEntity de(UUID id, Long idCliente) {
        return new PedidoEntity(id, idCliente);
    }

    public void adicionarItem(ItemPedidoEntity item) {
        itens.add(item);
        valorTotal = itens.stream()
                .map(ItemPedidoEntity::getPrecoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .floatValue();
    }

    public void confirmar() {
        if (situacao == SituacaoPedido.CONFIRMADO) {
            return;
        }
        situacao = SituacaoPedido.CONFIRMADO;
    }

    public UUID getId() {
        return id;
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public List<ItemPedidoEntity> getItens() {
        return itens;
    }

    public SituacaoPedido getSituacao() {
        return situacao;
    }

    public Float getValorTotal() {
        return valorTotal;
    }

    public LocalDateTime getDataPedido() {
        return dataPedido;
    }

}
