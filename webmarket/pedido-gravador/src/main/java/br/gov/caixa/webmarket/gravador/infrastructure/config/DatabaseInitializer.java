package br.gov.caixa.webmarket.gravador.infrastructure.config;

import br.gov.caixa.webmarket.gravador.domain.entity.ClienteEntity;
import br.gov.caixa.webmarket.gravador.domain.entity.ProdutoEntity;
import br.gov.caixa.webmarket.gravador.infrastructure.repository.ClienteRepository;
import br.gov.caixa.webmarket.gravador.infrastructure.repository.ProdutoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseInitializer {

    @Bean
    CommandLineRunner carregarDadosIniciais(
            ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository) {
        return args -> {
            inserirClienteSeAusente(clienteRepository, "Ana Souza", "11111111111");
            inserirClienteSeAusente(clienteRepository, "Bruno Lima", "22222222222");

            inserirProdutoSeAusente(produtoRepository, 1, "Notebook", 3500.00f, 10);
            inserirProdutoSeAusente(produtoRepository, 2, "Teclado", 250.00f, 25);
            inserirProdutoSeAusente(produtoRepository, 3, "Mouse", 120.00f, 30);
        };
    }

    private void inserirClienteSeAusente(
            ClienteRepository repository,
            String nome,
            String cpf) {
        if (repository.findByCpf(cpf).isEmpty()) {
            repository.save(new ClienteEntity(nome, cpf));
        }
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
