package br.gov.caixa.webmarket.produto.infrastructure.messaging;

import br.gov.caixa.webmarket.contracts.BaixarEstoqueCommand;
import br.gov.caixa.webmarket.produto.application.service.BaixaEstoqueService;
import br.gov.caixa.webmarket.produto.infrastructure.config.MessagingConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class BaixaEstoqueListener {
    private final BaixaEstoqueService baixaEstoque;

    public BaixaEstoqueListener(BaixaEstoqueService baixaEstoque) {
        this.baixaEstoque = baixaEstoque;
    }

    @RabbitListener(queues = MessagingConfig.QUEUE)
    public void consumir(BaixarEstoqueCommand comando) {
        baixaEstoque.processar(comando);
    }
}
