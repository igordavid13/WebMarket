package br.gov.caixa.webmarket.pedido.application.controller;

import br.gov.caixa.webmarket.pedido.domain.entity.Pedido;
import io.micrometer.core.annotation.Counted;
import jakarta.transaction.Transactional;
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
public class PedidoResource {

    /**
     * CRUD: Listagem com suporte a filtros (Critério 3: além do CRUD básico).
     * Métrica: contabiliza consultas para o /q/metrics (Critério 6 - Aula 4).
     */
    @GET
    @Counted(value = "pedidos_consultados_total", description = "Total de listagens de pedidos realizadas")
    public List<Pedido> listar(
            @QueryParam("status") String status,
            @QueryParam("cliente") String cliente) {

        if (status != null && !status.isBlank()) {
            return Pedido.findByStatus(status.toUpperCase());
        }
        if (cliente != null && !cliente.isBlank()) {
            return Pedido.findByCliente(cliente);
        }
        return Pedido.listAll();
    }

    /**
     * CRUD: Busca por ID (Retorna 200 OK ou 404 Not Found).
     */
    @GET
    @Path("/{id}")
    public Response buscarPorId(@PathParam("id") Long id) {
        Pedido pedido = Pedido.findById(id);
        if (pedido == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(pedido).build();
    }

    /**
     * CRUD: Criação de Pedido.
     * - @Transactional na operação de escrita (Critério 3).
     * - Retorna HTTP 201 Created com cabeçalho Location (Critério 1 - Nível 2).
     */
    @POST
    @Transactional
    public Response criar(@NotNull @Valid Pedido pedido, @Context UriInfo uriInfo) {
        pedido.id = null; // Garante geração de ID novo pela sequência
        pedido.prepararItensParaSalvar();
        pedido.persist();

        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(pedido.id)).build();
        return Response.created(location).entity(pedido).build();
    }

    /**
     * CRUD: Atualização de Status do Pedido (ex: CONFIRMADO / CANCELADO).
     */
    @PATCH
    @Path("/{id}/status")
    @Transactional
    public Response atualizarStatus(@PathParam("id") Long id, @QueryParam("novoStatus") String novoStatus) {
        if (novoStatus == null || novoStatus.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"erro\": \"novoStatus é obrigatório\"}").build();
        }

        Pedido pedido = Pedido.findById(id);
        if (pedido == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        pedido.status = novoStatus.toUpperCase();
        return Response.ok(pedido).build();
    }

    /**
     * CRUD: Exclusão lógica/física (Delete).
     * Retorna HTTP 204 No Content ou 404 Not Found.
     */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response excluir(@PathParam("id") Long id) {
        boolean excluido = Pedido.deleteById(id);
        if (!excluido) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}