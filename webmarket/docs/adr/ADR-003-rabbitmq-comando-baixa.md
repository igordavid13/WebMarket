# ADR-003: Uso de RabbitMQ para Enfileiramento de Comandos de Baixa de Estoque

## Status
Aceito

## Contexto
Após o registro do pedido no `ms-pedido`, o estoque físico precisa ser debitado pelo `ms-produto`. Essa ação é uma ordem de trabalho direta (*command*), com destinatário único, que não deve bloquear a resposta HTTP ao cliente e precisa tolerar a indisponibilidade temporária do serviço de produto.

## Decisão
Utilizar o RabbitMQ para publicação e consumo do comando imperativo `BaixarEstoqueCommand`:
- **Exchange:** `webmarket.commands` (Direct, durável)
- **Fila:** `estoque.baixar.q` (Durável, com confirmação manual/transacional)
- **DLQ:** `gravacao.dlq` para mensagens não processáveis

## Alternativas consideradas
- **HTTP REST síncrono:** Rejeitado porque se o `ms-produto` estivesse fora do ar ou com lentidão, o cliente não conseguiria finalizar o pedido.
- **Kafka para o comando:** Rejeitado porque o comando tem semântica ponto-a-ponto com um único consumidor lógico, necessitando de controle de fila e DLQ por mensagem.

## Consequências
- **Ganho:** Desacoplamento temporal comprovado (o pedido é gravado mesmo com o `ms-produto` desligado), garantia de não perda de mensagens e suporte a retry/DLQ.
- **Custo:** Introdução de um broker adicional na arquitetura.
- **Mitigação:** Execução dockerizada padronizada (`rabbitmq:3-management`) com painel web na porta 15672.