package br.gov.caixa.webmarket.contracts;

import java.util.List;
import java.util.UUID;

public record BaixarEstoqueCommand(
        UUID eventId,
        UUID identificadorPedido,
        List<ItemEstoqueCommand> itens
) {}