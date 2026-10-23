package br.gov.caixa.webmarket.produto.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_evento_produto")
public class OutboxEventoEntity {
    @Id
    private UUID eventId;

    @Column(nullable = false, length = 120)
    private String destino;

    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String payload;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    private LocalDateTime publicadoEm;

    protected OutboxEventoEntity() {}

    public OutboxEventoEntity(UUID eventId, String destino, String payload) {
        this.eventId = eventId;
        this.destino = destino;
        this.payload = payload;
        this.criadoEm = LocalDateTime.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getDestino() {
        return destino;
    }

    public String getPayload() {
        return payload;
    }

    public void marcarPublicado() {
        publicadoEm = LocalDateTime.now();
    }
}
