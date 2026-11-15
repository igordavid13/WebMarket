package br.gov.caixa.webmarket.pedido.application.controller;

import br.gov.caixa.webmarket.pedido.domain.entity.Pedido;
import br.gov.caixa.webmarket.pedido.domain.service.PedidoService;
import io.micrometer.core.annotation.Counted;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Path("/pedidos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@ApplicationScoped
public class PedidoResource {

    @Inject
    PedidoService pedidoService;

    @GET
    @Counted(value = "pedidos_consultados_total", description = "Total de listagens de pedidos realizadas")
    public List<Pedido> listar(
            @QueryParam("status") String status,
            @QueryParam("cliente") String cliente) {
        return pedidoService.listar(status, cliente);
    }

    @GET
    @Path("/{id}")
    public Pedido buscarPorId(@PathParam("id") Long id) {
        return pedidoService.buscarPorId(id);
    }

    @POST
    public Response criar(@NotNull @Valid Pedido pedido, @Context UriInfo uriInfo) {
        Pedido criado = pedidoService.criar(pedido);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(criado.getId())).build();
        return Response.created(location).entity(criado).build();
    }

    @PATCH
    @Path("/{id}/status")
    public Pedido atualizarStatus(@PathParam("id") Long id, @QueryParam("novoStatus") @NotBlank String novoStatus) {
        return pedidoService.atualizarStatus(id, novoStatus);
    }

    @DELETE
    @Path("/{id}")
    public Response excluir(@PathParam("id") Long id) {
        pedidoService.excluir(id);
        return Response.noContent().build();
    }
}