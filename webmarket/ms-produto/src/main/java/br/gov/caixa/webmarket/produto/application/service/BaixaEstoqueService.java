package br.gov.caixa.webmarket.produto.application.service;

import br.gov.caixa.webmarket.contracts.BaixaEstoqueFalhouEvent;
import br.gov.caixa.webmarket.contracts.BaixarEstoqueCommand;
import br.gov.caixa.webmarket.contracts.EstoqueBaixadoEvent;
import br.gov.caixa.webmarket.contracts.ItemEstoqueCommand;
import br.gov.caixa.webmarket.produto.domain.entity.EventoProcessadoEntity;
import br.gov.caixa.webmarket.produto.domain.entity.OutboxEventoEntity;
import br.gov.caixa.webmarket.produto.domain.entity.ProdutoEntity;
import br.gov.caixa.webmarket.produto.infrastructure.repository.EventoProcessadoRepository;
import br.gov.caixa.webmarket.produto.infrastructure.repository.OutboxEventoRepository;
import br.gov.caixa.webmarket.produto.infrastructure.repository.ProdutoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BaixaEstoqueService {
    private static final String TOPICO_SUCESSO = "estoque-baixado";
    private static final String TOPICO_FALHA = "baixa-estoque-falhou";

    private final ProdutoRepository produtos;
    private final EventoProcessadoRepository eventosProcessados;
    private final OutboxEventoRepository outbox;
    private final ProdutoCatalogoService catalogo;
    private final ObjectMapper objectMapper;

    public BaixaEstoqueService(
            ProdutoRepository produtos,
            EventoProcessadoRepository eventosProcessados,
            OutboxEventoRepository outbox,
            ProdutoCatalogoService catalogo,
            ObjectMapper objectMapper) {
        this.produtos = produtos;
        this.eventosProcessados = eventosProcessados;
        this.outbox = outbox;
        this.catalogo = catalogo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processar(BaixarEstoqueCommand comando) {
        validarComando(comando);
        if (eventosProcessados.existsById(comando.eventId())) {
            return;
        }

        List<ItemEstoqueCommand> itens = consolidarItens(comando.itens());
        List<Integer> ids = itens.stream().map(ItemEstoqueCommand::produtoId).toList();
        List<ProdutoEntity> encontrados = produtos.buscarTodosParaAtualizacao(ids);
        Map<Integer, ProdutoEntity> porId = encontrados.stream()
                .collect(Collectors.toMap(ProdutoEntity::getId, Function.identity()));

        String falha = validarEstoque(itens, porId);
        eventosProcessados.save(new EventoProcessadoEntity(comando.eventId()));

        if (falha != null) {
            gravarFalha(comando, falha);
            return;
        }

        itens.forEach(item -> porId.get(item.produtoId()).retirarQuantidade(item.quantidade()));
        produtos.saveAll(encontrados);

        EstoqueBaixadoEvent evento = new EstoqueBaixadoEvent(
                UUID.randomUUID(),
                comando.eventId(),
                comando.identificadorPedido(),
                itens,
                LocalDateTime.now().toString());
        gravarOutbox(evento.eventId(), TOPICO_SUCESSO, evento);
        invalidarCacheAposCommit(ids);
    }

    private void validarComando(BaixarEstoqueCommand comando) {
        if (comando == null || comando.eventId() == null || comando.identificadorPedido() == null
                || comando.itens() == null || comando.itens().isEmpty()) {
            throw new IllegalArgumentException("Comando de baixa inválido");
        }
        if (comando.itens().stream().anyMatch(item -> item == null || item.produtoId() == null
                || item.quantidade() == null || item.quantidade() <= 0)) {
            throw new IllegalArgumentException("Cada item deve informar produto e quantidade positiva");
        }
    }

        private List<ItemEstoqueCommand> consolidarItens(List<ItemEstoqueCommand> itens) {
        try {
            return itens.stream()
                    .collect(Collectors.toMap(
                    ItemEstoqueCommand::produtoId,
                    ItemEstoqueCommand::quantidade,
                            Math::addExact,
                            java.util.TreeMap::new))
                    .entrySet().stream()
                .map(entry -> new ItemEstoqueCommand(entry.getKey(), entry.getValue()))
                    .toList();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Quantidade total do produto excede o limite permitido", exception);
        }
    }

    private String validarEstoque(List<ItemEstoqueCommand> itens, Map<Integer, ProdutoEntity> porId) {
        for (ItemEstoqueCommand item : itens) {
            ProdutoEntity produto = porId.get(item.produtoId());
            if (produto == null) {
                return "Produto não encontrado: " + item.produtoId();
            }
            if (!produto.isAtivo()) {
                return "Produto inativo: " + item.produtoId();
            }
            if (produto.getQuantidadeEstoque() < item.quantidade()) {
                return "Estoque insuficiente para o produto: " + item.produtoId();
            }
        }
        return null;
    }

    private void gravarFalha(BaixarEstoqueCommand comando, String motivo) {
        BaixaEstoqueFalhouEvent evento = new BaixaEstoqueFalhouEvent(
                UUID.randomUUID(),
                comando.eventId(),
                comando.identificadorPedido(),
                "BAIXA_NAO_REALIZADA",
                motivo,
                LocalDateTime.now().toString());
        gravarOutbox(evento.eventId(), TOPICO_FALHA, evento);
    }

    private void gravarOutbox(UUID eventId, String destino, Object evento) {
        try {
            outbox.save(new OutboxEventoEntity(eventId, destino, objectMapper.writeValueAsString(evento)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível serializar evento de estoque", exception);
        }
    }

    private void invalidarCacheAposCommit(List<Integer> ids) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ids.forEach(catalogo::invalidarCache);
                }
            });
        }
    }

}
