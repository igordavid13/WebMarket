package br.gov.caixa.webmarket.pedido.infrastructure.client;

import br.gov.caixa.webmarket.pedido.domain.exception.ProdutoIndisponivelNegocioException;
import br.gov.caixa.webmarket.pedido.domain.exception.ServicoProdutoIndisponivelException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class ProdutoGateway {

    @Inject
    @RestClient
    ProdutoClient produtoClient;

    @Timeout(500)
    @Retry(maxRetries = 2, abortOn = ProdutoIndisponivelNegocioException.class)
    @CircuitBreaker(requestVolumeThreshold = 4, skipOn = ProdutoIndisponivelNegocioException.class)
    @Fallback(fallbackMethod = "fallbackIndisponivel", skipOn = ProdutoIndisponivelNegocioException.class)
    public ProdutoResponse confirmarDisponibilidade(Long produtoId, Integer quantidade) {
        return produtoClient.verificarDisponibilidade(produtoId, quantidade);
    }

    ProdutoResponse fallbackIndisponivel(Long produtoId, Integer quantidade) {
        throw new ServicoProdutoIndisponivelException();
    }
}