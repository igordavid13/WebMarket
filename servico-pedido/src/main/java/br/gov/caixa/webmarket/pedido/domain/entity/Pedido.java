package br.gov.caixa.webmarket.pedido.domain.entity;

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
public class Pedido {

    @Id
    @SequenceGenerator(
            name = "pedidoSeq",
            sequenceName = "pedido_id_seq",
            allocationSize = 1,
            initialValue = 10
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pedidoSeq")
    private Long id;

    @NotBlank(message = "O nome do cliente é obrigatório")
    @Column(name = "cliente_nome", nullable = false)
    private String clienteNome;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(nullable = false)
    private String status = "CRIADO";

    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @NotEmpty(message = "O pedido deve ter pelo menos um item")
    @Valid
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemPedido> itens = new ArrayList<>();

    public Pedido() {
    }

    public void prepararItensParaSalvar() {
        BigDecimal total = BigDecimal.ZERO;
        if (itens != null) {
            for (ItemPedido item : itens) {
                item.setPedido(this);
                if (item.getPrecoUnitario() != null && item.getQuantidade() != null) {
                    total = total.add(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
                }
            }
        }
        this.valorTotal = total;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public List<ItemPedido> getItens() { return itens; }
    public void setItens(List<ItemPedido> itens) { this.itens = itens; }
}