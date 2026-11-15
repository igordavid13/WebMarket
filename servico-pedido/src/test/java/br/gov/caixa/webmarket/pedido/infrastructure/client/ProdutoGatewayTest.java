package br.gov.caixa.webmarket.pedido.infrastructure.client;

import br.gov.caixa.webmarket.pedido.domain.exception.ProdutoIndisponivelNegocioException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@QuarkusTest
public class ProdutoGatewayTest {

    @InjectMock
    @RestClient
    ProdutoClient produtoClient;

    @Inject
    CircuitBreakerMaintenance circuitos;

    @BeforeEach
    @AfterEach
    void resetarCircuitos() {
        circuitos.resetAll();
    }

    private String payloadPedido(String cliente) {
        return """
            {
                "clienteNome": "%s",
                "itens": [
                    { "produtoId": 1, "quantidade": 2, "precoUnitario": 250.00 }
                ]
            }
            """.formatted(cliente);
    }

    @Test
    @DisplayName("Critério 2: Falha técnica tenta 3 vezes (1 + 2 retries), devolve 503 e não grava o pedido")
    public void falhaTecnicaTentaTresVezesEDevolve503SemGravar() {
        when(produtoClient.verificarDisponibilidade(anyLong(), anyInt()))
                .thenThrow(new ProcessingException("Connection refused"));

        given()
                .contentType(ContentType.JSON)
                .body(payloadPedido("Cliente Falha Tecnica"))
                .when().post("/pedidos")
                .then()
                .statusCode(503)
                .body("status", equalTo(503))
                .body("mensagens", not(empty()));

        verify(produtoClient, times(3)).verificarDisponibilidade(anyLong(), anyInt());

        given()
                .when().get("/pedidos")
                .then()
                .statusCode(200)
                .body("clienteNome", not(hasItem("Cliente Falha Tecnica")));
    }

    @Test
    @DisplayName("Critério 2: Resposta de negócio (produto inexistente/sem estoque) devolve 422 sem retry")
    public void erroDeNegocioDevolve422SemRetry() {
        when(produtoClient.verificarDisponibilidade(anyLong(), anyInt()))
                .thenThrow(new ProdutoIndisponivelNegocioException("Estoque insuficiente"));

        given()
                .contentType(ContentType.JSON)
                .body(payloadPedido("Cliente Negocio 422"))
                .when().post("/pedidos")
                .then()
                .statusCode(422)
                .body("status", equalTo(422))
                .body("mensagens", not(empty()));

        verify(produtoClient, times(1)).verificarDisponibilidade(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Critério 2: Circuito abre após janela de 4 falhas e bloqueia novas chamadas imediatas")
    public void circuitoAbertoNemChamaServicoDeProdutos() {
        when(produtoClient.verificarDisponibilidade(anyLong(), anyInt()))
                .thenThrow(new ProcessingException("Timeout"));

        // 1ª requisição consome 3 chamadas (1 + 2 retries)
        given().contentType(ContentType.JSON).body(payloadPedido("Cliente C1"))
                .when().post("/pedidos").then().statusCode(503);

        // 2ª requisição faz a 4ª chamada, atingindo requestVolumeThreshold=4 e abrindo o circuito
        given().contentType(ContentType.JSON).body(payloadPedido("Cliente C2"))
                .when().post("/pedidos").then().statusCode(503);

        reset(produtoClient);

        // 3ª requisição deve falhar imediatamente no fallback sem chamar o cliente remoto
        given().contentType(ContentType.JSON).body(payloadPedido("Cliente C3"))
                .when().post("/pedidos").then().statusCode(503);

        verifyNoInteractions(produtoClient);
    }
}