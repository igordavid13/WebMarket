package br.gov.caixa.webmarket.gravador.application.service;

import br.gov.caixa.webmarket.contracts.BaixarEstoqueCommand;
import br.gov.caixa.webmarket.contracts.PedidoView;
import br.gov.caixa.webmarket.contracts.ItemPedidoView;
import br.gov.caixa.webmarket.contracts.GravarPedidoCommand;
import br.gov.caixa.webmarket.contracts.ItemEstoqueCommand;
import br.gov.caixa.webmarket.gravador.domain.entity.PedidoOutboxEntity;
import br.gov.caixa.webmarket.gravador.infrastructure.repository.PedidoOutboxRepository;
import br.gov.caixa.webmarket.gravador.infrastructure.config.MessagingConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.gov.caixa.webmarket.gravador.infrastructure.repository.PedidoRepository;
import br.gov.caixa.webmarket.gravador.infrastructure.repository.ProdutoRepository;
import br.gov.caixa.webmarket.gravador.domain.entity.ItemPedidoEntity;
import br.gov.caixa.webmarket.gravador.domain.entity.PedidoEntity;
import br.gov.caixa.webmarket.contracts.ItemPedidoRequest;
import br.gov.caixa.webmarket.contracts.PedidoRequest;
import br.gov.caixa.webmarket.contracts.EstoqueBaixadoEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GravadorService {

    private final PedidoRepository repository;
    private final ProdutoRepository produtoRepository;
    private final PedidoOutboxRepository outbox;
    private final ObjectMapper objectMapper;

    public GravadorService(PedidoRepository repository,
                           ProdutoRepository produtoRepository,
                           PedidoOutboxRepository outbox,
                           ObjectMapper objectMapper) {
        this.repository = repository;
        this.produtoRepository = produtoRepository;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean gravar(GravarPedidoCommand command) {
        if (repository.existsById(command.identificadorPedido())) {
            return false;
        }

        PedidoRequest request = command.dados();
        PedidoEntity pedido = PedidoEntity.de(
                command.identificadorPedido(), request.idCliente());
        request.itens().forEach(item -> pedido.adicionarItem(criarItem(item, pedido)));
        repository.save(pedido);

        BaixarEstoqueCommand baixarEstoque = new BaixarEstoqueCommand(
            command.identificadorPedido(),
            command.identificadorPedido(),
            request.itens().stream()
                .map(item -> new ItemEstoqueCommand(item.produto(), item.quantidade()))
                .toList());
        try {
            outbox.save(PedidoOutboxEntity.pendente(
                    MessagingConfig.EXCHANGE,
                    objectMapper.writeValueAsString(baixarEstoque)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível registrar o comando de baixa", exception);
        }
        return true;
    }

    private ItemPedidoEntity criarItem(ItemPedidoRequest item, PedidoEntity pedido) {
        var produto = produtoRepository.findById(item.produto())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Produto não encontrado: " + item.produto()));
        return new ItemPedidoEntity(pedido, produto, BigDecimal.valueOf(produto.getPreco()), item.quantidade());
    }

    public Optional<PedidoView> buscar(UUID id) {
        return repository.findById(id).map(this::toView);
    }

    @Transactional
    public void confirmarPedido(EstoqueBaixadoEvent evento) {
        PedidoEntity pedido = repository.findById(evento.identificadorPedido())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pedido não encontrado para confirmação: " + evento.identificadorPedido()));
        pedido.confirmar();
        repository.save(pedido);
    }

    private PedidoView toView(PedidoEntity e) {
        List<ItemPedidoView> itens = e.getItens().stream()
                .map(item -> new ItemPedidoView(item.getId(), item.getProduto().getId().longValue()))
                .toList();
        return new PedidoView(e.getId(), e.getIdCliente(), itens,
                e.getDataPedido());
    }
}
