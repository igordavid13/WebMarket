package br.gov.caixa.webmarket.gravador.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pedido_outbox")
public class PedidoOutboxEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String destino;

    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String payload;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    private LocalDateTime publicadoEm;

    protected PedidoOutboxEntity() {}

    private PedidoOutboxEntity(UUID id, String destino, String payload) {
        this.id = id;
        this.destino = destino;
        this.payload = payload;
        this.criadoEm = LocalDateTime.now();
    }

    public static PedidoOutboxEntity pendente(String destino, String payload) {
        return new PedidoOutboxEntity(UUID.randomUUID(), destino, payload);
    }

    public UUID getId() {
        return id;
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
