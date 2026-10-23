package br.gov.caixa.webmarket.produto.application.service;

import br.gov.caixa.webmarket.contracts.BaixarEstoqueCommand;
import br.gov.caixa.webmarket.contracts.ItemEstoqueCommand;
import br.gov.caixa.webmarket.produto.domain.entity.OutboxEventoEntity;
import br.gov.caixa.webmarket.produto.domain.entity.ProdutoEntity;
import br.gov.caixa.webmarket.produto.infrastructure.repository.EventoProcessadoRepository;
import br.gov.caixa.webmarket.produto.infrastructure.repository.OutboxEventoRepository;
import br.gov.caixa.webmarket.produto.infrastructure.repository.ProdutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaixaEstoqueServiceTest {
    @Mock
    private ProdutoRepository produtos;
    @Mock
    private EventoProcessadoRepository eventosProcessados;
    @Mock
    private OutboxEventoRepository outbox;
    @Mock
    private ProdutoCatalogoService catalogo;

    private BaixaEstoqueService service;

    @BeforeEach
    void setUp() {
        service = new BaixaEstoqueService(
                produtos,
                eventosProcessados,
                outbox,
                catalogo,
                new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void deveBaixarEstoqueERegistrarEventoDeSucesso() {
        TransactionSynchronizationManager.initSynchronization();
        UUID commandId = UUID.randomUUID();
        UUID pedidoId = UUID.randomUUID();
        ProdutoEntity produto = ProdutoEntity.criar(7, "Mouse", new BigDecimal("120.00"), 5);
        when(eventosProcessados.existsById(commandId)).thenReturn(false);
        when(produtos.buscarTodosParaAtualizacao(List.of(7))).thenReturn(List.of(produto));

        service.processar(new BaixarEstoqueCommand(
                commandId, pedidoId, List.of(new ItemEstoqueCommand(7, 2))));

        assertEquals(3, produto.getQuantidadeEstoque());
        verify(produtos).saveAll(List.of(produto));
        verify(eventosProcessados).save(any());
        ArgumentCaptor<OutboxEventoEntity> evento = ArgumentCaptor.forClass(OutboxEventoEntity.class);
        verify(outbox).save(evento.capture());
        assertEquals("estoque-baixado", evento.getValue().getDestino());
        assertTrue(TransactionSynchronizationManager.getSynchronizations().stream()
            .anyMatch(synchronization -> synchronization instanceof TransactionSynchronization));
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(catalogo).invalidarCache(7);
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void comandoDuplicadoNaoBaixaEstoqueNovamente() {
        UUID commandId = UUID.randomUUID();
        when(eventosProcessados.existsById(commandId)).thenReturn(true);

        service.processar(new BaixarEstoqueCommand(
                commandId, UUID.randomUUID(), List.of(new ItemEstoqueCommand(7, 2))));

        verify(produtos, never()).buscarTodosParaAtualizacao(any());
        verify(produtos, never()).saveAll(any());
        verify(outbox, never()).save(any());
    }

    @Test
    void estoqueInsuficienteNaoAlteraNenhumProdutoERegistraFalha() throws Exception {
        UUID commandId = UUID.randomUUID();
        ProdutoEntity produto = ProdutoEntity.criar(7, "Mouse", new BigDecimal("120.00"), 1);
        when(eventosProcessados.existsById(commandId)).thenReturn(false);
        when(produtos.buscarTodosParaAtualizacao(List.of(7))).thenReturn(List.of(produto));

        service.processar(new BaixarEstoqueCommand(
                commandId, UUID.randomUUID(), List.of(new ItemEstoqueCommand(7, 2))));

        assertEquals(1, produto.getQuantidadeEstoque());
        verify(produtos, never()).saveAll(any());
        ArgumentCaptor<OutboxEventoEntity> evento = ArgumentCaptor.forClass(OutboxEventoEntity.class);
        verify(outbox).save(evento.capture());
        assertEquals("baixa-estoque-falhou", evento.getValue().getDestino());
        assertTrue(new ObjectMapper().readTree(evento.getValue().getPayload()).has("motivo"));
        verify(catalogo, never()).invalidarCache(7);
    }
}
