package br.gov.caixa.webmarket.gravador.domain.entity;

import br.gov.caixa.webmarket.contracts.PedidoGravadoEvent;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evento_pendente")
public class EventoPendenteEntity {
    @Id
    private UUID id;
    @Column(nullable = false)
    private LocalDateTime dataPedido;

    protected EventoPendenteEntity() {}

    public EventoPendenteEntity(UUID id, LocalDateTime dataPedido) {
        this.id = id;
        this.dataPedido = dataPedido;
    }

    public PedidoGravadoEvent toEvent() {
        return new PedidoGravadoEvent(id, dataPedido);
    }
}