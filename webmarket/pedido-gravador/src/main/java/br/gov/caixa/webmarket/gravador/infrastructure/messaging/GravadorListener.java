package br.gov.caixa.webmarket.gravador.infrastructure.messaging;

import br.gov.caixa.webmarket.contracts.GravarPedidoCommand;
import br.gov.caixa.webmarket.gravador.application.service.GravadorService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class GravadorListener {
    private final GravadorService gravador;
    private final ObjectMapper mapper;

    public GravadorListener(GravadorService gravador, ObjectMapper mapper) {
        this.gravador = gravador;
        this.mapper = mapper;
    }

    @Incoming("gravacao")
    @Blocking
    @Retry(maxRetries = 2, delay = 1000, jitter = 0)
    public void aoReceber(JsonObject payload) throws JsonProcessingException {
        gravador.gravar(mapper.readValue(payload.encode(), GravarPedidoCommand.class));
    }
}