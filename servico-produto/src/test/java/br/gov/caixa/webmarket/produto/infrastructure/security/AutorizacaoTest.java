package br.gov.caixa.webmarket.produto.infrastructure.security;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
public class AutorizacaoTest {

    @Test
    @DisplayName("Critério 5: Cliente pode consultar produtos (200), mas recebe 403 ao tentar cadastrar ou excluir")
    public void clientePodeLerMasNaoAlterarProdutos() {
        given()
                .auth().preemptive().basic("maria", "maria123")
                .when().get("/produtos")
                .then()
                .statusCode(200);

        String novoProduto = """
            { "nome": "Webcam HD", "preco": 150.00, "quantidadeEstoque": 5 }
            """;

        given()
                .auth().preemptive().basic("maria", "maria123")
                .contentType(ContentType.JSON)
                .body(novoProduto)
                .when().post("/produtos")
                .then()
                .statusCode(403)
                .body("status", equalTo(403))
                .body("erro", equalTo("Acesso negado"));

        given()
                .auth().preemptive().basic("maria", "maria123")
                .when().delete("/produtos/1")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Critério 5: Métricas (/q/metrics) exigem papel admin (401 sem login, 403 cliente, 200 admin)")
    public void metricasRestritasAoAdmin() {
        given().when().get("/q/metrics").then().statusCode(401);

        given().auth().preemptive().basic("maria", "maria123")
                .when().get("/q/metrics").then().statusCode(403);

        given().auth().preemptive().basic("admin", "admin123")
                .when().get("/q/metrics").then().statusCode(200);
    }
}