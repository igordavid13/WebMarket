package br.gov.caixa.webmarket.contracts;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ItemPedidoView(
    Long idItemPedido,
    Long idProduto
) {}
