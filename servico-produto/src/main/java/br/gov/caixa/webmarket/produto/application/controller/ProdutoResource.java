package br.gov.caixa.webmarket.produto.application.controller;

import br.gov.caixa.webmarket.produto.domain.entity.Produto;
import br.gov.caixa.webmarket.produto.domain.service.ProdutoService;
import io.micrometer.core.annotation.Counted;
import io.quarkus.security.Authenticated;
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
import static br.gov.caixa.webmarket.produto.infrastructure.security.Papeis.ADMIN;
import static br.gov.caixa.webmarket.produto.infrastructure.security.Papeis.CLIENTE;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;

@Path("/produtos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@ApplicationScoped
public class ProdutoResource {

    @Inject
    ProdutoService produtoService;

    @GET
    @RolesAllowed({CLIENTE, ADMIN})
    @Counted(value = "produtos_consultados_total", description = "Total de listagens de produtos realizadas")
    public List<Produto> listar(@QueryParam("somenteDisponiveis") @DefaultValue("false") boolean somenteDisponiveis) {
        return produtoService.listar(somenteDisponiveis);
    }

    @GET
    @RolesAllowed({CLIENTE, ADMIN})
    @Path("/{id}")
    public Produto buscarPorId(@PathParam("id") Long id) {
        return produtoService.buscarPorId(id);
    }

    @GET
    @Path("/{id}/disponibilidade")
    public Produto verificarDisponibilidade(@PathParam("id") Long id, @QueryParam("quantidade") @DefaultValue("1") Integer quantidade) {
        return produtoService.verificarDisponibilidade(id, quantidade);
    }

    @POST
    @RolesAllowed(ADMIN)
    public Response criar(@NotNull @Valid Produto produto, @Context UriInfo uriInfo) {
        Produto criado = produtoService.cadastrar(produto);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(criado.getId())).build();
        return Response.created(location).entity(criado).build();
    }

    @PUT
    @RolesAllowed(ADMIN)
    @Path("/{id}")
    public Produto atualizar(@PathParam("id") Long id, @NotNull @Valid Produto dados) {
        return produtoService.atualizar(id, dados);
    }

    @DELETE
    @RolesAllowed(ADMIN)
    @Path("/{id}")
    public Response excluir(@PathParam("id") Long id) {
        produtoService.excluir(id);
        return Response.noContent().build();
    }
}