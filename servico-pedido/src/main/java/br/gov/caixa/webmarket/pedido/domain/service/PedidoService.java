package br.gov.caixa.webmarket.pedido.domain.service;

import br.gov.caixa.webmarket.pedido.domain.entity.ItemPedido;
import br.gov.caixa.webmarket.pedido.domain.entity.Pedido;
import br.gov.caixa.webmarket.pedido.domain.exception.PedidoNaoEncontradoException;
import br.gov.caixa.webmarket.pedido.infrastructure.client.ProdutoGateway;
import br.gov.caixa.webmarket.pedido.infrastructure.repository.PedidoRepository;
import br.gov.caixa.webmarket.pedido.infrastructure.security.Papeis;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PedidoService {

    @Inject
    PedidoRepository pedidoRepository;

    @Inject
    ProdutoGateway produtoGateway;

    @Inject
    SecurityIdentity identidade;

    public List<Pedido> listar(String status, String cliente) {
        if (ehCliente()) {
            String usuarioAtual = usuarioLogado();
            if (cliente != null && !cliente.isBlank() && !cliente.equalsIgnoreCase(usuarioAtual)) {
                throw new ForbiddenException("Cliente só pode listar os próprios pedidos");
            }
            List<Pedido> meusPedidos = pedidoRepository.findByCliente(usuarioAtual);
            if (status != null && !status.isBlank()) {
                return meusPedidos.stream()
                        .filter(p -> status.equalsIgnoreCase(p.getStatus()))
                        .toList();
            }
            return meusPedidos;
        }

        if (status != null && !status.isBlank()) {
            return pedidoRepository.findByStatus(status.toUpperCase());
        }
        if (cliente != null && !cliente.isBlank()) {
            return pedidoRepository.findByCliente(cliente);
        }
        return pedidoRepository.listAll();
    }

    public Optional<Pedido> buscarPorIdOpcional(Long id) {
        return pedidoRepository.findByIdOptional(id);
    }

    public Pedido buscarPorId(Long id) {
        Pedido pedido = pedidoRepository.findByIdOptional(id)
                .orElseThrow(() -> new PedidoNaoEncontradoException(id));
        exigirPosse(pedido.getClienteNome(), "O pedido pertence a outro cliente");
        return pedido;
    }

    @Transactional
    public Pedido criar(Pedido pedido) {
        exigirPosse(pedido.getClienteNome(), "Cliente só pode criar pedidos em seu próprio nome");

        if (pedido.getItens() != null) {
            for (ItemPedido item : pedido.getItens()) {
                var produtoRemoto = produtoGateway.confirmarDisponibilidade(
                        item.getProdutoId(), item.getQuantidade());
                if (item.getPrecoUnitario() == null) {
                    item.setPrecoUnitario(produtoRemoto.preco());
                }
            }
        }

        pedido.setId(null);
        pedido.setStatus("CONFIRMADO");
        pedido.prepararItensParaSalvar();
        pedidoRepository.persist(pedido);
        return pedido;
    }

    @Transactional
    public Pedido atualizarStatus(Long id, String novoStatus) {
        Pedido pedido = buscarPorId(id);
        pedido.setStatus(novoStatus.toUpperCase());
        return pedido;
    }

    @Transactional
    public void excluir(Long id) {
        if (!pedidoRepository.deleteById(id)) {
            throw new PedidoNaoEncontradoException(id);
        }
    }

    private void exigirPosse(String donoDoDado, String mensagem) {
        if (ehCliente() && (donoDoDado == null || !usuarioLogado().equalsIgnoreCase(donoDoDado))) {
            throw new ForbiddenException(mensagem);
        }
    }

    private boolean ehCliente() {
        return identidade != null && identidade.hasRole(Papeis.CLIENTE);
    }

    private String usuarioLogado() {
        return identidade.getPrincipal().getName();
    }
}