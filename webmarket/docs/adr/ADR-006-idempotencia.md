# ADR-006: Garantia de Idempotência no Consumo de Mensagens

## Status
Aceito

## Contexto
Protocolos de mensageria durável (AMQP/RabbitMQ) operam sob garantia de entrega *at-least-once*. Falhas de rede, rebalanceamentos ou reinicializações de instâncias podem reenviar comandos já processados, o que causaria débitos duplicados no estoque físico.

## Decisão
Implementar idempotência estrita no `ms-produto` controlada pela tabela `EVENTO_PROCESSADO`:
- Cada comando recebido contém um `eventId` único (UUID).
- O `BaixaEstoqueService` verifica `eventoProcessadoRepository.existsById(eventId)`.
- Se a chave já existir, o processamento é imediatamente ignorado.
- Caso contrário, a inserção na `EVENTO_PROCESSADO` e o débito no saldo de estoque são commitados sob a mesma transação `@Transactional`.

## Alternativas consideradas
- **Confiar no ACK do broker:** Rejeitado porque se o consumidor cair entre o processamento e o ACK, a mensagem será reentregue e processada novamente.
- **Deduplicação no Redis:** Rejeitado porque a validação de idempotência precisa ter consistência transacional ACID com a atualização do estoque no banco relacional.

## Consequências
- **Ganho:** Proteção garantida contra débitos duplicados em qualquer cenário de reentrega.
- **Custo:** Consulta e escrita adicionais no banco H2 a cada mensagem.
- **Mitigação:** Tabela enxuta indexada por chave primária simples (`VARCHAR(64)`).