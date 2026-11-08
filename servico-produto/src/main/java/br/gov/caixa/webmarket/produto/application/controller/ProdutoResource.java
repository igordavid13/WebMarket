package br.gov.caixa.webmarket.produto.application.controller;

import br.gov.caixa.webmarket.produto.domain.entity.Produto;
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

@Path("/produtos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProdutoResource {

    /**
     * Lista todos os produtos cadastrados.
     * Métricas: contabiliza consultas para o /q/metrics (Critério 6 - Aula 4).
     */
    @GET
    @Counted(value = "produtos_consultados_total", description = "Total de listagens de produtos realizadas")
    public List<Produto> listarTodos(@QueryParam("somenteDisponiveis") @DefaultValue("false") boolean somenteDisponiveis) {
        if (somenteDisponiveis) {
            return Produto.findComEstoqueDisponivel();
        }
        return Produto.listAll();
    }

    /**
     * Busca um produto por ID. Retorna 200 ou 404.
     */
    @GET
    @Path("/{id}")
    public Response buscarPorId(@PathParam("id") Long id) {
        Produto produto = Produto.findById(id);
        if (produto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(produto).build();
    }

    /**
     * Endpoint específico para o servico-pedido verificar saldo em estoque.
     * Retorna 200 se disponível ou 409/404 se não houver estoque suficiente.
     */
    @GET
    @Path("/{id}/disponibilidade")
    public Response verificarDisponibilidade(@PathParam("id") Long id, @QueryParam("quantidade") @DefaultValue("1") Integer quantidade) {
        Produto produto = Produto.findById(id);
        if (produto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (produto.quantidadeEstoque < quantidade) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"erro\": \"Estoque insuficiente\", \"disponivel\": " + produto.quantidadeEstoque + "}")
                    .build();
        }
        return Response.ok(produto).build();
    }

    /**
     * Cadastro de produto:
     * - Usa @Transactional nas operações de escrita (Critério 3).
     * - Retorna HTTP 201 com cabeçalho Location apontando para o recurso criado (Critério 1 - Nível 2).
     */
    @POST
    @Transactional
    public Response criar(@NotNull @Valid Produto produto, @Context UriInfo uriInfo) {
        produto.id = null; // Garante geração de novo ID
        produto.persist();

        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(produto.id)).build();
        return Response.created(location).entity(produto).build();
    }

    /**
     * Atualização de estoque.
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public Response atualizar(@PathParam("id") Long id, @NotNull @Valid Produto dadosAtualizados) {
        Produto produto = Produto.findById(id);
        if (produto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        produto.nome = dadosAtualizados.nome;
        produto.preco = dadosAtualizados.preco;
        produto.quantidadeEstoque = dadosAtualizados.quantidadeEstoque;
        return Response.ok(produto).build();
    }

    /**
     * Exclusão de produto (CRUD - Delete)
     * Retorna HTTP 204 No Content se excluído ou 404 se não existir.
     */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response excluir(@PathParam("id") Long id) {
        boolean excluido = Produto.deleteById(id);
        if (!excluido) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}