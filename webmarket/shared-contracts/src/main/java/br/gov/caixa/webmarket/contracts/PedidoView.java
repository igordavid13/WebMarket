package br.gov.caixa.webmarket.contracts;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PedidoView (
    UUID identificadorPedido,
    Integer cliente,
    List<ItemPedidoView> itens,
    LocalDateTime dataPedido
) {}
