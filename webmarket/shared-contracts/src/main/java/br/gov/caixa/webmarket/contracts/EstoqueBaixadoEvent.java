package br.gov.caixa.webmarket.contracts;

import java.util.List;
import java.util.UUID;

public record EstoqueBaixadoEvent(
        UUID eventId,
        UUID comandoId,
        UUID identificadorPedido,
        List<ItemEstoqueCommand> itens,
        String ocorridoEm
) {}