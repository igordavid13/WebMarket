package br.gov.caixa.webmarket.pedido.domain.exception;

import jakarta.ws.rs.WebApplicationException;

public class ProdutoIndisponivelNegocioException extends WebApplicationException {
    public ProdutoIndisponivelNegocioException(String mensagem) {
        super(mensagem, 422);
    }
}