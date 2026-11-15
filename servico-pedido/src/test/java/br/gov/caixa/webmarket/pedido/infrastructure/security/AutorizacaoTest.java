package br.gov.caixa.webmarket.pedido.infrastructure.security;

import br.gov.caixa.webmarket.pedido.infrastructure.client.ProdutoClient;
import br.gov.caixa.webmarket.pedido.infrastructure.client.ProdutoResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@QuarkusTest
public class AutorizacaoTest {

    @InjectMock
    @RestClient
    ProdutoClient produtoClient;

    @Test
    @DisplayName("Critério 5 (Posse): Cliente lista apenas os próprios pedidos e recebe 403 ao filtrar outro cliente")
    public void clienteListaApenasPropriosPedidos() {
        given()
                .auth().preemptive().basic("maria", "maria123")
                .when().get("/pedidos")
                .then()
                .statusCode(200)
                .body("clienteNome", everyItem(equalTo("maria")));

        given()
                .auth().preemptive().basic("maria", "maria123")
                .queryParam("cliente", "joao")
                .when().get("/pedidos")
                .then()
                .statusCode(403)
                .body("status", equalTo(403))
                .body("erro", equalTo("Acesso negado"));
    }

    @Test
    @DisplayName("Critério 5 (Posse): Cliente recebe 403 ao consultar pedido de outro cliente (ID 2 é do joao)")
    public void clienteRecebe403AoAcessarPedidoDeOutro() {
        given()
                .auth().preemptive().basic("maria", "maria123")
                .when().get("/pedidos/2")
                .then()
                .statusCode(403)
                .body("status", equalTo(403))
                .body("mensagens[0]", equalTo("O pedido pertence a outro cliente"));
    }

    @Test
    @DisplayName("Critério 5 (Posse): Cliente recebe 403 ao tentar criar pedido em nome de outro cliente")
    public void clienteRecebe403AoCriarPedidoParaOutro() {
        String pedidoParaJoao = """
            {
                "clienteNome": "joao",
                "itens": [ { "produtoId": 1, "quantidade": 1, "precoUnitario": 250.00 } ]
            }
            """;

        given()
                .auth().preemptive().basic("maria", "maria123")
                .contentType(ContentType.JSON)
                .body(pedidoParaJoao)
                .when().post("/pedidos")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Critério 5 (RBAC): Cliente recebe 403 ao tentar excluir pedido (exclusivo admin)")
    public void clienteRecebe403AoExcluirPedido() {
        given()
                .auth().preemptive().basic("maria", "maria123")
                .when().delete("/pedidos/1")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Critério 5 (RBAC): Admin acessa pedidos de qualquer cliente e /q/metrics")
    public void adminTemAcessoTotal() {
        given()
                .auth().preemptive().basic("admin", "admin123")
                .when().get("/pedidos/2")
                .then()
                .statusCode(200)
                .body("clienteNome", equalTo("joao"));

        given().auth().preemptive().basic("admin", "admin123")
                .when().get("/q/metrics").then().statusCode(200);
    }

    @Test
    @DisplayName("Critério 5 (Posse): Cliente consegue criar pedido em seu próprio nome (201)")
    public void clienteCriaProprioPedidoComSucesso() {
        when(produtoClient.verificarDisponibilidade(anyLong(), anyInt()))
                .thenReturn(new ProdutoResponse(1L, "Teclado", new BigDecimal("250.00"), 10));

        String pedidoMaria = """
            {
                "clienteNome": "maria",
                "itens": [ { "produtoId": 1, "quantidade": 1, "precoUnitario": 250.00 } ]
            }
            """;

        given()
                .auth().preemptive().basic("maria", "maria123")
                .contentType(ContentType.JSON)
                .body(pedidoMaria)
                .when().post("/pedidos")
                .then()
                .statusCode(201);
    }
}