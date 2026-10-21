package br.gov.caixa.webmarket.pedido.domain.entity;

import br.gov.caixa.webmarket.pedido.domain.enums.SituacaoPedidoEnum;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "situacao_pedido")
public class SituacaoPedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "pedido_id",
            nullable = false
    )
    private PedidoEntity pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoPedidoEnum situacao;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    protected SituacaoPedidoEntity() {
    }

    public SituacaoPedidoEntity(
            PedidoEntity pedido,
            SituacaoPedidoEnum situacao) {

        this.pedido = pedido;
        this.situacao = situacao;
        this.dataHora = LocalDateTime.now();
    }


}
