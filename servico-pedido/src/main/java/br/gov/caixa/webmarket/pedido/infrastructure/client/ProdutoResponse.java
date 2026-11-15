package br.gov.caixa.webmarket.pedido.infrastructure.client;

import java.math.BigDecimal;

public record ProdutoResponse(
        Long id,
        String nome,
        BigDecimal preco,
        Integer quantidadeEstoque
) {}