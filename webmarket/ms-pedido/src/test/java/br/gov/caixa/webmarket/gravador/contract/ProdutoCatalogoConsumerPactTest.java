package br.gov.caixa.webmarket.gravador.contract;

import br.gov.caixa.webmarket.gravador.infrastructure.client.ProdutoCatalogoClient;
import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ms-produto", pactVersion = PactSpecVersion.V4)
class ProdutoCatalogoConsumerPactTest {

    @Pact(consumer = "ms-pedido")
    V4Pact consultarProduto(PactDslWithProvider builder) {
        PactDslJsonBody response = new PactDslJsonBody()
                .integerType("id", 1)
                .stringType("nome", "Notebook")
                .decimalType("preco", 3500.00)
                .integerType("quantidadeEstoque", 10)
                .booleanType("ativo", true);

        return builder
                .given("produto ativo existe")
                .uponReceiving("consulta de produto pelo identificador")
                .path("/produtos/1")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(response)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "consultarProduto")
    void deveConsultarProduto(MockServer mockServer) {
        ProdutoCatalogoClient client = new ProdutoCatalogoClient(mockServer.getUrl());

        ProdutoCatalogoClient.ProdutoResponse produto = client.buscar(1);

        assertEquals("Notebook", produto.nome());
        assertEquals(new BigDecimal("3500.0"), produto.preco());
        assertEquals(10, produto.quantidadeEstoque());
        assertEquals(true, produto.ativo());
    }
}
