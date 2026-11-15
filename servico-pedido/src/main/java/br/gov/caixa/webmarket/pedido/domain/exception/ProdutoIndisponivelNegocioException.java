package br.gov.caixa.webmarket.pedido.domain.exception;

public class ProdutoIndisponivelNegocioException extends RuntimeException {
    public ProdutoIndisponivelNegocioException(String mensagem) {
        super(mensagem);
    }
}