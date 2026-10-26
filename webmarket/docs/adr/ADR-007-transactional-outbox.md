# ADR-007: Padrão Transactional Outbox para Envio Confiável de Mensagens

## Status
Aceito

## Contexto
Ao registrar um pedido no `ms-pedido` ou confirmar uma baixa no `ms-produto`, a aplicação precisa alterar o banco de dados e emitir uma mensagem no broker. Se o broker estiver instável ou a rede falhar após o commit, a mensagem é perdida (*Dual-Write Problem*).

## Decisão
Implementar o padrão Transactional Outbox:
- As mensagens de integração são persistidas em tabelas locais (`PEDIDO_OUTBOX` no `ms-pedido` e `OUTBOX_EVENTO` no `ms-produto`) na mesma transação local de negócio.
- O componente publisher/relay despacha a mensagem para o broker e atualiza o status para `PUBLICADO`.

## Alternativas consideradas
- **Disparo direto no broker durante o request HTTP:** Rejeitado pelo risco de inconsistência caso o banco confirme e o broker falhe (ou vice-versa).
- **Two-Phase Commit (2PC / XA Transactions):** Rejeitado por degradar significativamente o throughput, introduzir bloqueios distribuídos e não ser suportado nativamente de forma simples em brokers modernos.

## Consequências
- **Ganho:** Garantia atômica de que nenhuma mensagem será perdida se a transação do banco for commitada.
- **Custo:** Necessidade de esquema outbox e processo de despacho/relay.
- **Mitigação:** Esquema relacional simples persistido via Spring Data JPA.