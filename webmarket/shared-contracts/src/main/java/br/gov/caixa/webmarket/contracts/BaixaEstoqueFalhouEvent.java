package br.gov.caixa.webmarket.contracts;

import java.util.UUID;

public record BaixaEstoqueFalhouEvent(
        UUID eventId,
        UUID comandoId,
        UUID identificadorPedido,
        String codigo,
        String motivo,
        String ocorridoEm
) {}
