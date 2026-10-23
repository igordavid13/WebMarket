package br.gov.caixa.webmarket.gravador.domain.entity;

import br.gov.caixa.webmarket.gravador.domain.enums.SituacaoPedidoEnum;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PedidoEntityTest {
    @Test
    void confirmarAdicionaSituacaoUmaUnicaVez() {
        PedidoEntity pedido = PedidoEntity.de(UUID.randomUUID(), new ClienteEntity("Ana Souza", "11111111111"));

        pedido.confirmar();
        pedido.confirmar();

        assertEquals(2, pedido.getSituacao().size());
        assertEquals(SituacaoPedidoEnum.CRIADO,
                pedido.getSituacao().get(0).getSituacao());
        assertEquals(SituacaoPedidoEnum.CONFIRMADO,
                pedido.getSituacao().get(1).getSituacao());
    }
}
