package br.gov.caixa.webmarket.api.infrastructure.messaging;

import br.gov.caixa.webmarket.contracts.PedidoGravadoEvent;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PedidoGravadoListener {
    private static final Logger LOG = Logger.getLogger(PedidoGravadoListener.class);

    @Incoming("pedidos-gravados")
    public void aoReceber(PedidoGravadoEvent evento) {
        LOG.infof("Gravacao confirmada para o pedido %s", evento.identificadorPedido());
    }
}