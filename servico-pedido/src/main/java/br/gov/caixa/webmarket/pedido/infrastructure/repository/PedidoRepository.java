package br.gov.caixa.webmarket.pedido.infrastructure.repository;

import br.gov.caixa.webmarket.pedido.domain.entity.Pedido;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class PedidoRepository implements PanacheRepository<Pedido> {

    /**
     * Consultas além do CRUD básico (Critério 3).
     */
    public List<Pedido> findByStatus(String status) {
        return find("status", status).list();
    }

    public List<Pedido> findByCliente(String clienteNome) {
        return find("LOWER(clienteNome) LIKE LOWER(?1)", "%" + clienteNome + "%").list();
    }

    public long countByStatus(String status) {
        return count("status", status);
    }
}