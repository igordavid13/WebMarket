package br.gov.caixa.webmarket.gravador.infrastructure.messaging;

import br.gov.caixa.webmarket.contracts.PedidoGravadoEvent;
import br.gov.caixa.webmarket.gravador.domain.entity.EventoPendenteEntity;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class EventoPublisher {
    private final EntityManager entityManager;
    private final Emitter<Record<String, PedidoGravadoEvent>> eventos;

    public EventoPublisher(EntityManager entityManager,
            @Channel("pedidos-gravados") Emitter<Record<String, PedidoGravadoEvent>> eventos) {
        this.entityManager = entityManager;
        this.eventos = eventos;
    }

    @Scheduled(every = "1s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    public void publicarPendentes() throws Exception {
        var pendentes = entityManager.createQuery("from EventoPendenteEntity order by dataPedido", EventoPendenteEntity.class)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(20).getResultList();
        for (EventoPendenteEntity pendente : pendentes) {
            PedidoGravadoEvent evento = pendente.toEvent();
            eventos.send(Record.of(evento.identificadorPedido().toString(), evento))
                    .toCompletableFuture().get(10, TimeUnit.SECONDS);
            entityManager.remove(pendente);
        }
    }
}