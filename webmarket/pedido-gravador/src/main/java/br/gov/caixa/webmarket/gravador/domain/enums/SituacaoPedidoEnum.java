package br.gov.caixa.webmarket.pedido.domain.enums;

public enum SituacaoPedidoEnum {
    CRIADO(3, "CRIADO"),
    CONFIRMADO (1, "CONFIRMADO"),
    CANCELADO(2, "CANCELADO");

    private final int codigo;
    private final String descricao;

    SituacaoPedidoEnum(Integer codigo, String descricao) {
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
