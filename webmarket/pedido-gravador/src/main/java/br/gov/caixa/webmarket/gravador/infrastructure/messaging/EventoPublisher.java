package br.com.caixa.pix.gravador.infrastructure.messaging;

import br.com.caixa.pix.contracts.PedidoGravadoEvent;
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
