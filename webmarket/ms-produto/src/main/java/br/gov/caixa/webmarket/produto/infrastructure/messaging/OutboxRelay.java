package br.gov.caixa.webmarket.produto.infrastructure.messaging;

import br.gov.caixa.webmarket.produto.domain.entity.OutboxEventoEntity;
import br.gov.caixa.webmarket.produto.infrastructure.repository.OutboxEventoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxRelay {
    private final OutboxEventoRepository outbox;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public OutboxRelay(
            OutboxEventoRepository outbox,
            KafkaTemplate<String, Object> kafka,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelayString = "${webmarket.outbox.fixed-delay:1000}")
    public void publicarPendentes() {
        List<OutboxEventoEntity> pendentes = outbox.buscarPendentes();
        for (OutboxEventoEntity evento : pendentes) {
            try {
                JsonNode payload = objectMapper.readTree(evento.getPayload());
                String chave = payload.path("identificadorPedido").asText();
                kafka.send(evento.getDestino(), chave, payload).get(10, TimeUnit.SECONDS);
                transactionTemplate.executeWithoutResult(status -> {
                    outbox.findById(evento.getEventId()).ifPresent(registro -> {
                        registro.marcarPublicado();
                        outbox.save(registro);
                    });
                });
            } catch (Exception exception) {
                return;
            }
        }
    }
}
