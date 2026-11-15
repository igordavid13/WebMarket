package br.gov.caixa.webmarket.produto.domain.service;

import br.gov.caixa.webmarket.produto.domain.entity.Produto;
import br.gov.caixa.webmarket.produto.domain.exception.EstoqueInsuficienteException;
import br.gov.caixa.webmarket.produto.domain.exception.ProdutoDuplicadoException;
import br.gov.caixa.webmarket.produto.domain.exception.ProdutoNaoEncontradoException;
import br.gov.caixa.webmarket.produto.infrastructure.repository.ProdutoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class ProdutoService {

    @Inject
    ProdutoRepository produtoRepository;

    public List<Produto> listar(boolean somenteDisponiveis) {
        return somenteDisponiveis ? produtoRepository.findComEstoqueDisponivel() : produtoRepository.listAll();
    }

    public Produto buscarPorId(Long id) {
        return produtoRepository.findByIdOptional(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    public Produto verificarDisponibilidade(Long id, Integer quantidade) {
        Produto produto = buscarPorId(id);
        if (produto.getQuantidadeEstoque() < quantidade) {
            throw new EstoqueInsuficienteException(id, produto.getQuantidadeEstoque());
        }
        return produto;
    }

    @Transactional
    public Produto cadastrar(Produto produto) {
        if (produtoRepository.findByNome(produto.getNome()).isPresent()) {
            throw new ProdutoDuplicadoException(produto.getNome());
        }
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
    public void excluir(Long id) {
        if (!produtoRepository.deleteById(id)) {
            throw new ProdutoNaoEncontradoException(id);
        }
    }
}