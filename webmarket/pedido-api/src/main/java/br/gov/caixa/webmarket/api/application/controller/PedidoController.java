package br.gov.caixa.webmarket.api.application.controller;

import br.gov.caixa.webmarket.api.infrastructure.client.GravadorClient;
import br.gov.caixa.webmarket.contracts.GravarPedidoCommand;
import br.gov.caixa.webmarket.contracts.PedidoRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

@Path("/pedidos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PedidoController {
    private final Emitter<JsonObject> comandos;
    private final ObjectMapper mapper;
    private final GravadorClient gravador;

    public PedidoController(@Channel("gravacao") Emitter<JsonObject> comandos,
            ObjectMapper mapper, @RestClient GravadorClient gravador) {
        this.comandos = comandos;
        this.mapper = mapper;
        this.gravador = gravador;
    }

    @POST
    public CompletionStage<Response> criar(@NotNull @Valid PedidoRequest dados, @Context UriInfo uriInfo)
            throws JsonProcessingException {
        UUID id = UUID.randomUUID();
        URI location = uriInfo.getAbsolutePathBuilder().path(id.toString()).build();
        JsonObject payload = new JsonObject(mapper.writeValueAsString(new GravarPedidoCommand(id, dados)));
        CompletableFuture<Void> confirmacao = new CompletableFuture<>();
        try {
            comandos.send(Message.of(payload, () -> {
                confirmacao.complete(null);
                return CompletableFuture.completedFuture(null);
            }, failure -> {
                confirmacao.completeExceptionally(failure);
                return CompletableFuture.completedFuture(null);
            }).addMetadata(new OutgoingRabbitMQMetadata.Builder().withDeliveryMode(2).build()));
            return confirmacao.orTimeout(10, TimeUnit.SECONDS)
                    .thenApply(ignored -> Response.accepted(Map.of("identificadorPedido", id, "situacao", "RECEBIDO"))
                            .location(location).build())
                    .exceptionally(failure -> indisponivel());
        } catch (IllegalStateException failure) {
            return CompletableFuture.completedFuture(indisponivel());
        }
    }

    @GET
    @Path("/{id}")
    public Response buscar(@PathParam("id") UUID id) {
        try {
            return Response.ok(gravador.buscar(id)).build();
        } catch (WebApplicationException failure) {
            if (failure.getResponse().getStatus() == 404) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return indisponivel();
        } catch (ProcessingException failure) {
            return indisponivel();
        }
    }

    private Response indisponivel() {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(Map.of("mensagem", "Servico temporariamente indisponivel")).build();
    }
}