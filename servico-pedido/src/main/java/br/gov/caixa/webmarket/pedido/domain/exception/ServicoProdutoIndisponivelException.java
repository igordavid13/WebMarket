package br.gov.caixa.webmarket.pedido.domain.exception;

public class ServicoProdutoIndisponivelException extends RuntimeException {
    public ServicoProdutoIndisponivelException() {
        super("Serviço de produtos indisponível; tente novamente em instantes");
    }
}