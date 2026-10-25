package br.gov.caixa.webmarket.produto.application.service;

import br.gov.caixa.webmarket.produto.application.view.ProdutoView;
import br.gov.caixa.webmarket.produto.infrastructure.repository.ProdutoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProdutoCatalogoService {
    private static final String CHAVE_CATALOGO = "produtos:catalogo";
    private static final String PREFIXO_PRODUTO = "produto:";
    private static final Duration TTL_CACHE = Duration.ofMinutes(10);

    private final ProdutoRepository produtos;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ProdutoCatalogoService(
            ProdutoRepository produtos,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.produtos = produtos;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ProdutoView> listar() {
        String catalogoEmCache = redisTemplate.opsForValue().get(CHAVE_CATALOGO);
        if (catalogoEmCache != null) {
            return desserializarLista(catalogoEmCache);
        }

        List<ProdutoView> catalogo = produtos.findAll().stream()
                .map(ProdutoView::de)
                .toList();
        salvarCatalogoNoCache(catalogo);
        return catalogo;
    }

    @Transactional(readOnly = true)
    public ProdutoView buscar(Integer id) {
        String produtoEmCache = redisTemplate.opsForValue().get(chaveProduto(id));
        if (produtoEmCache != null) {
            return desserializarProduto(id, produtoEmCache);
        }

        ProdutoView produto = produtos.findById(id)
                .map(ProdutoView::de)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Produto não encontrado: " + id));

        redisTemplate.opsForValue().set(chaveProduto(id), serializar(produto), TTL_CACHE);
        return produto;
    }

    public void invalidarCache(Integer produtoId) {
        redisTemplate.delete(List.of(CHAVE_CATALOGO, chaveProduto(produtoId)));
    }

    private void salvarCatalogoNoCache(List<ProdutoView> catalogo) {
        redisTemplate.opsForValue().set(CHAVE_CATALOGO, serializar(catalogo), TTL_CACHE);
        catalogo.forEach(produto ->
                redisTemplate.opsForValue().set(chaveProduto(produto.id()), serializar(produto), TTL_CACHE)
        );
    }

    private List<ProdutoView> desserializarLista(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            redisTemplate.delete(CHAVE_CATALOGO);
            return listar();
        }
    }

    private ProdutoView desserializarProduto(Integer id, String json) {
        try {
            return objectMapper.readValue(json, ProdutoView.class);
        } catch (JsonProcessingException exception) {
            redisTemplate.delete(chaveProduto(id));
            return buscar(id);
        }
    }

    private String serializar(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível serializar dados do catálogo", exception);
        }
    }

    private String chaveProduto(Integer id) {
        return PREFIXO_PRODUTO + id;
    }
}