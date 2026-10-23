package br.gov.caixa.webmarket.contracts;

public record ItemEstoqueCommand(
        Integer produtoId,
        Integer quantidade
) {}