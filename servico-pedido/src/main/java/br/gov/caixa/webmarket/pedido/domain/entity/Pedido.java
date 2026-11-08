package br.gov.caixa.webmarket.pedido.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Pedido")
public class Pedido extends PanacheEntityBase {

    @Id
    @SequenceGenerator(
            name = "pedidoSeq",
            sequenceName = "pedido_id_seq",
            allocationSize = 1,
            initialValue = 10 // Começa em 10 para não conflitar com o import.sql
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pedidoSeq")
    public Long id;

    @NotBlank(message = "O nome do cliente é obrigatório")
    @Column(name = "cliente_nome", nullable = false)
    public String clienteNome;

    @Column(name = "data_criacao", nullable = false)
    public LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(nullable = false)
    public String status = "CRIADO"; // CRIADO, CONFIRMADO, CANCELADO

    @Column(name = "valor_total", precision = 10, scale = 2)
    public BigDecimal valorTotal = BigDecimal.ZERO;

    @NotEmpty(message = "O pedido deve ter pelo menos um item")
    @Valid
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    public List<ItemPedido> itens = new ArrayList<>();

    /**
     * Associa os itens ao pedido e calcula o valor total antes de persistir.
     */
    public void prepararItensParaSalvar() {
        BigDecimal total = BigDecimal.ZERO;
        if (itens != null) {
            for (ItemPedido item : itens) {
                item.pedido = this;
                if (item.precoUnitario != null && item.quantidade != null) {
                    total = total.add(item.precoUnitario.multiply(BigDecimal.valueOf(item.quantidade)));
                }
            }
        }
        this.valorTotal = total;
    }

    // =========================================================================
    // Consultas além do CRUD básico (Critério 3 - Persistência com Panache)
    // =========================================================================

    /**
     * Filtra pedidos por status (ex: "CONFIRMADO", "CRIADO").
     */
    public static List<Pedido> findByStatus(String status) {
        return find("status", status).list();
    }

    /**
     * Filtra pedidos por nome do cliente.
     */
    public static List<Pedido> findByCliente(String clienteNome) {
        return find("LOWER(clienteNome) LIKE LOWER(?1)", "%" + clienteNome + "%").list();
    }

    /**
     * Contagem de pedidos por status (agregação Panache).
     */
    public static long countByStatus(String status) {
        return count("status", status);
    }
}