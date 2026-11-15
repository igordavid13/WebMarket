package br.gov.caixa.webmarket.pedido.domain.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class ServicoProdutoIndisponivelException extends WebApplicationException {
    public ServicoProdutoIndisponivelException() {
        super("Serviço de produtos temporariamente indisponível", Response.Status.SERVICE_UNAVAILABLE);
    }
}