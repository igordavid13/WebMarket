package br.gov.caixa.webmarket.produto.application.controller;

import br.gov.caixa.webmarket.produto.domain.entity.Produto;
import br.gov.caixa.webmarket.produto.domain.service.ProdutoService;
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

@Path("/produtos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ProdutoResource {

    @Inject
    ProdutoService produtoService;

    @GET
    @Counted(value = "produtos_consultados_total", description = "Total de listagens de produtos realizadas")
    public List<Produto> listar(@QueryParam("somenteDisponiveis") @DefaultValue("false") boolean somenteDisponiveis) {
        return produtoService.listar(somenteDisponiveis);
    }

    @GET
    @Path("/{id}")
    public Response buscarPorId(@PathParam("id") Long id) {
        return produtoService.buscarPorIdOpcional(id)
                .map(p -> Response.ok(p).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/{id}/disponibilidade")
    public Response verificarDisponibilidade(@PathParam("id") Long id, @QueryParam("quantidade") @DefaultValue("1") Integer quantidade) {
        var opt = produtoService.buscarPorIdOpcional(id);
        if (opt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Produto produto = opt.get();
        if (produto.getQuantidadeEstoque() < quantidade) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"erro\": \"Estoque insuficiente\", \"disponivel\": " + produto.getQuantidadeEstoque() + "}")
                    .build();
        }
        return Response.ok(produto).build();
    }

    @POST
    public Response criar(@NotNull @Valid Produto produto, @Context UriInfo uriInfo) {
        Produto criado = produtoService.cadastrar(produto);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(criado.getId())).build();
        return Response.created(location).entity(criado).build();
    }

    @PUT
    @Path("/{id}")
    public Response atualizar(@PathParam("id") Long id, @NotNull @Valid Produto dados) {
        if (produtoService.buscarPorIdOpcional(id).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Produto atualizado = produtoService.atualizar(id, dados);
        return Response.ok(atualizado).build();
    }

    @DELETE
    @Path("/{id}")
    public Response excluir(@PathParam("id") Long id) {
        boolean excluido = produtoService.excluir(id);
        if (!excluido) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}