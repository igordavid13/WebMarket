# 🛒 Marketplace: Microsserviços com DDD, Mensageria e Resiliência

Projeto de marketplace composto por dois microsserviços autônomos, **ms-produto** (Catálogo e Estoque) e **ms-pedido** (Gestão e Gravação de Pedidos), com um módulo compartilhado **shared-contracts**. A modelagem segue os princípios do **Domain-Driven Design (DDD)**, do domínio estratégico ao tático, atendendo integralmente aos seguintes requisitos arquiteturais:

- ✅ **Bases de Dados Segregadas:** Cada serviço possui seu próprio banco H2 isolado.
- ✅ **Fluxo Assíncrono com RabbitMQ:** Ao registrar um pedido, o comando de baixa de estoque é enfileirado de forma durável.
- ✅ **Consumidor Idempotente:** O `ms-produto` impede baixas duplicadas via tabela `EVENTO_PROCESSADO`.
- ✅ **Transactional Outbox Pattern:** Ambos os serviços gravam estado e mensagens na mesma transação local antes do despacho.
- ✅ **Cache-Aside com Redis:** Catálogo cacheado no Redis com invalidação transacional explícita após alteração de estoque.
- ✅ **Contract Testing com Pact:** Contrato automatizado garantindo a compatibilidade entre consumidor (`ms-pedido`) e provedor (`ms-produto`).

---

## 📑 Sumário

1. [Domínio e Subdomínios](#1-domínio-e-subdomínios)
2. [Linguagem Ubíqua](#2-linguagem-ubíqua)
3. [Histórias de Usuário](#3-histórias-de-usuário)
4. [Event Storming](#4-event-storming)
5. [Bounded Contexts](#5-bounded-contexts)
6. [Context Map](#6-context-map)
7. [Agregados](#7-agregados)
8. [Comandos e Eventos](#8-comandos-e-eventos)
9. [Coreografia x Orquestração](#9-coreografia-x-orquestração)
10. [Arquitetura e Resiliência](#10-arquitetura-e-resiliência)
11. [Fluxo Assíncrono](#11-fluxo-assíncrono)
12. [Idempotência](#12-idempotência)
13. [Cache com Redis](#13-cache-com-redis)
14. [Contract Test (Pact)](#14-contract-test-pact)
15. [API REST](#15-api-rest)
16. [Execução Local e Demonstração](#16-execução-local-e-demonstração)
17. [Evoluções Futuras](#17-evoluções-futuras)

---

## 1. Domínio e Subdomínios

O marketplace processa compras e assegura a consistência eventual do estoque dos produtos comercializados.

| Subdomínio | Tipo | Justificativa |
|---|---|---|
| **Pedidos** | Core | É onde a venda se concretiza e reside o valor central do negócio |
| **Catálogo** | Supporting | Suporta a visualização e reserva de estoque necessária para as vendas |

---

## 2. Linguagem Ubíqua

| Termo | Significado | Representação no Código |
|---|---|---|
| Produto | Item disponível no catálogo para compra | `Produto` / `ProdutoEntity` |
| Estoque | Quantidade física disponível para débito | `quantidadeEstoque` |
| Pedido | Registro de intenção de compra do cliente | `Pedido` / `PedidoEntity` |
| Item do Pedido | Composição do pedido (produto, quantidade e preço) | `ItemPedidoEntity` |
| Baixar Estoque | Comando assíncrono emitido após gravação do pedido | `BaixarEstoqueCommand` / `BaixaEstoqueEvent` |
| Evento Processado | Registro de idempotência de mensagens já tratadas | `EventoProcessadoEntity` |

---

## 3. Histórias de Usuário

| HU | História | Critério de Aceite |
|---|---|---|
| **HU1** | Como **cliente**, quero consultar o catálogo de produtos | Dado um produto ativo, os dados são retornados do cache Redis se disponíveis; se for cache-miss, busca no banco e popula o Redis. |
| **HU2** | Como **cliente**, quero registrar um pedido de compra | Dado um pedido válido, ele é persistido no banco local de pedidos junto ao registro de outbox com status 201 Created. |
| **HU3** | Como **sistema**, quero debitar o estoque de forma resiliente | O comando é entregue via RabbitMQ na fila `estoque.baixar.q`. Se o serviço de produto estiver fora do ar, a mensagem aguarda no broker. |
| **HU4** | Como **sistema**, quero garantir processamento único de baixas | Se a mesma mensagem for reenviada, a chave `eventId` na tabela `EVENTO_PROCESSADO` rejeita nova dedução. |
| **HU5** | Como **administrador**, quero catálogo sempre consistente | Após qualquer dedução de estoque commitada no banco, a chave do produto no Redis é invalidada. |

---

## 4. Event Storming

```text
[Cliente] CriarPedido 
   → (ms-pedido) PedidoGravado + Outbox gravada
   → [RabbitMQ: estoque.baixar.q] 
   → (ms-produto) BaixarEstoque 
   → EstoqueDebitado + EventoProcessado gravado + Outbox gravada
   → [Redis] Cache invalidado
   → [Kafka: estoque-eventos] EstoqueBaixadoEvent publicado