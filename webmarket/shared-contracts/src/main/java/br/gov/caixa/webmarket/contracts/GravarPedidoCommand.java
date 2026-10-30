package br.gov.caixa.webmarket.contracts;

import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record GravarPedidoCommand(
        @NotNull UUID identificadorPedido,
        @NotNull @Valid PedidoRequest dados
) {}