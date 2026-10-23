package br.gov.caixa.webmarket.produto.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evento_processado_produto")
public class EventoProcessadoEntity {
    @Id
    private UUID eventId;

    @Column(nullable = false)
    private LocalDateTime processadoEm;

    protected EventoProcessadoEntity() {}

    public EventoProcessadoEntity(UUID eventId) {
        this.eventId = eventId;
        this.processadoEm = LocalDateTime.now();
    }
}
