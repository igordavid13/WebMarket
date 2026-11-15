package br.gov.caixa.webmarket.pedido.domain.exception;

public class PedidoNaoEncontradoException extends RuntimeException {
    public PedidoNaoEncontradoException(Long id) {
        super("Pedido " + id + " não encontrado");
    }
}