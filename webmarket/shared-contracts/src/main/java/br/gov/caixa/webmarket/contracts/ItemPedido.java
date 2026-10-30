package br.gov.caixa.webmarket.contracts;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemPedido(
    @NotNull @Positive Integer produto,
    @NotNull @Positive Integer quantidade
) {}
