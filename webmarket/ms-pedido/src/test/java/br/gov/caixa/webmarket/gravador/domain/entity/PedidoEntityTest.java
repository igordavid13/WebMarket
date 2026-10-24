package br.gov.caixa.webmarket.gravador.domain.entity;

import br.gov.caixa.webmarket.gravador.domain.enums.SituacaoPedido;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PedidoEntityTest {
    @Test
    void confirmarAdicionaSituacaoUmaUnicaVez() {
        PedidoEntity pedido = PedidoEntity.de(UUID.randomUUID(), 1L);

        pedido.confirmar();
        pedido.confirmar();

        assertEquals(SituacaoPedido.CONFIRMADO, pedido.getSituacao());
    }
}
