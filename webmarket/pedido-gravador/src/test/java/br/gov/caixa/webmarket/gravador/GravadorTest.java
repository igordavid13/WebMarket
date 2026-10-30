package br.gov.caixa.webmarket.gravador;

import br.gov.caixa.webmarket.contracts.*;
import br.gov.caixa.webmarket.gravador.application.service.GravadorService;
import br.gov.caixa.webmarket.gravador.infrastructure.messaging.EventoPublisher;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.kafka.Record;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@QuarkusTest
class GravadorTest {
    @Inject GravadorService gravador;
    @Inject EventoPublisher publisher;
    @Inject @Any InMemoryConnector connector;

    @Test
    void gravaConsultaEReentregaSemDuplicar() throws Exception {
        UUID id = UUID.randomUUID();
        var command = command(id);
        given().contentType("application/json").body(command).post("/interno/pedidos").then().statusCode(200);
        given().get("/interno/pedidos/{id}", id).then().statusCode(200)
                .body("identificadorPedido", equalTo(id.toString())).body("cliente", equalTo(1))
                .body("itens[0].produto", equalTo(10)).body("itens[0].quantidade", equalTo(2));
        assertFalse(gravador.gravar(command));
        var sink = connector.<Record<String, PedidoGravadoEvent>>sink("pedidos-gravados");
        long antes = sink.received().stream().filter(message -> message.getPayload().key().equals(id.toString())).count();
        publisher.publicarPendentes();
        publisher.publicarPendentes();
        assertEquals(antes + 1, sink.received().stream()
                .filter(message -> message.getPayload().key().equals(id.toString())).count());
    }

    @Test
    void consomeComandoDaMensageria() {
        UUID id = UUID.randomUUID();
        connector.<JsonObject>source("gravacao").send(new JsonObject()
                .put("identificadorPedido", id.toString()).put("dados", new JsonObject()
                        .put("cliente", 1).put("dataPedido", "2026-10-30T12:00:00")
                        .put("itens", new io.vertx.core.json.JsonArray().add(new JsonObject().put("produto", 10).put("quantidade", 2)))));
        await().atMost(Duration.ofSeconds(10)).until(() -> gravador.buscar(id).isPresent());
    }

    @Test
    void rejeitaDadosInvalidosENaoEncontraPedidoInexistente() {
        given().contentType("application/json").body("{\"identificadorPedido\":null,\"dados\":null}")
                .post("/interno/pedidos").then().statusCode(400);
        given().contentType("application/json").body(new GravarPedidoCommand(UUID.randomUUID(),
                new PedidoRequest(1, List.of(new ItemPedido(10, 0)), LocalDateTime.now())))
                .post("/interno/pedidos").then().statusCode(400);
        given().get("/interno/pedidos/{id}", UUID.randomUUID()).then().statusCode(404);
        given().get("/q/health").then().statusCode(200);
    }

    private GravarPedidoCommand command(UUID id) {
        return new GravarPedidoCommand(id, new PedidoRequest(1, List.of(new ItemPedido(10, 2)), LocalDateTime.now()));
    }
}