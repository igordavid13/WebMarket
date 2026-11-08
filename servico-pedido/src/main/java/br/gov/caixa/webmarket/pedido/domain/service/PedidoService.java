package br.gov.caixa.webmarket.pedido.domain.service;

import br.gov.caixa.webmarket.pedido.domain.entity.Pedido;
import br.gov.caixa.webmarket.pedido.infrastructure.repository.PedidoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PedidoService {

    @Inject
    PedidoRepository pedidoRepository;

    public List<Pedido> listar(String status, String cliente) {
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
        return pedidoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado com id: " + id));
    }

    @Transactional
    public Pedido criar(Pedido pedido) {
        pedido.setId(null);
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
    public boolean excluir(Long id) {
        return pedidoRepository.deleteById(id);
    }
}