package br.gov.caixa.webmarket.produto.domain.exception;

public class ProdutoNaoEncontradoException extends RuntimeException {
    public ProdutoNaoEncontradoException(Long id) {
        super("Produto " + id + " não encontrado");
    }
}