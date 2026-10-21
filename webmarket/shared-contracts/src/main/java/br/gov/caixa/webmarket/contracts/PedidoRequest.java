package br.gov.caixa.webmarket.contracts;

import java.time.LocalDateTime;
import java.util.List;

public record PedidoRequest (
    Integer cliente,
    List<ItemPedido> itens,
    LocalDateTime dataPedido
) {}
