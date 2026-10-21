package br.com.caixa.pix.gravador.infrastructure.messaging;

import br.com.caixa.pix.contracts.GravarPedidoCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class GravacaoListener {

    private static final Logger log = LoggerFactory.getLogger(GravacaoListener.class);

    private final GravadorService gravador;

    public GravacaoListener(GravadorService gravador) {
        this.gravador = gravador;
    }

    @RabbitListener(queues = MessagingConfig.FILA)
    public void aoReceber(GravarComprovanteCommand command) {
        gravador.gravar(command); // idempotente sob redelivery
    }

    @RabbitListener(queues = MessagingConfig.DLQ)
    public void aoCairNaDlq(GravarPedidoCommand command) {
        log.warn("Pedido {} caiu na DLQ após esgotar as tentativas",
                command.identificadorPedido());
    }
}
