package br.gov.caixa.webmarket.produto.infrastructure.repository;

import br.gov.caixa.webmarket.produto.domain.entity.Produto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;

@QuarkusTest
public class ProdutoRepositoryTest {

    @Inject
    ProdutoRepository produtoRepository;

    @Test
    @DisplayName("Critério 3: Deve consultar apenas produtos com estoque disponível")
    public void deveBuscarProdutosComEstoqueDisponivel() {
        List<Produto> disponiveis = produtoRepository.findComEstoqueDisponivel();
        Assertions.assertFalse(disponiveis.isEmpty(), "Deveria encontrar produtos carregados pelo import.sql");
        Assertions.assertTrue(disponiveis.stream().allMatch(p -> p.getQuantidadeEstoque() > 0));
    }

    @Test
    @DisplayName("Critério 3: Deve buscar produto por nome ignorando maiúsculas/minúsculas")
    public void deveBuscarPorNome() {
        var produto = produtoRepository.findByNome("teclado mecânico");
        Assertions.assertTrue(produto.isPresent());
        Assertions.assertEquals("Teclado Mecânico", produto.get().getNome());
    }
}