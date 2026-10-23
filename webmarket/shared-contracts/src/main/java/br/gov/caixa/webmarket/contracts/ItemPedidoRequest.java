package br.gov.caixa.webmarket.contracts;

public record ItemPedidoRequest(
    Integer produto,
    Integer quantidade
) {}
