# ADR-001: Decomposição em Bounded Contexts (Pedidos e Catálogo/Estoque)

## Status
Aceito

## Contexto
O WebMarket precisa separar as operações de venda da gestão de catálogo e estoque para permitir evolução independente, ciclos de vida desacoplados e modelagem rica orientada a Domain-Driven Design (DDD).

## Decisão
Dividir o sistema em dois Bounded Contexts com serviços autônomos:
1. `ms-pedido`: responsável pelo ciclo de vida do pedido (criação, cálculo, cancelamento e histórico).
2. `ms-produto`: responsável pelo catálogo, preços vigentes e integridade do estoque físico.

## Alternativas consideradas
- **Monólito único:** Rejeitado para evitar acoplamento de código, dependência de deploy conjunto e violação dos limites de domínio.
- **Micro-serviço adicional para Estoque isolado de Catálogo:** Rejeitado nesta fase para evitar complexidade operacional prematura em um escopo acadêmico.

## Consequências
- **Ganho:** Fronteiras de domínio bem definidas, autonomia de deploy e escalabilidade independente.
- **Custo:** Necessidade de comunicação assíncrona entre contextos e gerenciamento de consistência eventual.
- **Mitigação:** Uso de mensageria assíncrona durável e contratos de integração explícitos (Pact).