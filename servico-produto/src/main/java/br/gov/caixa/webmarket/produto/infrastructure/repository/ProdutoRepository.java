package br.gov.caixa.webmarket.produto.infrastructure.repository;

import br.gov.caixa.webmarket.produto.domain.entity.Produto;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProdutoRepository implements PanacheRepository<Produto> {

    /**
     * Consulta derivada que virou método explícito com Panache.
     */
    public List<Produto> findComEstoqueDisponivel() {
        return find("quantidadeEstoque > 0").list();
    }

    public Optional<Produto> findByNome(String nome) {
        return find("LOWER(nome) = LOWER(?1)", nome).firstResultOptional();
    }

    public Optional<Produto> findDisponivelParaVenda(Long id, Integer quantidade) {
        return find("id = ?1 and quantidadeEstoque >= ?2", id, quantidade).firstResultOptional();
    }
}