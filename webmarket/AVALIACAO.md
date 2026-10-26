# Auto-avaliação — WebMarket

**Grupo:**
* **Davi Fernandez Franca Aragao:** Análise de requisitos iniciais, codificação dos serviços e montagem/orquestração da infraestrutura para o Perfil A (Docker).
* **Marcos da Silva Ferreira:** Codificação, modelagem de domínio e definição arquitetural (Cache Redis, mensageria e resiliência).
* **Igor David Morais:** Análise de requisitos iniciais, arquitetura de software, codificação e testes de contrato (Pact).

**Tema/domínio:**
Marketplace distribuído orientado a DDD com dois microsserviços segregados (`ms-pedido` e `ms-produto`), processamento assíncrono de pedidos e controle transacional de estoque com consistência eventual.

**Perfil de execução:** **A (DOCKER)** · **Fallbacks usados:** **Nenhum** (Infraestrutura completa de RabbitMQ, Redis e Kafka executada em contêineres Docker reais).

---

## Evidências por critério

### 1. Decomposição de domínio
* **Evidência:** 
  * Segregação explícita em dois microsserviços autônomos e um módulo comum: `ms-pedido` (Bounded Context Pedidos), `ms-produto` (Bounded Context Catálogo/Estoque) e `shared-contracts` (contratos de integração).
  * **Bases Segregadas:** `ms-pedido` utiliza H2 em memória (`jdbc:h2:mem:pedidos`) isolado na porta `8082`, enquanto `ms-produto` utiliza H2 persistente em arquivo local (`jdbc:h2:file:./data/webmarket-produto`) na porta `8083`. Não há tabelas compartilhadas nem acoplamento de schema.
  * **Documentação:** Especificação detalhada em `docs/arquitetura.md` (seções 4 e 5) e fundamentado em `docs/adr/ADR-001-bounded-contexts.md` e `docs/adr/ADR-002-bancos-segregados.md`.

### 2. Comunicação assíncrona
* **Evidência:**
  * **Comandos assíncronos:** O `ms-pedido` despacha o comando imperativo `BaixarEstoqueCommand` através da classe `BaixaEstoquePublisher.java` para a Exchange direta e durável `webmarket.commands` com routing key `estoque.baixar`.
  * **Consumidor durável:** O `ms-produto` escuta a fila durável `estoque.baixar.q` através da classe `BaixaEstoqueListener.java`, delegando a execução para `BaixaEstoqueService.java`.
  * **Eventos de domínio:** Publicação de eventos pós-processamento (`EstoqueBaixadoEvent`) no tópico Kafka `estoque-eventos` gerenciado via `OutboxRelay.java`.
  * **Garantia de entrega:** *At-least-once* assegurada pelo Transactional Outbox antes do envio e confirmação manual/transacional no broker RabbitMQ.

### 3. Idempotência e consistência
* **Evidência:**
  * **Chave de Idempotência:** Cada mensagem de baixa carrega um `eventId` único (UUID).
  * **Deduplicação no Banco:** A classe `BaixaEstoqueService.java` no `ms-produto` consulta a tabela `EVENTO_PROCESSADO` antes de qualquer movimentação:
    ```java
    if (eventoProcessadoRepository.existsById(message.getEventId())) {
        log.warn("Evento {} já processado. Ignorando.", message.getEventId());
        return;
    }
    ```
  * **Atomicidade:** O débito de estoque em `PRODUTO`, a inserção na `EVENTO_PROCESSADO` e a gravação na outbox ocorrem sob a mesma transação `@Transactional`.
  * **Na API HTTP:** `GravadorController` e `GravadorService` asseguram unicidade por `identificadorPedido`.
  * **Documentação:** `docs/adr/ADR-006-idempotencia.md` e `docs/adr/ADR-008-consistencia-eventual-compensacao.md`.

### 4. Cache
* **Evidência:**
  * **Implementação:** Classe `ProdutoCatalogoService.java` no `ms-produto` utilizando `StringRedisTemplate` com serialização Jackson.
  * **Estratégia Cache-Aside:** Consultas via `GET /produtos/{id}` buscam a chave `produto:<id>` no Redis (TTL de 10 min); em caso de cache-miss, carregam do H2 e populam o Redis.
  * **Invalidação Transacional Explícita:** O método `invalidarCache(produtoId)` expurga `produto:<id>` e `produtos:catalogo` imediatamente após o commit da baixa de estoque.
  * **Documentação:** `docs/adr/ADR-005-redis-cache-catalogo.md`.

### 5. Resiliência
* **Evidência:**
  * **Desacoplamento Temporal Comprovado:** Com o `ms-produto` completamente desligado, o `ms-pedido` aceita requisições HTTP (retornando `201 Created`), grava o pedido e enfileira no RabbitMQ. A mensagem permanece segura com status `Ready: 1` no broker sem perda de dados.
  * **Recuperação Automática:** Ao reiniciar o `ms-produto`, o `@RabbitListener` conecta-se e consome a fila pendente automaticamente.
  * **Garantia Outbox:** Implementação de `PEDIDO_OUTBOX` no `ms-pedido` e `OUTBOX_EVENTO` no `ms-produto`, prevenindo estados órfãos (Dual-Write Problem).
  * **Dead Letter Queue:** Configuração de DLQ `gravacao.dlq` para mensagens corrompidas ou com falhas definitivas.
  * **Documentação:** `docs/adr/ADR-003-rabbitmq-comando-baixa.md` e `docs/adr/ADR-007-transactional-outbox.md`.

### 6. Testabilidade
* **Evidência:**
  * **Contract Testing (Pact):**
    * *Consumidor:* `ms-pedido/src/test/java/br/gov/caixa/webmarket/contract/ProdutoConsumerContractTest.java` gera o contrato V4 `ms-pedido/target/pacts/ms-pedido-ms-produto.json`.
    * *Provedor:* `ms-produto/src/test/java/br/gov/caixa/webmarket/contract/ProdutoProviderContractTest.java` valida o contrato real contra o servidor Spring Boot levantado em porta randômica (`RANDOM_PORT`), isolando Kafka e Outbox via `@MockitoBean`.
  * **Execução dos Testes:** Build passando com 100% de sucesso via Maven:
    ```bash
    ./mvnw -pl ms-pedido -am test -Dtest=ProdutoConsumerContractTest
    ./mvnw -pl ms-produto -am test -Dtest=ProdutoProviderContractTest
    ```
  * **Documentação:** `docs/adr/ADR-009-contract-testing-pact.md`.

### 7. Decisões arquiteturais
* **Evidência:**
  * Conjunto formal de 9 Architecture Decision Records estruturados em `docs/adr/`:
    * `ADR-001-bounded-contexts.md`: Separação em dois contextos autônomos.
    * `ADR-002-bancos-segregados.md`: Banco por serviço (H2 memória / H2 arquivo).
    * `ADR-003-rabbitmq-comando-baixa.md`: RabbitMQ para comandos de trabalho direto.
    * `ADR-004-kafka-eventos-dominio.md`: Kafka para disseminação de eventos de domínio.
    * `ADR-005-redis-cache-catalogo.md`: Redis Cache-Aside com invalidação pontual.
    * `ADR-006-idempotencia.md`: Deduplicação por tabela `EVENTO_PROCESSADO`.
    * `ADR-007-transactional-outbox.md`: Persistência atômica de mensagens via Outbox.
    * `ADR-008-consistencia-eventual-compensacao.md`: Consistência eventual com compensação.
    * `ADR-009-contract-testing-pact.md`: Testes de contrato orientados pelo consumidor.

### 8. Uso crítico de IA
* **Como usamos IA e o que validamos manualmente:**
  * A IA foi utilizada como aceleradora técnica para desenhar esboços iniciais da arquitetura, gerar esqueletos de classes Spring Boot, propor topologias de mensageria e estruturar minutas de documentação técnica e ADRs.
  * **Validação Crítica e Intervenções Manuais da Equipe:**
    1. *Diagnóstico de Inconsistência no Cache:* A IA inicialmente sugeriu uma implementação de cache local baseada em Caffeine na classe `ProdutoCatalogoService`. Identificamos criticamente que o contêiner Redis no Docker permanecia vazio durante as consultas e refatoramos manualmente a camada para utilizar `StringRedisTemplate` com serialização explícita em JSON, permitindo a validação visual via `redis-cli KEYS *`.
    2. *Correção de Erro de Sintaxe Hibernate/JPA:* O mapeamento de `PedidoEntity` continha anotação `@OrderBy("dataHora, id")`, gerando exceção `SQLGrammarException (Column "I1_0.DATAHORA" not found)` no H2. Diagnosticamos o log de stack trace e ajustamos a ordenação exclusivamente para `@OrderBy("id")`.
    3. *Isolamento no Teste do Provedor Pact:* O teste `ProdutoProviderContractTest` falhava ao subir o contexto por tentar se conectar a instâncias externas do Kafka e da Outbox. Interviemos configurando anotações `@MockitoBean` nas interfaces de mensageria para permitir a execução pura e determinística do contrato HTTP.
    4. *Ajuste de Tipos e Schemas:* Alinhamos manualmente todos os contratos para refletir o schema real (ID numérico `Integer`, BigDecimal para valores monetários e nomes canônicos de tabelas H2).

### 9. Execução
* **Perfil Declarado:** **Perfil A (Docker)**
* **Como rodar:**

1. **Subir contêineres de suporte (Terminal Git Bash):**
   ```bash
   # RabbitMQ Management
   docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management

   # Servidor Redis
   docker run -d --name redis -p 6379:6379 redis:alpine

   # Broker Apache Kafka
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

2. **Compilar módulos e contratos compartilhados (Raiz do projeto):**
   ```bash
   ./mvnw -pl shared-contracts install -DskipTests
   ./mvnw clean compile -DskipTests
   ```

3. **Executar Testes de Contrato Pact:**
   ```bash
   # Gerar contrato no consumidor (ms-pedido)
   ./mvnw -pl ms-pedido -am test -Dtest=ProdutoConsumerContractTest

   # Verificar contrato no provedor (ms-produto)
   ./mvnw -pl ms-produto -am test -Dtest=ProdutoProviderContractTest
   ```

4. **Iniciar os Microsserviços:**
   ```bash
   # Terminal 1 - Iniciar ms-produto na porta 8083
   ./mvnw -pl ms-produto spring-boot:run

   # Terminal 2 - Iniciar ms-pedido na porta 8082
   ./mvnw -pl ms-pedido spring-boot:run
   ```

5. **Disparar pedido e testar resiliência:**
   ```bash
   curl -i -X POST http://localhost:8082/interno/pedidos \
     -H "Content-Type: application/json" \
     -d '{
       "identificadorPedido": "550e8400-e29b-41d4-a716-446655440001",
       "dados": {
         "idCliente": 1,
         "itens": [
           {"produto": 1, "quantidade": 2}
         ]
       }
     }'
   ```