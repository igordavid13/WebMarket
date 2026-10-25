package br.gov.caixa.webmarket.produto.contract;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import br.gov.caixa.webmarket.produto.domain.entity.ProdutoEntity;
import br.gov.caixa.webmarket.produto.infrastructure.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

@Provider("ms-produto")
@PactFolder("src/test/resources/pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProdutoProviderPactTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProdutoRepository produtos;

    @BeforeEach
    void configurarTarget(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verificar(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("produto ativo existe")
    void prepararProduto() {
        produtos.save(ProdutoEntity.criar(1, "Notebook", new BigDecimal("3500.00"), 10));
    }
}
