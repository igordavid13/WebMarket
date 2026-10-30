package br.gov.caixa.webmarket.contracts;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PedidoRequest (
    @NotNull @Positive Integer cliente,
    @NotEmpty List<@NotNull @Valid ItemPedido> itens,
    @NotNull LocalDateTime dataPedido
) {}
