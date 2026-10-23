package br.gov.caixa.webmarket.gravador.infrastructure.messaging;

import br.gov.caixa.webmarket.contracts.PedidoGravadoEvent;
import br.gov.caixa.webmarket.gravador.infrastructure.config.KafkaConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventoPublisher {

    private final KafkaTemplate<String, PedidoGravadoEvent> kafka;

    public EventoPublisher(KafkaTemplate<String, PedidoGravadoEvent> kafka) {
        this.kafka = kafka;
    }

    public void publicar(PedidoGravadoEvent evento) {
        kafka.send(KafkaConfig.TOPICO, evento.identificadorPedido().toString(), evento);
    }
}
