package br.gov.caixa.webmarket.gravador.application.service;

import br.gov.caixa.webmarket.contracts.PedidoGravadoEvent;
import br.gov.caixa.webmarket.contracts.PedidoView;
import br.gov.caixa.webmarket.contracts.ItemPedidoView;
import br.gov.caixa.webmarket.contracts.GravarPedidoCommand;
import br.gov.caixa.webmarket.gravador.infrastructure.messaging.EventoPublisher;
import br.gov.caixa.webmarket.gravador.infrastructure.repository.ClienteRepository;
import br.gov.caixa.webmarket.gravador.infrastructure.repository.PedidoRepository;
import br.gov.caixa.webmarket.gravador.infrastructure.repository.ProdutoRepository;
import br.gov.caixa.webmarket.gravador.domain.entity.ClienteEntity;
import br.gov.caixa.webmarket.gravador.domain.entity.ItemPedidoEntity;
import br.gov.caixa.webmarket.gravador.domain.entity.PedidoEntity;
import br.gov.caixa.webmarket.contracts.ItemPedidoRequest;
import br.gov.caixa.webmarket.contracts.PedidoRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GravadorService {

    private final PedidoRepository repository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final EventoPublisher eventos;

    public GravadorService(PedidoRepository repository, ClienteRepository clienteRepository,
                           ProdutoRepository produtoRepository, EventoPublisher eventos) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.eventos = eventos;
    }

    @Transactional
    public boolean gravar(GravarPedidoCommand command) {
        if (repository.existsById(command.identificadorPedido())) {
            return false;
        }
        PedidoRequest request = command.dados();
        ClienteEntity cliente = clienteRepository.findById(request.idCliente())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cliente não encontrado: " + request.idCliente()));
        PedidoEntity pedido = PedidoEntity.de(command.identificadorPedido(), cliente);
        request.itens().forEach(item -> pedido.adicionarItem(criarItem(item, pedido)));
        repository.save(pedido);
        eventos.publicar(new PedidoGravadoEvent(command.identificadorPedido(), LocalDateTime.now()));
        return true;
    }

    private ItemPedidoEntity criarItem(ItemPedidoRequest item, PedidoEntity pedido) {
        var produto = produtoRepository.findById(item.produto())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Produto não encontrado: " + item.produto()));
        produto.retirarQuantidade(item.quantidade());
        return new ItemPedidoEntity(pedido, produto, BigDecimal.valueOf(produto.getPreco()), item.quantidade());
    }

    public Optional<PedidoView> buscar(UUID id) {
        return repository.findById(id).map(this::toView);
    }

    private PedidoView toView(PedidoEntity e) {
        List<ItemPedidoView> itens = e.getItens().stream()
                .map(item -> new ItemPedidoView(item.getId(), item.getProduto().getId().longValue()))
                .toList();
        return new PedidoView(e.getId(), e.getCliente().getId(), itens,
                e.getDataPedido());
    }
}
