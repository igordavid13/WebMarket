package br.gov.caixa.webmarket.produto.application.view;

import br.gov.caixa.webmarket.produto.domain.entity.ProdutoEntity;

import java.math.BigDecimal;

public record ProdutoView(
        Integer id,
        String nome,
        BigDecimal preco,
        Integer quantidadeEstoque,
        boolean ativo
) {
    public static ProdutoView de(ProdutoEntity produto) {
        return new ProdutoView(
                produto.getId(),
                produto.getNome(),
                produto.getPreco(),
                produto.getQuantidadeEstoque(),
                produto.isAtivo());
    }
}
