# WebMarket - Arquitetura de Software v2

## DDD, Spring Boot, H2, Redis, RabbitMQ, Kafka, Outbox, idempotência, PACT e ADRs

> Projeto acadêmico com dois microsserviços: `ms-pedido` e `ms-produto`.

## 1. Objetivo

O WebMarket demonstra, de forma enxuta, os conteúdos das aulas:

- DDD estratégico e tático;
- bounded contexts e bancos segregados;
- comunicação síncrona e assíncrona;
- comando em fila e evento em tópico;
- consistência eventual e compensação;
- Redis como cache reconstruível do catálogo;
- idempotência HTTP e de consumidores;
- Transactional Outbox;
- retry, DLQ/DLT;
- contract testing com PACT;
- decisões documentadas por ADR.

## 2. Escopo

### 2.1 `ms-pedido`

Responsável por:

- criar pedidos já confirmados, consultar e cancelar pedidos antes da publicação do comando de baixa;
- consultar e preservar os preços dos produtos no momento da criação;
- aplicar a máquina de estados do agregado `Pedido`;
- garantir idempotência do `POST /pedidos`;
- persistir eventos na Outbox;
- publicar comandos no RabbitMQ e eventos no Kafka;
- reagir a falhas definitivas de estoque por compensação, cancelando o pedido.

### 2.2 `ms-produto`

Responsável por:

- cadastrar e consultar produtos;
- adicionar e consultar estoque;
- manter o banco como fonte da verdade do estoque físico;
- manter no Redis um cache de consultas de catálogo;
- baixar estoque de maneira atômica no banco;
- consumir comandos de baixa definitiva no RabbitMQ;
- garantir idempotência no processamento;
- publicar eventos de sucesso ou falha no Kafka.

### 2.3 Fora do escopo

- pagamento real;
- cálculo de frete;
- autenticação corporativa;
- promoções e cupons;
- alta disponibilidade de produção;
- event sourcing completo.

## 3. Linguagem ubíqua

| Termo | Significado |
|---|---|
| Produto | Bem disponível para compra. |
| Estoque físico | Quantidade persistida no banco do `ms-produto`. |
| Pedido | Agregado que reúne cliente, itens, preços e estado. |
| Preço | Valor unitário do produto preservado no pedido no momento da compra. |
| Confirmação | O pedido nasce confirmado quando sua criação é aceita. |
| Cancelamento | Encerramento do pedido sem conclusão da compra. |
| Compensação | Operação que desfaz um efeito anterior sem apagar seu histórico. |

## 4. Bounded contexts e agregados

### 4.1 Contexto Pedido

**Aggregate Root:** `Pedido`.

Objetos internos:

- `ItemPedido`;
- `Preco`;
- `StatusPedido`.

Cada `ItemPedido` guarda `produtoId`, `quantidade`, `precoUnitario` e `precoTotal`. O preço registrado no pedido não muda quando o preço do catálogo for alterado.

Invariantes:

- o pedido deve possuir pelo menos um item;
- toda quantidade deve ser positiva;
- o preço total deve ser a soma dos subtotais;
- toda criação aceita gera um pedido `CONFIRMADO`;
- apenas um pedido `CONFIRMADO` pode ser cancelado;
- objetos internos só mudam por operações da raiz `Pedido`.

Estados:

```text
CONFIRMADO -> CANCELADO
CONFIRMADO -> CANCELADO_POR_FALHA_ESTOQUE  (compensação)
```

### 4.2 Contexto Produto

**Aggregate Root:** `Produto`.

Value Objects sugeridos:

- `ProdutoId`;
- `Preco`;
- `Estoque`.

Invariantes:

- preço maior que zero;
- estoque físico nunca negativo;
- quantidade de movimentação maior que zero;
- produto inativo não aceita baixa de estoque;
- baixa definitiva não pode exceder o estoque físico.

## 5. Organização do projeto: um repositório, duas aplicações Spring Boot

O WebMarket será mantido em **um único repositório Git e um único projeto Maven pai**, organizado como um projeto Maven multi-module. Dentro desse repositório existirão **duas aplicações Spring Boot independentes**:

- `ms-pedido`: primeira aplicação Spring Boot;
- `ms-produto`: segunda aplicação Spring Boot.

Essa organização facilita a divisão do trabalho, a execução dos testes e o compartilhamento controlado dos pact files e da infraestrutura. Entretanto, o fato de os dois serviços estarem no mesmo repositório **não transforma os serviços em uma única aplicação**.

Cada microsserviço continuará possuindo:

- seu próprio `pom.xml`;
- seu próprio código-fonte;
- sua própria porta HTTP;
- seu próprio arquivo `application.yml` e, quando necessário, `application-local.yml`;
- seu próprio banco H2;
- seu próprio ciclo de inicialização;
- seus próprios testes;
- seu próprio empacotamento Spring Boot;
- possibilidade de execução e parada independentemente do outro.

### 5.1 Estrutura multi-module Maven

```text
webmarket/
├── pom.xml                         # Maven parent/aggregator
├── ms-pedido/
│   ├── pom.xml                     # Aplicação Spring Boot independente
│   └── src/
│       ├── main/java/
│       ├── main/resources/
│       │   └── application.yml
│       └── test/java/
├── ms-produto/
│   ├── pom.xml                     # Aplicação Spring Boot independente
│   └── src/
│       ├── main/java/
│       ├── main/resources/
│       │   └── application.yml
│       └── test/java/
├── pacts/                          # Contratos gerados e verificados
├── infra/                          # RabbitMQ, Kafka, Redis e scripts
├── docs/
│   ├── arquitetura.md
│   └── adr/
├── AVALIACAO.md
└── README.md
```

### 5.2 `pom.xml` raiz

O `pom.xml` da raiz funciona como agregador Maven. Ele não é uma aplicação Spring Boot executável.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>br.gov.caixa.webmarket</groupId>
    <artifactId>webmarket</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>ms-pedido</module>
        <module>ms-produto</module>
    </modules>
</project>
```

As versões de Java, Spring Boot, Pact e plugins Maven devem ser centralizadas no projeto pai quando isso não criar acoplamento de implementação entre os serviços.

### 5.3 Configuração independente do `ms-pedido` (YAML)

Arquivo `ms-pedido/src/main/resources/application.yml`:

```yaml
server:
  port: 8082
spring:
  application:
    name: ms-pedido
  datasource:
    url: jdbc:h2:mem:pedidos;DB_CLOSE_DELAY=-1

    driver-class-name: org.h2.Driver
    username: sa
    password: ""
```

Endpoints principais:

```text
http://localhost:8082/interno/pedidos
```

### 5.4 Configuração independente do `ms-produto` (YAML)

Arquivo `ms-produto/src/main/resources/application.yml`:

```yaml
server:
  port: 8083
spring:
  application:
    name: ms-produto

  datasource:
    url: jdbc:h2:file:./data/webmarket-produto;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: sa
```

Endpoints principais:

```text
http://localhost:8083/produtos
```

### 5.5 Como executar os dois serviços localmente

Cada aplicação pode ser iniciada separadamente, em terminais diferentes. A
configuração local simula a infraestrutura da POC.

Terminal 1:

```bash
./mvnw -Pms-pedido-local -pl ms-pedido -am -DskipTests install
./mvnw -Pms-pedido-local -pl ms-pedido spring-boot:run
```

Terminal 2:

```bash
./mvnw -Pms-produto-local -pl ms-produto spring-boot:run
```

O `ms-pedido` usa a porta `8082` e inicia o Qpid Broker-J embutido na porta
`5672`, simulando o RabbitMQ. O `ms-produto` usa a porta `8083` e conecta-se ao
AMQP em `localhost:5672` e ao Kafka em `localhost:9092`. Atualmente, apenas o
broker AMQP compatível é iniciado pelo `ms-pedido`; o Kafka ainda é um serviço
externo e precisa estar disponível em `localhost:9092`. Em produção, RabbitMQ e
Kafka devem ser serviços externos.

### 5.6 Exemplos de execução dos endpoints

#### `ProdutoController` (`ms-produto`)

Listar o catálogo:

```bash
curl http://localhost:8083/produtos
```

Consultar um produto:

```bash
curl http://localhost:8083/produtos/1
```

Resposta esperada:

```json
{
  "id": 1,
  "nome": "Notebook",
  "preco": 3500.00,
  "quantidadeEstoque": 10,
  "ativo": true
}
```

#### `GravadorController` (`ms-pedido`)

Gravar um pedido:

```bash
curl -X POST http://localhost:8082/interno/pedidos \
  -H 'Content-Type: application/json' \
  -d '{
    "identificadorPedido": "11111111-1111-1111-1111-111111111111",
    "dados": {
      "idCliente": 1001,
      "itens": [
        {
          "produto": 1,
          "quantidade": 2
        },
        {
          "produto": 2,
          "quantidade": 1
        }
      ]
    }
  }'
```

O endpoint retorna `200 OK` quando o pedido é recebido. O `ms-pedido` grava o
comando de baixa na Outbox para publicação no AMQP.

Consultar o pedido gravado:

```bash
curl http://localhost:8082/interno/pedidos/11111111-1111-1111-1111-111111111111
```

### 5.7 Empacotamento e execução dos artefatos

O build da raiz compila os dois módulos:

```bash
./mvnw clean verify
```

Cada módulo gera seu próprio artefato executável Spring Boot. Portanto, é possível executar apenas um serviço, atualizar um serviço sem alterar o outro e testar cada módulo isoladamente.

### 5.8 O que não será feito

Não será adotado o seguinte desenho:

```text
uma aplicação Spring Boot
└── porta 8080
    ├── /pedidos
    └── /produtos
```

Esse desenho seria um monólito modular, porque Pedido e Produto estariam na mesma JVM, no mesmo ciclo de deploy e no mesmo processo. O projeto do curso utiliza um monorepo multi-module, mas preserva duas fronteiras físicas de execução.

### 5.9 Compartilhamento de código

Os módulos não devem compartilhar entidades JPA, Aggregate Roots, repositories ou regras de domínio. Se existir código comum, ele deve se limitar a elementos técnicos pequenos e estáveis. Os contratos REST e de mensageria devem ser tratados como contratos versionados, não como classes de domínio compartilhadas que obriguem os dois serviços a evoluir juntos.

### 5.10 Relação com o PACT

A organização em um único repositório simplifica o PACT:

```text
ms-pedido
   └── gera pacts/ms-pedido-ms-produto.json

pacts/
   └── ms-pedido-ms-produto.json

ms-produto
   └── verifica o pact contra a API real
```

O pact file pode ser compartilhado pelo diretório `pacts/` durante o projeto acadêmico. Um Pact Broker somente será necessário se o grupo quiser demonstrar versionamento e compatibilidade entre builds independentes.

## 6. Arquitetura geral

```mermaid
flowchart LR
    C[Cliente] -->|REST| PED[ms-pedido]
    C -->|REST consulta| PRO[ms-produto]
    PED -->|REST: consulta produto/preço| PRO

    subgraph BC1[Bounded Context Pedido]
    PED --> PDB[(H2 Pedido)]
    PDB --> POUT[(OUTBOX_EVENTO_PEDIDO)]
    PR[Outbox Relay Pedido] --> KAFKA[(Kafka)]
    POUT --> PR
    PR -->|BaixarEstoqueCommand| RABBIT[(RabbitMQ)]
    end

    subgraph BC2[Bounded Context Produto]
        PRO --> PRODDB[(H2 Produto)]
        PRO --> REDIS[(Redis cache de consultas)]
      RABBIT --> PC[Consumidor idempotente]
      PC --> PRODDB
      PC --> EPROC[(EVENTO_PROCESSADO_PRODUTO)]
      PC --> EOUT[(OUTBOX_EVENTO_PRODUTO)]
      ER[Outbox Relay Produto] --> KAFKA
      EOUT --> ER
    end

    KAFKA -->|EstoqueBaixadoEvent| PED
    KAFKA -->|BaixaEstoqueFalhouEvent| PED
```

## 7. Escolha de RabbitMQ e Kafka

### RabbitMQ: comando e trabalho

A criação do pedido já o deixa confirmado e gera um trabalho com um dono lógico:

```text
BaixarEstoqueCommand
```

Esse comando vai para uma fila RabbitMQ porque:

- está no imperativo: "baixe o estoque";
- existe um responsável lógico, o `ms-produto`;
- deve ser processado por uma instância consumidora;
- precisa de retry e DLQ por mensagem;
- não exige replay histórico de longo prazo.

Topologia proposta:

```text
Exchange: webmarket.commands
Routing key: estoque.baixar
Fila: estoque.baixar.q
DLX: webmarket.commands.dlx
DLQ: estoque.baixar.dlq
```

### Kafka: fatos do domínio

Os resultados são fatos no passado:

- `EstoqueBaixadoEvent`;
- `BaixaEstoqueFalhouEvent`;
- `PedidoCanceladoEvent`.

Esses fatos vão para tópicos Kafka porque podem, no futuro, interessar a mais de um consumidor e podem ser retidos e reprocessados.

Tópicos propostos:

```text
estoque-baixado
baixa-estoque-falhou
pedido-cancelado
```

Chave de partição:

```text
pedidoId
```

Todos os eventos do mesmo pedido caem na mesma partição e preservam a ordem por pedido.

## 8. Redis e fonte da verdade

O banco do `ms-produto` é a fonte da verdade do estoque e dos produtos. O Redis é apenas um cache reconstruível para consultas do catálogo; ele não controla reservas nem disponibilidade para compra.

```text
Banco:
produto.quantidade_estoque = 8

Redis:
produto:{produtoId} = { nome, preco, estoque }
```

Uma baixa de estoque atualiza o banco e invalida o cache do produto afetado.

### Baixa atômica

O processamento de um comando com vários itens deve ser `all-or-nothing` no banco do `ms-produto`:

1. validar que todos os produtos existem e estão ativos;
2. validar que há estoque suficiente para todos os itens;
3. se algum item não puder ser atendido, não alterar nenhum produto;
4. se todos puderem ser atendidos, baixar todas as quantidades;
5. registrar o `eventId` processado na mesma transação;
6. gravar o evento de sucesso ou falha na Outbox.

O banco deve garantir a atomicidade da validação, das baixas e do registro idempotente.

### Reconstrução

Se o Redis for perdido, o `ms-produto` deve conseguir reconstruir o cache consultando os produtos no banco:

```text
produto e estoque físico persistidos no banco
```

O cache pode ser preenchido sob demanda (cache-aside) ou por rotina de inicialização.

## 9. Fluxos

### 9.1 Criar e confirmar pedido

```mermaid
sequenceDiagram
    actor Cliente
    participant Pedido as ms-pedido
    participant Produto as ms-produto
    participant BancoProduto as H2 Produto
    participant Outbox as Outbox Pedido
    participant Rabbit as RabbitMQ
    participant BancoPedido as H2 Pedido

    Cliente->>Pedido: POST /pedidos + Idempotency-Key
    Pedido->>Pedido: verifica chave e payload_hash
    Pedido->>Produto: consulta produtos e preços
    Produto->>BancoProduto: busca produtos ativos
    BancoProduto-->>Produto: dados e preços atuais
    Produto-->>Pedido: produtos e preços
    Pedido->>Pedido: valida itens e calcula precoTotal
    Pedido->>BancoPedido: grava Pedido CONFIRMADO + itens + Outbox
    BancoPedido-->>Pedido: COMMIT
    Pedido-->>Cliente: 201 pedidoId + precoTotal + CONFIRMADO
    Outbox->>Rabbit: publica BaixarEstoqueCommand
```

### 9.2 Baixar estoque após a criação

```mermaid
sequenceDiagram
    participant Pedido as ms-pedido
    participant BancoPedido as H2 Pedido
    participant Rabbit as RabbitMQ
    participant Produto as ms-produto
    participant BancoProduto as H2 Produto
    participant Outbox as Outbox Produto
    participant Kafka

    Rabbit->>Produto: BaixarEstoqueCommand
    Produto->>BancoProduto: valida e baixa itens + registra eventId
    alt sucesso
        Produto->>Outbox: grava EstoqueBaixadoEvent
        Outbox->>Kafka: publica evento
        Kafka->>Pedido: EstoqueBaixadoEvent
        Pedido->>BancoPedido: registra conclusão da baixa
    else falha definitiva
        Produto->>Outbox: grava BaixaEstoqueFalhouEvent
        Outbox->>Kafka: publica evento
        Kafka->>Pedido: BaixaEstoqueFalhouEvent
        Pedido->>BancoPedido: compensação para CANCELADO_POR_FALHA_ESTOQUE
    end
```

O status `CONFIRMADO` indica que o pedido foi aceito e persistido, não que a baixa de estoque já terminou. Como não existe reserva, pode ocorrer falta de estoque entre a consulta do catálogo e o processamento do comando; nesse caso, o pedido será cancelado por compensação depois da resposta inicial.

### 9.3 Cancelar pedido

1. `ms-pedido` valida que o pedido está `CONFIRMADO` e que o comando de baixa ainda está `PENDENTE` na Outbox.
2. Na mesma transação, altera o pedido para `CANCELADO` e impede a publicação do comando pendente.
3. Grava `PedidoCanceladoEvent` na Outbox para informar os demais interessados.

Depois que o comando de baixa tiver sido publicado, o cancelamento manual retorna conflito. Repor estoque após uma baixa concluída exigiria um fluxo compensatório adicional, que fica fora do escopo desta versão.

## 10. Idempotência

### 10.1 API de criação

Cabeçalho obrigatório:

```http
Idempotency-Key: 75dea32d-8d4d-43dc-a4fa-182e2abcaa30
```

Banco:

```sql
UNIQUE (idempotency_key)
```

Regras:

- chave nova: processar normalmente;
- chave existente + mesmo `payload_hash`: retornar o pedido anterior;
- chave existente + hash diferente: retornar `409 Conflict`;
- a repetição não pode criar outro pedido nem publicar outro comando de baixa.

### 10.2 Consumidor RabbitMQ

A tabela `EVENTO_PROCESSADO_PRODUTO` usa `event_id` como chave primária.

A mesma transação deve:

1. inserir `event_id`;
2. atualizar todos os produtos;
3. gravar evento de saída na Outbox do `ms-produto`;
4. efetuar commit.

Se o `event_id` já existir, o comando é considerado processado e não produz nova baixa.

### 10.3 Idempotência da baixa

A baixa usa `eventId` como chave única. Reprocessar o mesmo comando deve retornar o resultado anterior sem alterar o estoque novamente.

## 11. Outbox

Cada microsserviço publica eventos a partir da própria Outbox.

### Outbox do `ms-pedido`

Armazena:

- `BaixarEstoqueCommand` junto com a criação do pedido confirmado;
- `PedidoCanceladoEvent` após o cancelamento.

### Outbox do `ms-produto`

Armazena:

- `EstoqueBaixadoEvent`;
- `BaixaEstoqueFalhouEvent`.

O Relay pode publicar mais de uma vez se cair depois da publicação e antes de marcar `PUBLICADO`. Por isso todos os consumidores devem permanecer idempotentes.

## 12. Retry e DLQ/DLT

### RabbitMQ

- falha transitória: retry com backoff;
- falha permanente ou tentativas esgotadas: `estoque.baixar.dlq`;
- ack somente depois do commit local;
- fila durável, mensagem persistente e publisher confirms.

### Kafka

- retry por tópicos auxiliares ou mecanismo equivalente;
- tentativas esgotadas: DLT;
- commit do offset somente depois do processamento bem-sucedido;
- consumidor idempotente por `eventId`.

## 13. PACT: o que é e onde entra

PACT implementa consumer-driven contract testing. O consumidor declara exatamente o contrato de que precisa; o provedor executa o contrato contra a implementação real.

### 13.1 Contrato REST de consulta de produto

```text
Consumer: ms-pedido
Provider: ms-produto
Interação: GET /produtos/{produtoId}
```

O teste do `ms-pedido` gera o pact para consultar os dados e o preço atual do produto. O `ms-produto` verifica esse pact.

Contrato esperado:

```json
{
    "produtoId": "54e67e66-3e00-4dd3-923c-07943825c431",
    "nome": "Teclado",
    "preco": 150.00,
    "ativo": true
}
```

### 13.2 Organização sugerida no código

```text
ms-pedido/
├── src/main/java/.../infrastructure/client/ProdutoCatalogoClient.java
├── src/test/java/.../contract/ProdutoCatalogoConsumerPactTest.java
└── target/pacts/ms-pedido-ms-produto.json

ms-produto/
├── src/main/java/.../presentation/ProdutoResource.java
├── src/test/java/.../contract/ProdutoProviderPactTest.java
└── pacts/ms-pedido-ms-produto.json
```

### 13.3 Dependências Maven conceituais

Use os módulos Pact JVM compatíveis com a versão adotada no projeto:

```xml
<dependency>
    <groupId>au.com.dius.pact.consumer</groupId>
    <artifactId>junit5</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>au.com.dius.pact.provider</groupId>
    <artifactId>junit5</artifactId>
    <scope>test</scope>
</dependency>
```

A versão deve ser centralizada no `dependencyManagement` e validada contra as versões de Java e Spring Boot usadas pelo grupo.

### 13.4 Exemplo do consumidor Pact no `ms-pedido`

```java
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ms-produto", pactVersion = PactSpecVersion.V4)
class ProdutoCatalogoConsumerPactTest {

    @Pact(consumer = "ms-pedido")
    V4Pact consultarProduto(PactDslWithProvider builder) {
        var response = new PactDslJsonBody()
            .uuid("produtoId")
            .stringValue("nome", "Teclado")
            .decimalType("preco", 150.00)
            .booleanType("ativo", true);

        return builder
            .given("produto ativo existe")
            .uponReceiving("consulta de produto pelo identificador")
                .path("/produtos/54e67e66-3e00-4dd3-923c-07943825c431")
                .method("GET")
            .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(response)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "consultarProduto")
    void deveLerProduto(MockServer mockServer) {
        ProdutoCatalogoClient client = new ProdutoCatalogoClient(mockServer.getUrl());
        ProdutoResponse response = client.buscar("54e67e66-3e00-4dd3-923c-07943825c431");
        assertEquals("Teclado", response.nome());
    }
}
```

> Os nomes exatos de classes e métodos podem variar conforme a versão do Pact JVM. O conceito obrigatório é: o cliente REST real do `ms-pedido` deve chamar o `MockServer` do PACT, e o teste deve gerar o arquivo pact.

### 13.5 Exemplo de verificação no `ms-produto`

```java
@Provider("ms-produto")
@PactFolder("pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProdutoProviderPactTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void configurarTarget(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verificar(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("produto ativo existe")
    void prepararProduto() {
        // Semear produto ativo com preço previsível no banco de teste.
    }
}
```

No Spring Boot, o teste inicia o servidor em uma porta aleatória e injeta o valor com `@LocalServerPort`, evitando conflitos entre execuções.

### 13.6 Message Pact

Depois do contrato REST, o grupo pode adicionar um message pact para `BaixarEstoqueCommand`:

```text
Consumer da mensagem: ms-produto
Provider da mensagem: ms-pedido
```

O `ms-produto` declara o payload de que precisa. O `ms-pedido` é verificado como produtor do comando. Isso testa o schema sem precisar subir o RabbitMQ.

### 13.7 Execução do Pact implementado

O projeto possui atualmente um Pact REST implementado para a interação
`GET /produtos/1`:

- consumer: `ms-pedido`;
- provider: `ms-produto`;
- versão Pact JVM: `4.6.14`;
- cliente usado pelo consumer:
  `ms-pedido/src/main/java/br/gov/caixa/webmarket/gravador/infrastructure/client/ProdutoCatalogoClient.java`;
- teste consumer:
  `ms-pedido/src/test/java/br/gov/caixa/webmarket/gravador/contract/ProdutoCatalogoConsumerPactTest.java`;
- teste provider:
  `ms-produto/src/test/java/br/gov/caixa/webmarket/produto/contract/ProdutoProviderPactTest.java`.

Gerar o contrato no `ms-pedido`:

```bash
./mvnw -Pms-pedido-local -pl ms-pedido \
  -Dtest=ProdutoCatalogoConsumerPactTest test
```

O arquivo é gerado em:

```text
ms-pedido/target/pacts/ms-pedido-ms-produto.json
```

Compartilhar o arquivo com o provider:

```bash
mkdir -p ms-produto/src/test/resources/pacts
cp ms-pedido/target/pacts/ms-pedido-ms-produto.json \
   ms-produto/src/test/resources/pacts/ms-pedido-ms-produto.json
```

Verificar o contrato no `ms-produto`:

```bash
./mvnw -Pms-produto-local -pl ms-produto \
  -Dtest=ProdutoProviderPactTest test
```

O provider inicia uma aplicação Spring Boot em porta aleatória, usa H2 em
memória e semeia o estado `produto ativo existe`. Assim, a verificação não
depende dos processos locais do `ms-pedido`, do Qpid, do Kafka ou do cache de
execução.

## 14. ADR: o que é

ADR significa **Architecture Decision Record**, ou Registro de Decisão Arquitetural.

Um ADR não é uma documentação genérica da arquitetura. Cada arquivo registra uma decisão, seu contexto, alternativas e consequências.

Estrutura:

```markdown
# ADR-003: Usar RabbitMQ para baixa de estoque

## Status
Aceito

## Contexto
A criação do pedido confirmado precisa solicitar a baixa de estoque a um único responsável.
O processo precisa suportar retry e DLQ sem bloquear a resposta HTTP.

## Decisão
Usar comando assíncrono BaixarEstoqueCommand em uma fila RabbitMQ, publicado pela Outbox junto com o pedido confirmado.

## Alternativas consideradas
- REST síncrono: rejeitado porque acopla disponibilidade e latência.
- Kafka: rejeitado para este comando porque não precisamos de replay histórico.
- Atualização direta no banco de Produto: rejeitada porque viola a propriedade dos dados.

## Consequências
- Ganho: desacoplamento temporal, retry e DLQ.
- Custo: consistência eventual e infraestrutura adicional.
- Mitigação: idempotência, Outbox, monitoramento e compensação.
```

### ADRs obrigatórios propostos

```text
docs/adr/
├── ADR-001-bounded-contexts.md
├── ADR-002-bancos-segregados.md
├── ADR-003-rabbitmq-comando-baixa.md
├── ADR-004-kafka-eventos-dominio.md
├── ADR-005-redis-cache-catalogo.md
├── ADR-006-idempotencia.md
├── ADR-007-transactional-outbox.md
├── ADR-008-consistencia-eventual-compensacao.md
└── ADR-009-contract-testing-pact.md
```

## 15. Persistência com H2 no Spring Boot

Cada microsserviço possui sua própria instância lógica de H2. Não haverá banco compartilhado entre `ms-pedido` e `ms-produto`. Para o projeto acadêmico, a sugestão é executar H2 em modo arquivo no desenvolvimento, preservando os dados entre reinicializações, e usar H2 em memória nos testes automatizados.

### 15.1 Dependências Maven

No projeto Spring Boot com Spring Data JPA e Hibernate:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
</dependency>
```

O starter fornece Spring Data JPA e Hibernate ORM; o H2 é o driver JDBC usado em desenvolvimento e nos testes.

### 15.2 `ms-pedido/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:pedidos;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: ""

# Desenvolvimento acadêmico. Se Flyway/Liquibase for adotado, prefira validate/none.
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

### 15.3 `ms-produto/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/webmarket-produto;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: sa
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

Mesmo usando H2 local, os arquivos são diferentes. Isso preserva a regra arquitetural de banco por microsserviço.

### 15.4 Perfil de teste

Nos testes, cada serviço pode usar H2 em memória em `src/test/resources/application-test.yml` e ativar o perfil `test` com `@ActiveProfiles("test")`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:webmarket;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  jpa:
    hibernate:
      ddl-auto: create-drop
```

Como cada microsserviço executa sua própria suíte de testes, o nome `webmarket` pode ser reutilizado. Para testes que iniciem ambos no mesmo processo, use nomes diferentes, como `webmarket_pedido_test` e `webmarket_produto_test`.

### 15.5 Particularidades do H2

- `UUID` pode continuar como tipo das chaves.
- `NUMERIC(15,2)` continua adequado para armazenar preços.
- Para simplificar a Outbox no projeto acadêmico, o `payload` será armazenado como `CLOB`, contendo JSON serializado pela aplicação.
- `TIMESTAMP WITH TIME ZONE` substitui o `TIMESTAMPTZ` usado anteriormente.
- As constraints `PRIMARY KEY`, `UNIQUE`, `CHECK` e `FOREIGN KEY` continuam sendo parte da garantia de integridade e idempotência.
- Não utilizar recursos específicos de H2 nos repositories ou migrations.

> O H2 é suficiente para o objetivo acadêmico, mas o desenho continua evitando acoplamento ao banco: regras de domínio ficam fora de SQL e fora das entidades de infraestrutura sempre que possível.

## 16. Modelo de dados H2

### Banco H2 do `ms-pedido`

```sql
CREATE TABLE pedido (
    pedido_id UUID PRIMARY KEY,
    cliente_id UUID NOT NULL,
    preco_total NUMERIC(15,2) NOT NULL CHECK (preco_total >= 0),
    status VARCHAR(40) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    payload_hash VARCHAR(64) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE item_pedido (
    item_pedido_id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL REFERENCES pedido(pedido_id),
    produto_id UUID NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    preco_unitario NUMERIC(15,2) NOT NULL CHECK (preco_unitario > 0),
    preco_total NUMERIC(15,2) NOT NULL CHECK (preco_total > 0),
    UNIQUE (pedido_id, produto_id)
);

CREATE TABLE outbox_evento_pedido (
    event_id UUID PRIMARY KEY,
    agregado_id UUID NOT NULL,
    tipo_mensagem VARCHAR(100) NOT NULL,
    destino VARCHAR(120) NOT NULL,
    payload CLOB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    tentativas INTEGER NOT NULL DEFAULT 0,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    publicado_em TIMESTAMP WITH TIME ZONE
);
```

### Banco H2 do `ms-produto`

```sql
CREATE TABLE produto (
    produto_id UUID PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(500),
    preco NUMERIC(15,2) NOT NULL CHECK (preco > 0),
    quantidade_estoque INTEGER NOT NULL CHECK (quantidade_estoque >= 0),
    versao BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE evento_processado_produto (
    event_id UUID PRIMARY KEY,
    tipo_mensagem VARCHAR(100) NOT NULL,
    agregado_id UUID NOT NULL,
    processado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox_evento_produto (
    event_id UUID PRIMARY KEY,
    agregado_id UUID NOT NULL,
    tipo_evento VARCHAR(100) NOT NULL,
    destino VARCHAR(120) NOT NULL,
    payload CLOB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    tentativas INTEGER NOT NULL DEFAULT 0,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    publicado_em TIMESTAMP WITH TIME ZONE
);
```

## 17. Divisão detalhada entre 3 integrantes

### 17.1 Igor David Morais - `ms-pedido`, PACT consumidor e integração

### Implementação principal

- estruturar o projeto Spring Boot do `ms-pedido`;
- implementar `Pedido`, `ItemPedido`, `Preco` e `StatusPedido`;
- implementar `CriarPedidoUseCase`, `CancelarPedidoUseCase` e `ConsultarPedidoUseCase`;
- implementar os endpoints:
  - `POST /pedidos`;
  - `GET /pedidos/{id}`;
  - `POST /pedidos/{id}/cancelamento`;
- implementar o cliente REST `ProdutoCatalogoClient` para consultar produtos e preços;
- implementar `Idempotency-Key` e `payload_hash`;
- criar migrations do banco Pedido;
- implementar Outbox do `ms-pedido`;
- implementar relay para RabbitMQ e Kafka;
- produzir `BaixarEstoqueCommand`;
- consumir `EstoqueBaixadoEvent` e `BaixaEstoqueFalhouEvent`;
- aplicar compensação do pedido em falha definitiva;
- criar testes unitários do agregado e casos de uso;
- criar o teste PACT consumidor de consulta de produto;
- criar o provider do message pact de `BaixarEstoqueCommand`, se incluído.

### Stub/mock para trabalhar independentemente

#### Stub HTTP do `ms-produto`

Igor precisa de um stub configurável para consulta de catálogo:

```text
GET /produtos/{id} -> 200 com nome, preço e status
GET /produtos/{id} -> 404 PRODUTO_NAO_ENCONTRADO
GET /produtos/{id} -> 500 ERRO_INTERNO
GET /produtos/{id} -> timeout
```

Pode usar:

- WireMock;
- MockServer do PACT no teste de contrato;
- implementação fake de `ProdutoReservaPort` nos testes unitários.

Exemplo de fake:

```java
class ProdutoCatalogoFake implements ProdutoCatalogoPort {
    private ProdutoResumo produto;

    void responderCom(ProdutoResumo produto) {
        this.produto = produto;
    }

    @Override
    public ProdutoResumo buscar(UUID produtoId) {
        return produto;
    }
}
```

#### Fake de publicação

Criar uma porta:

```java
interface PublicadorMensagem {
    void publicar(Mensagem mensagem);
}
```

Nos testes, usar `PublicadorMensagemFake` que guarda as mensagens em memória. Assim Igor valida a Outbox e o comando gerado sem RabbitMQ ou Kafka.

#### Eventos falsos de retorno

Fixtures para:

- `EstoqueBaixadoEvent`;
- `BaixaEstoqueFalhouEvent`;
- evento duplicado;
- evento fora de ordem.

### Critério de conclusão individual

O `ms-pedido` deve consultar o Produto pelo stub, gerar o pact REST, provar idempotência HTTP e gravar o pedido confirmado junto com a mensagem de baixa na Outbox.

### 17.2 Davi Fernandez Franca Aragao - `ms-produto`, consumidor idempotente e PACT provider

### Implementação principal

- estruturar o projeto Spring Boot do `ms-produto`;
- implementar `Produto`, `Estoque`, `ProdutoId` e `Preco`;
- implementar casos de uso de cadastro, consulta, atualização de estoque e baixa;
- implementar endpoints:
  - `POST /produtos`;
  - `GET /produtos/{id}`;
  - `POST /produtos/{id}/estoque`;
    - `PATCH /produtos/{id}/preco`;
- criar migrations das tabelas Produto, Evento Processado e Outbox;
- consumir `BaixarEstoqueCommand` no RabbitMQ;
- gravar `eventId`, baixar estoque e criar evento de saída na mesma transação;
- implementar optimistic locking ou atualização condicional;
- produzir `EstoqueBaixadoEvent` e `BaixaEstoqueFalhouEvent` via Outbox;
- verificar o pact REST gerado pelo `ms-pedido`;
- criar o message pact consumidor de `BaixarEstoqueCommand`, se incluído;
- criar testes de concorrência e reprocessamento.

### Stub/mock para trabalhar independentemente

#### Fake do cache de catálogo

Enquanto Marcos implementa o cache Redis, Davi trabalha contra uma implementação em memória da porta de cache:

```java
interface ProdutoCachePort {
    Optional<ProdutoResumo> buscar(UUID produtoId);
    void armazenar(ProdutoResumo produto);
    void invalidar(UUID produtoId);
}
```

O cache não mantém saldo reservado nem participa da baixa do estoque.

Cenários de cache:

- cache hit e cache miss;
- invalidação após alteração de preço;
- invalidação após atualização ou baixa de estoque.

#### Stub de RabbitMQ

Nos testes unitários, chamar diretamente o método consumidor com um `BaixarEstoqueCommand` fixture. Para integração sem broker externo, usar o mecanismo de teste escolhido no perfil do projeto ou uma porta de entrada adaptável.

#### Fake de Outbox

Usar `OutboxRepositoryFake` para comprovar que sucesso e falha geram os eventos certos sem Kafka.

#### Stub de eventos

Fixtures para:

- comando de baixa válido;
- comando duplicado com mesmo `eventId`;
- comando com estoque insuficiente.

### Critério de conclusão individual

O `ms-produto` deve passar nos provider tests do PACT, processar duas vezes o mesmo comando com uma única baixa e invalidar o cache quando o produto mudar.

### 17.3 Marcos da Silva Ferreira - cache Redis, infraestrutura, resiliência e testes integrados

### Implementação principal

- definir chaves e estruturas Redis para cache do catálogo;
- implementar cache-aside e invalidação por produto;
- integrar o adapter Redis à porta de cache definida no `ms-produto`;
- criar Docker Compose com:
  - H2 do `ms-pedido`;
  - H2 do `ms-produto`;
  - Redis;
  - RabbitMQ;
  - Kafka;
- configurar fila, exchange, binding, DLX e DLQ;
- configurar tópicos Kafka, retry e DLT;
- padronizar `.env.example`, portas e health checks;
- criar coleção Bruno/Postman ou testes RestAssured;
- executar testes ponta a ponta;
- consolidar README, ADRs e roteiro da apresentação;
- monitorar e demonstrar DLQ/DLT;
- preparar evidências da baixa concorrente e do reprocessamento idempotente.

### Stub/mock para trabalhar independentemente

#### Stub do `ms-pedido`

Marcos precisa de scripts/fixtures que chamem:

```text
GET /produtos/{id}
POST /produtos
POST /produtos/{id}/estoque
```

sem exigir o `ms-pedido` real.

Pode usar:

- arquivos HTTP do VS Code/IntelliJ;
- coleção Bruno/Postman;
- teste RestAssured direto no `ms-produto`;
- pequeno producer de mensagens de teste.

#### Producer fake de RabbitMQ

Publicar fixtures de `BaixarEstoqueCommand`:

- comando válido;
- comando duplicado;
- comando malformado;
- comando que provoca falha transitória;
- comando que esgota retry e vai para DLQ.

#### Consumidor espião

Criar um consumidor de teste que capture:

- `EstoqueBaixadoEvent`;
- `BaixaEstoqueFalhouEvent`.

O spy deve guardar os eventos para assertions do teste de integração.

#### Banco e APIs simulados

- migrations com dados conhecidos;
- seed de produtos;
- scripts de reset do ambiente;
- perfil de testes com portas e bancos isolados.

### Critério de conclusão individual

A infraestrutura deve subir de forma reproduzível; a baixa deve ser atômica e idempotente; mensagens inválidas devem chegar à DLQ/DLT; e o fluxo integrado deve ser demonstrável mesmo usando stubs nas pontas.

## 18. Matriz de responsabilidades

| Entrega | Responsável | Revisor | Teste independente |
|---|---|---|---|
| Aggregate e API Pedido | Igor | Davi | Fake `ProdutoCatalogoPort` |
| Idempotência HTTP | Igor | Marcos | Banco de teste + requisição repetida |
| Outbox Pedido | Igor | Davi | `PublicadorMensagemFake` |
| Pact consumer REST | Igor | Davi | Pact MockServer |
| Aggregate e API Produto | Davi | Igor | Fake `ProdutoCachePort` |
| Consumidor Rabbit idempotente | Davi | Marcos | chamada direta + fixtures |
| Pact provider REST | Davi | Igor | pact file do `ms-pedido` |
| Cache Redis | Marcos | Davi | chamadas REST/adapter isolado |
| Docker Compose | Marcos | Igor | smoke test dos health checks |
| RabbitMQ/DLQ | Marcos | Davi | producer fake |
| Kafka/DLT | Marcos | Igor | producer e consumer spy |
| E2E | Marcos | Igor e Davi | ambiente integrado |
| ADRs e README | Marcos consolida | Todos | revisão cruzada |

## 19. Ordem de implementação

### Etapa 1 - Contratos

Todos juntos:

1. fechar DTOs REST;
2. fechar schemas de comando e eventos;
3. definir rotas, tópicos e fila;
4. definir DDL;
5. criar ADRs iniciais.

### Etapa 2 - Desenvolvimento paralelo

- Igor usa fake do catálogo e PACT MockServer;
- Davi usa fake do cache e mensagens fixture;
- Marcos usa requisições e producers fake.

### Etapa 3 - Contract tests

1. Igor gera o pact REST;
2. Davi copia/publica o pact no `ms-produto`;
3. Davi executa a verificação do provider;
4. pipeline falha se o contrato não for honrado.

### Etapa 4 - Integração

1. trocar fake do cache pelo adapter Redis;
2. trocar stub REST pelo `ms-produto` real;
3. integrar RabbitMQ;
4. integrar Kafka;
5. validar Outbox e consumidores.

### Etapa 5 - Robustez

- repetição da mesma request;
- repetição do mesmo comando;
- falha transitória;
- poison message;
- DLQ/DLT;
- baixa concorrente;
- compensação.

## 20. Testes de aceitação

- pedido válido nasce confirmado e preserva os preços obtidos no catálogo;
- pedido e comando de baixa são gravados na mesma transação local via Outbox;
- baixa de vários itens é atômica: ou todos são baixados, ou nenhum é alterado;
- mesma `Idempotency-Key` e mesmo payload retornam o mesmo pedido;
- mesma chave com payload diferente retorna conflito;
- reprocessar o comando não baixa estoque novamente;
- falha definitiva produz evento de falha e compensação;
- cancelamento antes da publicação do comando impede sua baixa;
- mudança de preço ou estoque invalida o cache do produto;
- poison message vai para DLQ;
- evento Kafka esgota retry e vai para DLT;
- PACT consumidor gera contrato;
- PACT provider verifica contrato;
- duas baixas concorrentes não deixam o estoque negativo;
- repetir o comando de baixa não altera o estoque novamente;
- Redis pode ser reconstruído consultando o cadastro e o estoque persistidos no banco.

## 21. Estrutura de repositório sugerida

```text
webmarket/
├── pom.xml                         # parent multi-module
├── ms-pedido/
│   ├── src/main/java/.../domain
│   ├── src/main/java/.../application
│   ├── src/main/java/.../infrastructure
│   ├── src/main/java/.../presentation
│   ├── src/test/java/.../unit
│   ├── src/test/java/.../contract
│   └── src/test/java/.../integration
├── ms-produto/
│   ├── src/main/java/.../domain
│   ├── src/main/java/.../application
│   ├── src/main/java/.../infrastructure
│   ├── src/main/java/.../presentation
│   ├── src/test/java/.../unit
│   ├── src/test/java/.../contract
│   └── src/test/java/.../integration
├── pacts/
├── infra/
│   ├── docker-compose.yml
│   ├── rabbitmq/
│   ├── kafka/
│   └── scripts/
├── docs/
│   ├── arquitetura.md
│   └── adr/
├── AVALIACAO.md
└── README.md
```

## 22. Definição de pronto

O projeto estará pronto quando:

- o repositório possuir um Maven parent com os módulos `ms-pedido` e `ms-produto`;
- os dois serviços subirem independentemente, em processos e portas diferentes;
- cada serviço possuir banco próprio;
- o Redis for tratado apenas como cache reconstruível do catálogo;
- o PACT REST for gerado pelo consumidor e verificado pelo provedor;
- RabbitMQ processar o comando com consumidor idempotente e DLQ;
- Kafka publicar/consumir fatos com retry e DLT;
- Outbox impedir perda lógica entre banco e broker;
- a mesma request e a mesma mensagem não duplicarem efeitos;
- a compensação de falha definitiva funcionar;
- os ADRs registrarem alternativas e trade-offs;
- o README permitir reproduzir a execução;
- o `AVALIACAO.md` apontar cada critério para sua evidência no repositório.
