package br.gov.caixa.webmarket.contracts;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SharedContractsApplicationTests {

    @Test
    void preservaIdentificadorEDadosDoPedido() {
        UUID id = UUID.randomUUID();
        PedidoRequest dados = new PedidoRequest(1, List.of(new ItemPedido(10, 2)), LocalDateTime.now());
        GravarPedidoCommand command = new GravarPedidoCommand(id, dados);
        assertEquals(id, command.identificadorPedido());
        assertEquals(dados, command.dados());
    }

}
