# 🛒 Marketplace: Microsserviços com DDD

Projeto de marketplace com dois microsserviços, **ms-produto** (Catálogo) e **ms-pedido** (Pedidos). A modelagem segue **Domain-Driven Design**, do domínio estratégico ao tático. O projeto cobre estes requisitos:

- ✅ Fluxo assíncrono: quando o pedido é criado, o estoque é reduzido
- ✅ Consumidor idempotente: o mesmo pedido nunca reduz o estoque duas vezes
- ✅ Cache dos produtos mais acessados do catálogo
- ✅ Contract Test de Pedido → Produto

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
10. [Arquitetura](#10-arquitetura)
11. [Fluxo Assíncrono](#11-fluxo-assíncrono)
12. [Idempotência](#12-idempotência)
13. [Cache](#13-cache)
14. [Contract Test](#14-contract-test)
15. [API](#15-api)
16. [Execução local](#16-execução-local)
17. [Evoluções Futuras](#17-evoluções-futuras)

---

## 1. Domínio e Subdomínios

O marketplace vende produtos e precisa manter o estoque consistente depois de cada compra.

| Subdomínio | Tipo | Justificativa |
|---|---|---|
| **Pedidos** | Core | É onde a venda acontece e onde o negócio gera valor |
| **Catálogo** | Supporting | É necessário para vender, mas não diferencia o negócio |

---

## 2. Linguagem Ubíqua

| Termo | Significado | No código |
|---|---|---|
| Produto | Item disponível para venda | `Produto` |
| Estoque | Quantidade disponível, nunca negativa | `Estoque` |
| Preço | Valor monetário com moeda | `Dinheiro` |
| Pedido | Intenção de compra de um cliente | `Pedido` |
| Item do pedido | Produto, quantidade e preço no momento da compra | `ItemPedido` |
| Criar pedido | Comando, ou seja, uma intenção | `CriarPedidoCommand` |
| Pedido criado | Evento, ou seja, um fato já ocorrido | `PedidoCriadoEvent` |

**Convenções**

- Comandos ficam no **imperativo**: `CriarPedido`, `AlterarPreco`.
- Eventos ficam no **passado**: `PedidoCriado`, `PrecoAlterado`.
- Nomes técnicos de banco, como `TB_PRD`, não fazem parte da linguagem do domínio.

---

## 3. Histórias de Usuário

| HU | História | Critério de aceite |
|---|---|---|
| **HU1** | Como **cliente**, quero consultar o catálogo para escolher produtos | Dado um produto ativo, quando eu consultar, vejo nome, preço e estoque. A resposta é servida via cache |
| **HU2** | Como **cliente**, quero criar um pedido para comprar produtos | Dado um produto com estoque, quando eu criar o pedido, ele é registrado e o `PedidoCriadoEvent` é publicado |
| **HU3** | Como **sistema**, quero reduzir o estoque quando um pedido for criado para manter a disponibilidade correta | Dado um `PedidoCriadoEvent`, o estoque é reduzido **uma única vez**, mesmo que o evento chegue repetido |
| **HU4** | Como **administrador**, quero alterar preço e estoque para manter o catálogo atualizado | Quando o preço ou o estoque mudar, o cache do produto é invalidado |

---

## 4. Event Storming

Leia cada linha assim: **Ator → Comando → Agregado → Evento → Política**.

| Ator | Comando | Agregado | Evento | Política |
|---|---|---|---|---|
| Admin | CadastrarProduto | Produto | ProdutoCadastrado | — |
| Admin | AlterarPreco | Produto | PrecoAlterado | Invalidar cache |
| Admin | AtualizarEstoque | Produto | EstoqueAtualizado | Invalidar cache |
| Cliente | CriarPedido | Pedido | **PedidoCriado** | **Quando o pedido for criado, reduzir o estoque** |
| Política | ReduzirEstoque | Produto | EstoqueAtualizado | Invalidar cache |
| Cliente | ConfirmarPedido | Pedido | PedidoConfirmado | — |
| Cliente | CancelarPedido | Pedido | PedidoCancelado | (futuro) liberar estoque |

**Linha do tempo**

```text
ProdutoCadastrado → PedidoCriado → [Política] → EstoqueAtualizado → PedidoConfirmado
                                                                   ↘ PedidoCancelado
```

**Read model:** os produtos mais acessados ficam no Redis.

---

## 5. Bounded Contexts

| Contexto | Serviço | Responsabilidades | Dados |
|---|---|---|---|
| **Catálogo** | `ms-produto` | Produtos, preço, estoque, consulta de catálogo | Banco próprio |
| **Pedidos** | `ms-pedido` | Carrinho, checkout, pedido, histórico | Banco próprio |

> O mesmo termo tem significados diferentes em cada contexto. No **Catálogo**, *Produto* tem preço atual e estoque. Em **Pedidos**, o produto é só um `produtoId` com o **preço registrado no momento da compra**.

---

## 6. Context Map

```text
          +-------------------------------+
          |      CATÁLOGO (Upstream)      |
          |          ms-produto           |
          |  [OHS] API REST /produtos     |
          +---------------+---------------+
                          |
     Customer / Supplier  |  REST síncrono + Contract Test (Pact)
                          v
          +---------------+---------------+
          |      PEDIDOS (Downstream)     |
          |           ms-pedido           |
          |  [ACL] ProdutoDTO → ItemPedido|
          +---------------+---------------+
                          |
     Published Language   |  PedidoCriadoEvent (fila: pedido.criado)
                          v
          +-------------------------------+
          |  CATÁLOGO: consumidor         |
          |  idempotente → reduz estoque  |
          +-------------------------------+
```

| Padrão | Aplicação |
|---|---|
| **Customer/Supplier** | Pedidos depende do Catálogo, e o Catálogo se compromete com o contrato |
| **Open Host Service** | `GET /produtos/{id}` funciona como API pública e estável |
| **Anti-Corruption Layer** | O `ms-pedido` traduz o DTO do Catálogo para o próprio modelo |
| **Published Language** | O `PedidoCriadoEvent` é o contrato público do evento |

---

## 7. Agregados

O **agregado** é um conjunto de objetos que mudam juntos e mantêm suas regras consistentes. A **raiz do agregado** é a única porta de entrada para alterá-lo.

### Catálogo

```text
Produto  ← Aggregate Root
 ├─ ProdutoId   (VO)
 ├─ Dinheiro    (VO)  valor + moeda
 └─ Estoque     (VO)  quantidade >= 0
```

O Produto controla:

- a alteração de preço (`alterarPreco`)
- a redução e a liberação de estoque (`reduzirEstoque` / `liberarEstoque`)
- a regra que **impede o estoque negativo**

### Pedidos

```text
Pedido  ← Aggregate Root
 ├─ PedidoId         (VO)
 ├─ ClienteId        (VO)
 ├─ ItemPedido       (Entidade interna)
 │    ├─ ProdutoId   (referência por ID)
 │    ├─ Quantidade  (VO)  > 0
 │    └─ Dinheiro    (precoUnitario)
 ├─ EnderecoEntrega  (VO)  logradouro, numero, cidade, uf, cep
 └─ Dinheiro         (valorTotal)
```

### Regras de agregado

- Toda alteração passa pela raiz, por exemplo `pedido.adicionarItem(...)`.
- Um agregado referencia outro **apenas por ID**. O Pedido guarda o `ProdutoId`, não o objeto `Produto`.
- Cada transação altera **um único agregado**. Por isso, reduzir o estoque depois de criar o pedido é **assíncrono**, com consistência eventual.

---

## 8. Comandos e Eventos

### Comandos

| Contexto | Comando | Campos |
|---|---|---|
| Catálogo | `CadastrarProdutoCommand` | nome, descricao, preco, estoqueInicial |
| Catálogo | `AlterarPrecoCommand` | produtoId, novoPreco |
| Catálogo | `AtualizarEstoqueCommand` | produtoId, quantidade |
| Pedidos | `CriarPedidoCommand` | clienteId, itens |
| Pedidos | `ConfirmarPedidoCommand` | pedidoId |
| Pedidos | `CancelarPedidoCommand` | pedidoId |

### Eventos de domínio

| Contexto | Evento |
|---|---|
| Catálogo | `ProdutoCadastradoEvent`, `PrecoAlteradoEvent`, `EstoqueAtualizadoEvent` |
| Pedidos | `PedidoCriadoEvent`, `PedidoConfirmadoEvent`, `PedidoCanceladoEvent` |

### Contrato do `PedidoCriadoEvent`

```json
{
  "eventId": "8f1c2a7e-3b4d-4c5e-9f60-1a2b3c4d5e6f",
  "pedidoId": "123",
  "clienteId": "456",
  "itens": [
    { "produtoId": "A", "quantidade": 2 },
    { "produtoId": "B", "quantidade": 1 }
  ],
  "valorTotal": 1000.00,
  "dataHora": "2026-10-09T18:00:00"
}
```

> O campo `itens` é obrigatório. Sem ele, o consumidor não sabe quais produtos reduzir nem em quantas unidades.

---

## 9. Coreografia x Orquestração

| Critério | Coreografia | Orquestração |
|---|---|---|
| Funcionamento | Cada serviço reage aos eventos | Um coordenador central comanda o fluxo |
| Acoplamento | Baixo | Os serviços dependem do orquestrador |
| Visibilidade | Distribuída | Centralizada |
| Compensações | Ficam difíceis com muitos passos | Ficam explícitas |
| Indicado para | Fluxos curtos | Sagas longas (pagamento, frete etc.) |

**Decisão: coreografia ✅**

O fluxo tem só um produtor, um evento e um consumidor, sem compensações complexas. Um orquestrador traria custo sem benefício.

```text
ms-pedido ──PedidoCriadoEvent──▶ [pedido.criado] ──▶ ms-produto (reduz estoque)
```

> Migrar para orquestração quando entrarem pagamento, frete e cupons, com necessidade de compensação (estorno, liberação de estoque).

---

## 10. Arquitetura

```text
                    +------------------+
                    |    ms-produto    |
                    |------------------|
                    | Produto          |
                    | Estoque / Preço  |
                    | Redis (cache)    |
                    +---------+--------+
                              ^
                              | REST + Pact
                              v
                    +---------+--------+
                    |    ms-pedido     |
                    |------------------|
                    | Pedido           |
                    | Checkout         |
                    | Carrinho         |
                    +---------+--------+
                              |
                              | PedidoCriadoEvent
                              v
                     [ fila: pedido.criado ]
                              |
                              v
                    +------------------+
                    |    ms-produto    |
                    | Consumidor       |
                    | idempotente      |
                    +------------------+
```

### Estrutura sugerida de cada serviço

```text
ms-<servico>/
 ├─ domain/          # Agregados, entidades, VOs, eventos de domínio
 ├─ application/     # Casos de uso (handlers de comandos)
 ├─ infrastructure/  # Persistência, mensageria, cache, clients REST
 └─ api/             # Controllers / endpoints REST
```

---

## 11. Fluxo Assíncrono

| Papel | Serviço | Ação |
|---|---|---|
| Producer | `ms-pedido` | Publica o `PedidoCriadoEvent` na fila `pedido.criado` quando o pedido é criado |
| Consumer | `ms-produto` | Consome o evento e reduz o estoque de cada item |

**Exemplo**

```text
Produto A: 10 → 8   (pedido com 2 unidades)
Produto B:  5 → 4   (pedido com 1 unidade)
```

---

## 12. Idempotência

**Problema:** o mesmo evento pode ser entregue mais de uma vez (entrega *at-least-once*).

```text
Sem proteção:  10 → 8 → 6   ❌
Com proteção:  10 → 8 → 8   ✅
```

**Tabela de controle**

```sql
CREATE TABLE TB_EVENTO_PROCESSADO (
    event_id            VARCHAR(36) PRIMARY KEY,
    data_processamento  TIMESTAMP   NOT NULL
);
```

**Fluxo**

```text
Recebe evento
     ↓
eventId existe em TB_EVENTO_PROCESSADO?
     ├─ SIM → ignora
     └─ NÃO → reduz estoque + registra eventId (mesma transação)
```

> Reduzir o estoque e registrar o `eventId` têm que acontecer **na mesma transação**. A chave primária em `event_id` garante a proteção mesmo com consumidores concorrentes.

---

## 13. Cache

| Item | Definição |
|---|---|
| Serviço | `ms-produto` |
| Tecnologia | Redis |
| Endpoints cacheados | `GET /produtos`, `GET /produtos/{id}` |
| Chave | `produto::{id}` |
| Estratégia | Cache-Aside |

**Cache-Aside**

```text
Busca no Redis
     ├─ Encontrou → retorna
     └─ Não encontrou → busca no banco → grava no Redis → retorna
```

**Invalidação explícita** quando ocorrer `AlterarPreco`, `AtualizarEstoque` ou uma redução de estoque via evento:

```java
cache.invalidate(produtoId);
```

---

## 14. Contract Test

| Papel | Serviço |
|---|---|
| Consumer | `ms-pedido` |
| Provider | `ms-produto` |
| Ferramenta | Pact |

**Contrato:** `GET /produtos/{id}`

```json
{
  "id": "123",
  "nome": "Notebook",
  "preco": 3500.00,
  "estoqueDisponivel": 15
}
```

**Fluxo**

1. O `ms-pedido` gera o pacto com as expectativas dele.
2. O pacto é publicado (Pact Broker ou arquivo compartilhado).
3. O `ms-produto` verifica o pacto no próprio pipeline.
4. Se o provider quebrar o contrato, o build falha.

Execução local dos testes Pact:

```bash
./mvnw -Pms-pedido-local -pl ms-pedido -Dtest=ProdutoCatalogoConsumerPactTest test
./mvnw -Pms-produto-local -pl ms-produto -Dtest=ProdutoProviderPactTest test
```

O teste consumer gera `ms-pedido/target/pacts/ms-pedido-ms-produto.json`. Para verificar
o contrato no provider, copie esse arquivo para
`ms-produto/src/test/resources/pacts/ms-pedido-ms-produto.json` antes de executar o
segundo comando.

---

## 15. API

### ms-produto

| Método | Endpoint | Descrição | Cache |
|---|---|---|---|
| `POST` | `/produtos` | Cadastrar produto | — |
| `GET` | `/produtos` | Listar catálogo | ✅ |
| `GET` | `/produtos/{id}` | Consultar produto | ✅ |
| `PATCH` | `/produtos/{id}/preco` | Alterar preço | Invalida |
| `PATCH` | `/produtos/{id}/estoque` | Atualizar estoque | Invalida |

### ms-pedido

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/pedidos` | Criar pedido (publica `PedidoCriadoEvent`) |
| `GET` | `/pedidos/{id}` | Consultar pedido |
| `GET` | `/pedidos?clienteId={id}` | Histórico do cliente |
| `POST` | `/pedidos/{id}/confirmar` | Confirmar pedido |
| `POST` | `/pedidos/{id}/cancelar` | Cancelar pedido |

---

## 16. Execução local

Os comandos abaixo devem ser executados no diretório `webmarket`, onde está localizado o `mvnw`.

### Compilar o projeto

```bash
./mvnw -pl pedido-gravador -am -DskipTests compile
```

### Iniciar a aplicação

```bash
./mvnw -pl pedido-gravador -am spring-boot:run
```

A aplicação será iniciada na porta `8082`.

### Gravar um pedido diretamente pelo endpoint HTTP

Esta chamada grava o pedido diretamente no banco de dados, sem passar pelo
RabbitMQ:

```bash
curl --fail-with-body --silent --show-error \
  --request POST 'http://localhost:8082/interno/pedidos' \
  --header 'Content-Type: application/json' \
  --data-binary @pedido-gravador/src/main/resources/pedido-exemplo.json
```

Essa chamada HTTP retorna `200 OK` após o `GravadorController` chamar
diretamente o `GravadorService`.

### Publicar um pedido no RabbitMQ

Para testar o fluxo assíncrono, publique a mensagem diretamente no exchange
`gravacao.ex`, usando a routing key `gravacao`. O exemplo abaixo usa a API de
gerenciamento do RabbitMQ, normalmente disponível na porta `15672`, com o
usuário e senha padrão `guest`:

```bash
curl --fail-with-body --silent --show-error \
  --user 'guest:guest' \
  --request POST 'http://localhost:15672/api/exchanges/%2F/gravacao.ex/publish' \
  --header 'Content-Type: application/json' \
  --data-raw '{"properties":{},"routing_key":"gravacao","payload": "{\"identificadorPedido\":\"550e8400-e29b-41d4-a716-446655440001\",\"dados\":{\"idCliente\":1,\"itens\":[{\"produto\":1,\"quantidade\":1},{\"produto\":2,\"quantidade\":2},{\"produto\":3,\"quantidade\":1}]}}","payload_encoding":"string"}'
```

O `GravacaoListener` consome a fila `gravacao.q` e chama o
`GravadorService.gravar()`. Nesse fluxo, a controller não participa:

```text
RabbitMQ gravacao.q → GravacaoListener → GravadorService → banco de dados
```

O fluxo completo é:

```text
curl HTTP → GravadorController → GravadorService → banco de dados
curl RabbitMQ → exchange gravacao.ex → fila gravacao.q
             → GravacaoListener → GravadorService → banco de dados
```

O `DatabaseInitializer` cadastra automaticamente os clientes e produtos usados
no exemplo quando a aplicação é iniciada.

---

## 17. Evoluções Futuras

- **Outbox Pattern:** garante que o pedido salvo e o evento publicado não fiquem inconsistentes.
- **Saga orquestrada:** para pagamento, frete e compensações.
- **Liberação de estoque** no `PedidoCanceladoEvent`.
- **Novos contextos:** Pagamento, Promoções/Cupons, Frete.
- **Dead Letter Queue** para eventos que falharem no processamento.
