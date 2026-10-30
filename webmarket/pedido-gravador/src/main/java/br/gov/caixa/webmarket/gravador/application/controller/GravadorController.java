package br.gov.caixa.webmarket.gravador.application.controller;

import br.gov.caixa.webmarket.contracts.GravarPedidoCommand;
import br.gov.caixa.webmarket.gravador.application.service.GravadorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

@Path("/interno/pedidos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GravadorController {
    private final GravadorService gravador;

    public GravadorController(GravadorService gravador) {
        this.gravador = gravador;
    }

    @POST
    public Response gravar(@NotNull @Valid GravarPedidoCommand command) {
        gravador.gravar(command);
        return Response.ok().build();
    }

    @GET
    @Path("/{id}")
    public Response buscar(@PathParam("id") UUID id) {
        return gravador.buscar(id).map(pedido -> Response.ok(pedido).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}