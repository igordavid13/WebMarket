package br.gov.caixa.webmarket.contract;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ms-produto")
public class ProdutoConsumerContractTest {

    @Pact(consumer = "ms-pedido")
    public V4Pact consultaProdutoPact(PactDslWithProvider builder) {
        return builder
                .given("produto com id 1 existe")
                .uponReceiving("consulta do produto 1 no catalogo")
                .path("/produtos/1")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(newJsonBody(o -> {
                    o.integerType("id", 1);
                    o.stringType("nome", "Teclado");
                    o.decimalType("preco", 150.00);
                    o.integerType("quantidadeEstoque", 10);
                    o.booleanType("ativo", true);
                }).build())
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "consultaProdutoPact")
    void deveConsultarProdutoComSucesso(au.com.dius.pact.consumer.MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(
                mockServer.getUrl() + "/produtos/1", String.class);

        assertEquals(200, response.getStatusCode().value());
    }
}