package br.gov.caixa.webmarket.api.infrastructure.client;

import br.gov.caixa.webmarket.contracts.PedidoView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.util.UUID;

@ApplicationScoped
@RegisterRestClient(configKey = "gravador")
@Path("/interno/pedidos")
@Produces(MediaType.APPLICATION_JSON)
public interface GravadorClient {
    @GET
    @Path("/{id}")
    PedidoView buscar(@PathParam("id") UUID id);
}