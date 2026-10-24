package br.gov.caixa.webmarket.gravador.infrastructure.config;

import br.gov.caixa.webmarket.gravador.domain.entity.ProdutoEntity;
import br.gov.caixa.webmarket.gravador.infrastructure.repository.ProdutoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseInitializer {

    @Bean
    CommandLineRunner carregarDadosIniciais(
            ProdutoRepository produtoRepository) {
        return args -> {
            inserirProdutoSeAusente(produtoRepository, 1, "Notebook", 3500.00f, 10);
            inserirProdutoSeAusente(produtoRepository, 2, "Teclado", 250.00f, 25);
            inserirProdutoSeAusente(produtoRepository, 3, "Mouse", 120.00f, 30);
        };
    }

    private void inserirProdutoSeAusente(
            ProdutoRepository repository,
            Integer id,
            String nome,
            Float preco,
            Integer quantidade) {
        if (repository.findById(id).isEmpty()) {
            repository.save(ProdutoEntity.de(id, nome, preco, quantidade));
        }
    }
}
