package br.gov.caixa.webmarket.produto.domain.exception;

public class EstoqueInsuficienteException extends RuntimeException {
    public EstoqueInsuficienteException(Long id, int disponivel) {
        super("Estoque insuficiente para o produto " + id + " (disponível: " + disponivel + ")");
    }
}