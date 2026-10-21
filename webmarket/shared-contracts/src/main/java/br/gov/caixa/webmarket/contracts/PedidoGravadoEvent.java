package br.gov.caixa.webmarket.contracts;

import java.time.LocalDateTime;
import java.util.UUID;

public record PedidoGravadoEvent (
    UUID identificadorPedido,
    LocalDateTime dataPedido
) {}
