package br.gov.caixa.webmarket.produto.application.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
public class ProdutoResourceTest {

    @Test
    @DisplayName("Critério 1: Deve listar produtos retornando HTTP 200")
    public void deveListarProdutosComSucesso() {
        given()
                .when().get("/produtos")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2));
    }

    @Test
    @DisplayName("Critério 1: Deve criar produto retornando HTTP 201 com Location no header")
    public void deveCriarProdutoComLocation() {
        String novoProdutoJson = """
            {
                "nome": "Headset Gamer",
                "preco": 350.00,
                "quantidadeEstoque": 8
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(novoProdutoJson)
                .when().post("/produtos")
                .then()
                .statusCode(201)
                .header("Location", containsString("/produtos/"))
                .body("nome", equalTo("Headset Gamer"));
    }

    @Test
    @DisplayName("Critério 1 e 4: Deve retornar 404 com corpo padrão para produto inexistente")
    public void deveRetornar404ParaProdutoInexistente() {
        given()
                .when().get("/produtos/999999")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("erro", equalTo("Produto não encontrado"));
    }

    @Test
    @DisplayName("Critério 3: Deve filtrar produtos com estoque usando consulta customizada")
    public void deveFiltrarProdutosComEstoque() {
        given()
                .queryParam("somenteDisponiveis", true)
                .when().get("/produtos")
                .then()
                .statusCode(200)
                .body("quantidadeEstoque", everyItem(greaterThanOrEqualTo(1)));
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
        // Dispara uma chamada para incrementar a contagem
        given().when().get("/produtos").then().statusCode(200);

        given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .body(containsString("produtos_consultados_total"));
    }

    @Test
    @DisplayName("Critério 1 e 3: Deve excluir produto existente retornando HTTP 204")
    public void deveExcluirProdutoComSucesso() {
        // Exclui o item de id 3 (Monitor criado no import.sql)
        io.restassured.RestAssured.given()
                .when().delete("/produtos/3")
                .then()
                .statusCode(204);

        // Confirma que não existe mais (404)
        io.restassured.RestAssured.given()
                .when().get("/produtos/3")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Critério 4: Deve retornar 400 com corpo padronizado quando entrada for inválida")
    public void deveRetornar400ComCorpoPadrao() {
        String jsonInvalido = """
            {
                "nome": "",
                "preco": -10.00,
                "quantidadeEstoque": -1
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(jsonInvalido)
                .when().post("/produtos")
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("erro", equalTo("Requisição inválida"))
                .body("mensagens.size()", greaterThanOrEqualTo(3));
    }

    @Test
    @DisplayName("Critério 4: Deve retornar 409 com corpo padronizado ao cadastrar produto duplicado")
    public void deveRetornar409ProdutoDuplicado() {
        String duplicadoJson = """
            {
                "nome": "Teclado Mecânico",
                "preco": 250.00,
                "quantidadeEstoque": 5
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(duplicadoJson)
                .when().post("/produtos")
                .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("erro", equalTo("Produto duplicado"))
                .body("mensagens", not(empty()));
    }
}