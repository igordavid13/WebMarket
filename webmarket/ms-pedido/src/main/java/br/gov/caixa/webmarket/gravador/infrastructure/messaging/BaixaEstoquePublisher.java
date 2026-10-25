package br.gov.caixa.webmarket.gravador.infrastructure.messaging;

import br.gov.caixa.webmarket.contracts.BaixarEstoqueCommand;
import br.gov.caixa.webmarket.gravador.domain.entity.PedidoOutboxEntity;
import br.gov.caixa.webmarket.gravador.infrastructure.config.MessagingConfig;
import br.gov.caixa.webmarket.gravador.infrastructure.repository.PedidoOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
public class BaixaEstoquePublisher {
    private static final Logger log = LoggerFactory.getLogger(BaixaEstoquePublisher.class);
    private final PedidoOutboxRepository outbox;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public BaixaEstoquePublisher(
            PedidoOutboxRepository outbox,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${webmarket.outbox.fixed-delay:1000}")
    @Transactional
    public void publicarPendentes() {
        List<PedidoOutboxEntity> pendentes = outbox.buscarPendentes();
        for (PedidoOutboxEntity registro : pendentes) {
            try {
                BaixarEstoqueCommand comando = objectMapper.readValue(
                        registro.getPayload(), BaixarEstoqueCommand.class);
                rabbitTemplate.convertAndSend(
                    MessagingConfig.BAIXA_ESTOQUE_EXCHANGE,
                    MessagingConfig.BAIXA_ESTOQUE_ROUTING,
                        comando);
                registro.marcarPublicado();
                outbox.save(registro);
            } catch (Exception exception) {
                log.error("Falha ao publicar comando de baixa da outbox: {}", registro.getId(), exception);
                return;
            }
        }
    }
}
