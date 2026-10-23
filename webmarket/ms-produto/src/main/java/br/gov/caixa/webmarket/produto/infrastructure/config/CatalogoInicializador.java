package br.gov.caixa.webmarket.produto.infrastructure.config;

import br.gov.caixa.webmarket.produto.domain.entity.ProdutoEntity;
import br.gov.caixa.webmarket.produto.infrastructure.repository.ProdutoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class CatalogoInicializador {
    @Bean
    CommandLineRunner carregarCatalogoInicial(ProdutoRepository produtos) {
        return args -> {
            inserirSeAusente(produtos, 1, "Notebook", "3500.00", 10);
            inserirSeAusente(produtos, 2, "Teclado", "250.00", 25);
            inserirSeAusente(produtos, 3, "Mouse", "120.00", 30);
        };
    }

    private void inserirSeAusente(
            ProdutoRepository produtos,
            Integer id,
            String nome,
            String preco,
            Integer estoque) {
        if (!produtos.existsById(id)) {
            produtos.save(ProdutoEntity.criar(id, nome, new BigDecimal(preco), estoque));
        }
    }
}
