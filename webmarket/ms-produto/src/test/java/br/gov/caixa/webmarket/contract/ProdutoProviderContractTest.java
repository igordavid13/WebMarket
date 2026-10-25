package br.gov.caixa.webmarket.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import br.gov.caixa.webmarket.produto.MsProdutoApplication;
import br.gov.caixa.webmarket.produto.infrastructure.messaging.OutboxRelay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Provider("ms-produto")
@PactFolder("../ms-pedido/target/pacts")
@SpringBootTest(
        classes = MsProdutoApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.kafka.listener.auto-startup=false"
        }
)
public class ProdutoProviderContractTest {

    @LocalServerPort
    private int port;

    // Isola o Kafka e o relay durante a validação do contrato HTTP
    @MockitoBean
    private KafkaAdmin kafkaAdmin;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private OutboxRelay outboxRelay;

    @BeforeEach
    void setup(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("produto com id 1 existe")
    void prepararProduto() {
        // Inicialização mantida pelo CatalogoInicializador no H2
    }
}