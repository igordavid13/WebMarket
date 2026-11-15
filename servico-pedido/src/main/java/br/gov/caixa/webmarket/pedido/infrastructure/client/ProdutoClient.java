package br.gov.caixa.webmarket.pedido.infrastructure.client;

import br.gov.caixa.webmarket.pedido.domain.exception.ProdutoIndisponivelNegocioException;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/produtos")
@RegisterRestClient(configKey = "produtos")
@Produces(MediaType.APPLICATION_JSON)
public interface ProdutoClient {

    @GET
    @Path("/{id}/disponibilidade")
    ProdutoResponse verificarDisponibilidade(
            @PathParam("id") Long id,
            @QueryParam("quantidade") Integer quantidade
    );

    @ClientExceptionMapper
    static RuntimeException mapearErro(Response response) {
        int status = response.getStatus();
        if (status == Response.Status.NOT_FOUND.getStatusCode()) {
            return new ProdutoIndisponivelNegocioException("Produto não encontrado no catálogo");
        }
        if (status == Response.Status.CONFLICT.getStatusCode()) {
            return new ProdutoIndisponivelNegocioException("Estoque insuficiente para o produto solicitado");
        }
        return null; // Status 5xx mantém comportamento padrão (falha técnica)
    }
}