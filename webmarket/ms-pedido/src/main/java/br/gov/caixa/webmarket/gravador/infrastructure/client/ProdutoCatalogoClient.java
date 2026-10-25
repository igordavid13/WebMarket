package br.gov.caixa.webmarket.gravador.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProdutoCatalogoClient {
    private final URI baseUri;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ProdutoCatalogoClient(String baseUrl) {
        this(URI.create(baseUrl), HttpClient.newHttpClient(), new ObjectMapper().findAndRegisterModules());
    }

    ProdutoCatalogoClient(URI baseUri, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUri = baseUri;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public ProdutoResponse buscar(Integer id) {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/produtos/" + id))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                throw new IllegalArgumentException("Produto não encontrado: " + id);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Catálogo indisponível. Status HTTP: " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), ProdutoResponse.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível consultar o catálogo de produtos", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta ao catálogo interrompida", exception);
        }
    }

    public record ProdutoResponse(
            Integer id,
            String nome,
            java.math.BigDecimal preco,
            Integer quantidadeEstoque,
            boolean ativo) {
    }
}
