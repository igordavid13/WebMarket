package br.gov.caixa.webmarket.pedido.infrastructure.repository;

import br.gov.caixa.webmarket.pedido.domain.entity.Pedido;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;

@QuarkusTest
public class PedidoRepositoryTest {

    @Inject
    PedidoRepository pedidoRepository;

    @Test
    @DisplayName("Critério 3: Deve consultar pedidos por status no repositório")
    public void deveBuscarPedidosPorStatus() {
        List<Pedido> confirmados = pedidoRepository.findByStatus("CONFIRMADO");
        Assertions.assertFalse(confirmados.isEmpty(), "Deveria encontrar pedidos CONFIRMADOS do import.sql");
        Assertions.assertTrue(confirmados.stream().allMatch(p -> "CONFIRMADO".equals(p.getStatus())));
    }

    @Test
    @DisplayName("Critério 3: Deve contar pedidos por status")
    public void deveContarPedidosPorStatus() {
        long contagem = pedidoRepository.countByStatus("CONFIRMADO");
        Assertions.assertTrue(contagem >= 1);
    }
}