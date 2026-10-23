package br.gov.caixa.webmarket.contracts;

import java.util.UUID;

public record GravarPedidoCommand(
        UUID identificadorPedido,
        PedidoRequest dados
) {}