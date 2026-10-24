package br.gov.caixa.webmarket.gravador.domain.enums;

public enum SituacaoPedido {
    CRIADO(3, "CRIADO"),
    CONFIRMADO (1, "CONFIRMADO"),
    CANCELADO(2, "CANCELADO"),
    CANCELADO_POR_FALHA_ESTOQUE(4, "CANCELADO_POR_FALHA_ESTOQUE");

    private final int codigo;
    private final String descricao;

    SituacaoPedido(Integer codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }
}
