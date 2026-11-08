package br.gov.caixa.webmarket.produto.domain.service;

import br.gov.caixa.webmarket.produto.domain.entity.Produto;
import br.gov.caixa.webmarket.produto.infrastructure.repository.ProdutoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProdutoService {

    @Inject
    ProdutoRepository produtoRepository;

    public List<Produto> listar(boolean somenteDisponiveis) {
        if (somenteDisponiveis) {
            return produtoRepository.findComEstoqueDisponivel();
        }
        return produtoRepository.listAll();
    }

    public Produto buscarPorId(Long id) {
        return produtoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado com id: " + id));
    }

    public Optional<Produto> buscarPorIdOpcional(Long id) {
        return produtoRepository.findByIdOptional(id);
    }

    @Transactional
    public Produto cadastrar(Produto produto) {
        produto.setId(null);
        produtoRepository.persist(produto);
        return produto;
    }

    @Transactional
    public Produto atualizar(Long id, Produto dadosAtualizados) {
        Produto existente = buscarPorId(id);
        existente.setNome(dadosAtualizados.getNome());
        existente.setPreco(dadosAtualizados.getPreco());
        existente.setQuantidadeEstoque(dadosAtualizados.getQuantidadeEstoque());
        return existente;
    }

    @Transactional
    public boolean excluir(Long id) {
        return produtoRepository.deleteById(id);
    }

    public boolean temEstoqueSuficiente(Long id, Integer quantidade) {
        return produtoRepository.findDisponivelParaVenda(id, quantidade).isPresent();
    }
}