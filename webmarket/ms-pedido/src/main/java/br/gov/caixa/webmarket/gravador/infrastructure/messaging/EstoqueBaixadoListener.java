package br.gov.caixa.webmarket.gravador.infrastructure.messaging;

import br.gov.caixa.webmarket.contracts.EstoqueBaixadoEvent;
import br.gov.caixa.webmarket.gravador.application.service.GravadorService;
import br.gov.caixa.webmarket.gravador.infrastructure.config.KafkaConfig;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EstoqueBaixadoListener {
    private final GravadorService gravador;

    public EstoqueBaixadoListener(GravadorService gravador) {
        this.gravador = gravador;
    }

    @KafkaListener(topics = KafkaConfig.TOPICO_ESTOQUE_BAIXADO)
    public void aoReceber(EstoqueBaixadoEvent evento) {
        gravador.confirmarPedido(evento);
    }
}
