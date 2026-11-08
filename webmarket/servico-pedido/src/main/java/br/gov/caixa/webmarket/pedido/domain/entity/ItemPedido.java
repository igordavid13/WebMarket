package br.gov.caixa.webmarket.pedido.domain.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "ItemPedido")
public class ItemPedido extends PanacheEntityBase {

    @Id
    @SequenceGenerator(
            name = "itemPedidoSeq",
            sequenceName = "item_pedido_id_seq",
            allocationSize = 1,
            initialValue = 10
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "itemPedidoSeq")
    public Long id;

    @NotNull(message = "O ID do produto é obrigatório")
    @Column(name = "produto_id", nullable = false)
    public Long produtoId;

    @NotNull(message = "A quantidade é obrigatória")
    @Min(value = 1, message = "A quantidade mínima é 1")
    @Column(nullable = false)
    public Integer quantidade;

    @NotNull(message = "O preço unitário é obrigatório")
    @DecimalMin(value = "0.01", message = "O preço unitário deve ser maior que zero")
    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    public BigDecimal precoUnitario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    @JsonBackReference // Evita recursão infinita na serialização JSON
    public Pedido pedido;
}