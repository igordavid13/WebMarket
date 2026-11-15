package br.gov.caixa.webmarket.pedido.application.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import br.gov.caixa.webmarket.pedido.infrastructure.client.ProdutoClient;
import br.gov.caixa.webmarket.pedido.infrastructure.client.ProdutoResponse;
import io.quarkus.test.InjectMock;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.math.BigDecimal;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PedidoResourceTest {

    @InjectMock
    @RestClient
    ProdutoClient produtoClient;

    @Test
    @DisplayName("Critério 1: Deve listar pedidos retornando HTTP 200")
    public void deveListarPedidos() {
        given()
                .when().get("/pedidos")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2));
    }

    @Test
    @DisplayName("Critério 1: Deve criar pedido com itens retornando HTTP 201 com Location")
    public void deveCriarPedidoComSucesso() {
        String novoPedidoJson = """
            {
                "clienteNome": "Carlos Drummond",
                "status": "CRIADO",
                "itens": [
                    {
                        "produtoId": 1,
                        "quantidade": 2,
                        "precoUnitario": 250.00
                    }
                ]
            }
            """;
        when(produtoClient.verificarDisponibilidade(anyLong(), anyInt()))
                .thenReturn(new ProdutoResponse(1L, "Teclado Mecânico", new BigDecimal("250.00"), 10));
        given()
                .contentType(ContentType.JSON)
                .body(novoPedidoJson)
                .when().post("/pedidos")
                .then()
                .statusCode(201)
                .header("Location", containsString("/pedidos/"))
                .body("clienteNome", equalTo("Carlos Drummond"))
                .body("valorTotal", equalTo(500.0F));
    }

    @Test
    @DisplayName("Critério 1 e 4: Deve retornar 404 com corpo padrão para ID inexistente")
    public void deveRetornar404ParaPedidoInexistente() {
        given()
                .when().get("/pedidos/999999")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("erro", equalTo("Pedido não encontrado"));
    }

    @Test
    @DisplayName("Critério 3: Deve filtrar pedidos por status além do CRUD básico")
    public void deveFiltrarPedidosPorStatus() {
        given()
                .queryParam("status", "CONFIRMADO")
                .when().get("/pedidos")
                .then()
                .statusCode(200)
                .body("status", everyItem(equalTo("CONFIRMADO")));
    }

    @Test
    @DisplayName("Critério 4: Deve retornar 400 com corpo padronizado ao enviar pedido sem cliente ou sem itens")
    public void deveRetornar400PedidoInvalido() {
        String pedidoInvalido = """
            {
                "clienteNome": "",
                "itens": []
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(pedidoInvalido)
                .when().post("/pedidos")
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("erro", equalTo("Requisição inválida"))
                .body("mensagens.size()", greaterThanOrEqualTo(2));
    }

    @Test
    @DisplayName("Critério 6: Deve responder UP no health check liveness")
    public void deveResponderHealthCheckUp() {
        given()
                .when().get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("Critério 6: Deve expor métricas do Micrometer em /q/metrics")
    public void deveExporMetricasPrometheus() {
        given().when().get("/pedidos").then().statusCode(200);

        given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .body(containsString("pedidos_consultados_total"));
    }
}