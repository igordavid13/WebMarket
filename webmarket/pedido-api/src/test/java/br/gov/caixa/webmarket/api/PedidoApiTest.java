package br.gov.caixa.webmarket.api;

import br.gov.caixa.webmarket.api.infrastructure.client.GravadorClient;
import br.gov.caixa.webmarket.contracts.*;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class PedidoApiTest {
    @Inject @Any InMemoryConnector connector;
    @InjectMock @RestClient GravadorClient gravador;

    @Test
    void aceitaPedidoEEnviaComandoValidado() {
        var dados = new PedidoRequest(1, List.of(new ItemPedido(10, 2)), LocalDateTime.now());
        var resposta = given().contentType("application/json").body(dados).post("/pedidos")
                .then().statusCode(202).header("Location", containsString("/pedidos/"))
                .body("situacao", equalTo("RECEBIDO")).extract();
        String id = resposta.path("identificadorPedido");
        assertNotNull(UUID.fromString(id));
        var mensagens = connector.<JsonObject>sink("gravacao").received();
        JsonObject payload = mensagens.stream().map(message -> message.getPayload())
                .filter(json -> id.equals(json.getString("identificadorPedido"))).findFirst().orElseThrow();
        assertEquals(1, payload.getJsonObject("dados").getInteger("cliente"));
        assertEquals(2, payload.getJsonObject("dados").getJsonArray("itens").getJsonObject(0).getInteger("quantidade"));
    }

    @Test
    void consultaPedidoNoGravador() {
        UUID id = UUID.randomUUID();
        when(gravador.buscar(id)).thenReturn(new PedidoView(id, 1, List.of(new ItemPedido(10, 2)), LocalDateTime.now()));
        given().get("/pedidos/{id}", id).then().statusCode(200).body("identificadorPedido", equalTo(id.toString()));
    }

    @Test
    void trataPedidoInexistenteEGravadorIndisponivel() {
        UUID ausente = UUID.randomUUID();
        when(gravador.buscar(ausente)).thenThrow(new NotFoundException());
        given().get("/pedidos/{id}", ausente).then().statusCode(404);
        UUID indisponivel = UUID.randomUUID();
        when(gravador.buscar(indisponivel)).thenThrow(new ProcessingException("Indisponivel"));
        given().get("/pedidos/{id}", indisponivel).then().statusCode(503);
    }

    @Test
    void rejeitaPedidoInvalidoSemPublicar() {
        int antes = connector.<JsonObject>sink("gravacao").received().size();
        given().contentType("application/json").body("{\"cliente\":1,\"itens\":[],\"dataPedido\":\"2026-10-30T12:00:00\"}")
                .post("/pedidos").then().statusCode(400);
        given().contentType("application/json").body("{\"cliente\":1,\"itens\":[{\"produto\":10,\"quantidade\":0}],\"dataPedido\":\"2026-10-30T12:00:00\"}")
                .post("/pedidos").then().statusCode(400);
        assertEquals(antes, connector.<JsonObject>sink("gravacao").received().size());
        given().get("/q/health").then().statusCode(200);
    }
}