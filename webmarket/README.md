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
| Baixar Estoque | Comando assíncrono emitido após gravação do pedido | `BaixarEstoqueCommand` / `BaixaEstoqueMessage` |
| Evento Processado | Registro de idempotência de mensagens já tratadas | `EventoProcessadoEntity` / `EVENTO_PROCESSADO` |

---

## 3. Histórias de Usuário

| HU | História | Critério de Aceite |
|---|---|---|
| **HU1** | Como **cliente**, quero consultar o catálogo de produtos | Dado um produto ativo, os dados são retornados do cache Redis se disponíveis; se for cache-miss, busca no banco e popula o Redis. |
| **HU2** | Como **cliente**, quero registrar um pedido de compra | Dado um pedido válido, ele é persistido no banco local de pedidos junto ao registro de outbox com status `201 Created`. |
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
```

---

## 5. Bounded Contexts

| Contexto | Microsserviço | Porta | Banco de Dados (Isolado) | Responsabilidades |
|---|---|---|---|---|
| **Pedidos** | `ms-pedido` | `8082` | H2 em memória (`jdbc:h2:mem:pedidos`) | Registro de pedidos, persistência outbox e disparo de comandos |
| **Catálogo/Estoque** | `ms-produto` | `8083` | H2 em arquivo (`jdbc:h2:file:./data/webmarket-produto`) | Manutenção de catálogo, débito de estoque, cache Redis e idempotência |

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
          |  [ACL] ProdutoView / DTO      |
          +---------------+---------------+
                          |
     Published Language   |  RabbitMQ (Exchange: webmarket.commands -> Queue: estoque.baixar.q)
                          v
          +-------------------------------+
          |  CATÁLOGO: consumidor         |
          |  idempotente -> reduz estoque |
          |  e invalida cache Redis       |
          +-------------------------------+
```

---

## 7. Agregados

* **Agregado Pedido (`ms-pedido`):** Raiz `PedidoEntity`, contendo sua lista de `ItemPedidoEntity`. Assegura integridade e gera o registro correlato em `PedidoOutboxEntity`.
* **Agregado Produto (`ms-produto`):** Raiz `ProdutoEntity`. Controla saldo de estoque sem permitir valores negativos, garantindo persistência atômica com `EventoProcessadoEntity`.

---

## 8. Comandos e Eventos

### Comandos e Mensagens de Integração

* **`GravarPedidoCommand`:** Recebido via API REST no `ms-pedido`.
* **`BaixaEstoqueMessage` (RabbitMQ):**
  * **Exchange:** `webmarket.commands`
  * **Routing Key:** `estoque.baixar`
  * **Queue:** `estoque.baixar.q` (durable)
  * **DLQ:** `gravacao.dlq`

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440001",
  "pedidoId": 1,
  "itens": [
    {
      "produtoId": 1,
      "quantidade": 2
    }
  ]
}
```

---

## 9. Coreografia x Orquestração

* **Decisão Arquitetural:** Coreografia reativa orientada a comandos duráveis.
* **Justificativa:** O fluxo atual compreende pedido e baixa de estoque direta. A utilização do RabbitMQ com filas duráveis e confirmação manual (*ACK*) provê desacoplamento temporal: `ms-pedido` conclui a venda mesmo com `ms-produto` indisponível.

---

## 10. Arquitetura e Resiliência

```text
                  +------------------------------------------------+
                  |                   ms-pedido                    |
                  |  [Controller] -> [Service]                     |
                  |         |               |                      |
                  |   (Grava H2)      (Grava Outbox)               |
                  +-------------------------+----------------------+
                                            | Outbox Relay / Pub
                                            v
                              [ RabbitMQ: webmarket.commands ]
                                            |
                                            v (estoque.baixar.q)
                  +------------------------------------------------+
                  |                   ms-produto                   |
                  |  [BaixaEstoqueListener]                        |
                  |         |                                      |
                  |  [Idempotência: EVENTO_PROCESSADO]             |
                  |         |                                      |
                  |  [Debita Estoque H2]                           |
                  |         |                                      |
                  |  [Invalida Redis: produto:1 e produtos:catalogo]|
                  +------------------------------------------------+
```

---

## 11. Fluxo Assíncrono

1. O cliente requisita a gravação do pedido em `POST /interno/pedidos`.
2. O `ms-pedido` abre transação local, salva o pedido, grava a mensagem na tabela outbox e responde com sucesso (`201 Created`).
3. O `BaixaEstoquePublisher` publica a mensagem na exchange `webmarket.commands`.
4. O RabbitMQ retém a mensagem na fila `estoque.baixar.q`.
5. O `ms-produto`, ao consumir, debita o estoque e grava a chave de idempotência.

---

## 12. Idempotência

Para suportar reentregas de mensagens (*at-least-once delivery*), o `ms-produto` utiliza a tabela `EVENTO_PROCESSADO`:

```sql
CREATE TABLE EVENTO_PROCESSADO (
    event_id      VARCHAR(64) PRIMARY KEY,
    processado_em TIMESTAMP NOT NULL
);
```

Antes de efetuar o débito do estoque, o serviço executa:
```java
if (eventoProcessadoRepository.existsById(message.getEventId())) {
    log.warn("Evento {} já processado. Ignorando duplicidade.", message.getEventId());
    return;
}
```

---

## 13. Cache com Redis

* **Serviço:** `ms-produto`
* **Implementação:** `StringRedisTemplate` com serialização JSON via Jackson.
* **Estratégia:** Cache-Aside com TTL de 10 minutos.
* **Padrão de Chaves:**
  * Catálogo geral: `produtos:catalogo`
  * Produto individual: `produto:{id}` (ex: `produto:1`)
* **Invalidação Transacional:** Ao processar com sucesso a baixa de estoque, o método `invalidarCache(produtoId)` expurga `produtos:catalogo` e `produto:{id}` do Redis.

---

## 14. Contract Test (Pact)

Garante que as alterações de API do catálogo não quebrem a integração com o serviço de pedidos.

* **Consumidor:** `ms-pedido` (`ProdutoConsumerContractTest.java`)
* **Provedor:** `ms-produto` (`ProdutoProviderContractTest.java`)
* **Arquivo Gerado:** `ms-pedido/target/pacts/ms-pedido-ms-produto.json`

### Contrato Pact Verificado

```json
{
  "id": 1,
  "nome": "Notebook",
  "preco": 3500.00,
  "quantidadeEstoque": 10,
  "ativo": true
}
```

---

## 15. API REST

### ms-produto (Porta 8083)

| Método | Endpoint | Descrição | Comportamento de Cache |
|---|---|---|---|
| `GET` | `/produtos` | Lista catálogo de produtos ativos | Lê/Grava em `produtos:catalogo` |
| `GET` | `/produtos/{id}` | Busca produto por ID | Lê/Grava em `produto:{id}` |

### ms-pedido (Porta 8082)

| Método | Endpoint | Descrição | Status HTTP |
|---|---|---|---|
| `POST` | `/interno/pedidos` | Cria novo pedido e emite comando de baixa | `201 Created` |

---

## 16. Execução Local e Demonstração

### 1. Iniciar Infraestrutura Docker

```bash
# Iniciar RabbitMQ com painel de gerenciamento
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management

# Iniciar servidor Redis
docker run -d --name redis -p 6379:6379 redis:alpine

# Iniciar Kafka
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  apache/kafka:latest
```

### 2. Instalar Contratos e Compilar Módulos

Na raiz do projeto (`WebMarket`):

```bash
# Instalar contratos compartilhados no repositório local
./mvnw -pl shared-contracts install -DskipTests

# Compilar todos os microsserviços
./mvnw clean compile -DskipTests
```

### 3. Executar os Testes de Contrato (Pact)

```bash
# 1. Gerar o contrato do consumidor
./mvnw -pl ms-pedido -am test -Dtest=ProdutoConsumerContractTest

# 2. Validar o contrato no provedor
./mvnw -pl ms-produto -am test -Dtest=ProdutoProviderContractTest
```

### 4. Execução dos Microsserviços

Abra terminais dedicados para cada aplicação:

```bash
# Terminal 1 - ms-produto (Porta 8083)
./mvnw -pl ms-produto spring-boot:run

# Terminal 2 - ms-pedido (Porta 8082)
./mvnw -pl ms-pedido spring-boot:run
```

### 5. Roteiro de Demonstração (Cenário do Vídeo)

#### Teste de Resiliência (Broker durável com serviço desligado)
1. Pare o `ms-produto` (`Ctrl + C`).
2. Envie um pedido via `ms-pedido`:
   ```bash
   curl -i -X POST http://localhost:8082/interno/pedidos \
     -H "Content-Type: application/json" \
     -d '{
       "identificadorPedido": "550e8400-e29b-41d4-a716-446655440001",
       "dados": {
         "idCliente": 1,
         "itens": [{"produto": 1, "quantidade": 2}]
       }
     }'
   ```
3. Acesse o RabbitMQ (`http://localhost:15672` - `guest:guest`) e comprove na aba **Queues** que a fila `estoque.baixar.q` retém a mensagem com **Ready = 1**.
4. Inicie o `ms-produto`. Observe a mensagem sendo consumida imediatamente e a fila zerando.

#### Teste de Cache e Invalidação no Redis
1. Em um terminal, consulte o produto 1 para aquecer o cache:
   ```bash
   curl -i http://localhost:8083/produtos/1
   ```
2. Inspecione o contêiner do Redis:
   ```bash
   docker exec -it redis redis-cli KEYS *
   # A chave "produto:1" será exibida
   ```
3. Envie novo pedido com o produto 1.
4. Execute `docker exec -it redis redis-cli KEYS *` e confirme que a chave foi expurgada do cache.

#### Teste de Idempotência
1. Reenvie a mesma requisição com o identificador `"550e8400-e29b-41d4-a716-446655440001"`.
2. Verifique nos logs do `ms-produto` o aviso de mensagem já processada, mantendo o estoque intacto.

---

## 17. Evoluções Futuras

- **Saga Orquestrada:** Implementação de saga completa para abarcar fluxos de pagamento externo e cálculo de frete.
- **Compensação de Estoque:** Devolução automática do estoque mediante evento `PedidoCanceladoEvent`.
- **Pact Broker:** Publicação centralizada dos arquivos de pacto em pipeline de CI/CD automatizado.