# ADR-008: Modelo de Consistência Eventual com Ações Compensatórias

## Status
Aceito

## Contexto
Em arquiteturas de microsserviços com bases segregadas, a criação de pedidos e a reserva de estoque não podem depender de bloqueios distribuídos síncronos sem sacrificar disponibilidade e performance.

## Decisão
Adotar o modelo de consistência eventual baseada em coreografia reativa:
1. O pedido nasce no estado `CONFIRMADO` (aceito e registrado).
2. O comando de baixa é processado assincronamente pelo estoque.
3. Em caso de sucesso, o pedido é concluído.
4. Em caso de falta de estoque definitiva, o evento `BaixaEstoqueFalhouEvent` aciona uma compensação no `ms-pedido`, transicionando o pedido para `CANCELADO_POR_FALHA_ESTOQUE`.

## Alternativas consideradas
- **Reserva prévia síncrona via HTTP:** Rejeitado pelo acoplamento de disponibilidade e risco de travar recursos caso o checkout seja abandonado.
- **Saga orquestrada centralizada:** Rejeitada para a primeira versão devido à complexidade de infraestrutura desproporcional para um fluxo com apenas dois serviços.

## Consequências
- **Ganho:** Alta disponibilidade de checkout e resposta rápida na API pública.
- **Custo:** Janela temporal onde o cliente possui um pedido aceito, mas cujo estoque ainda não foi efetivado.
- **Mitigação:** Tratamento explícito de compensação caso o estoque seja insuficiente.