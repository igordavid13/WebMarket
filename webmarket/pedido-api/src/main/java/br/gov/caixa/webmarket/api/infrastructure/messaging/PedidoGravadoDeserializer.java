package br.gov.caixa.webmarket.api.infrastructure.messaging;

import br.gov.caixa.webmarket.contracts.PedidoGravadoEvent;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class PedidoGravadoDeserializer extends ObjectMapperDeserializer<PedidoGravadoEvent> {
    public PedidoGravadoDeserializer() {
        super(PedidoGravadoEvent.class);
    }
}