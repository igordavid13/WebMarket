package br.gov.caixa.webmarket.pedido.application.controller;

import br.gov.caixa.webmarket.pedido.domain.entity.Pedido;
import br.gov.caixa.webmarket.pedido.domain.service.PedidoService;
import io.micrometer.core.annotation.Counted;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
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
    public Response buscarPorId(@PathParam("id") Long id) {
        return pedidoService.buscarPorIdOpcional(id)
                .map(p -> Response.ok(p).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public Response criar(@NotNull @Valid Pedido pedido, @Context UriInfo uriInfo) {
        Pedido criado = pedidoService.criar(pedido);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(criado.getId())).build();
        return Response.created(location).entity(criado).build();
    }

    @PATCH
    @Path("/{id}/status")
    public Response atualizarStatus(@PathParam("id") Long id, @QueryParam("novoStatus") String novoStatus) {
        if (novoStatus == null || novoStatus.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"erro\": \"novoStatus é obrigatório\"}").build();
        }

        if (pedidoService.buscarPorIdOpcional(id).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Pedido atualizado = pedidoService.atualizarStatus(id, novoStatus);
        return Response.ok(atualizado).build();
    }

    @DELETE
    @Path("/{id}")
    public Response excluir(@PathParam("id") Long id) {
        boolean excluido = pedidoService.excluir(id);
        if (!excluido) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}