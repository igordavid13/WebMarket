# Auto-avaliação
WebMarket - Gestão de Pedidos e Estoque
Grupo: [Inserir nomes dos integrantes e papéis (ex: Arquiteto, Dev APIs/Dados, Dev Integração/Resiliência, Dev Testes/Segurança)]
Tema/domínio: Ecossistema simplificado de e-commerce com segregação entre gestão de pedidos de compra e catálogo/estoque de produtos com verificação de disponibilidade.
Versões: Quarkus 3.20.3 | Java 21

---

## Evidências por critério

### 1. Serviços e API REST
* **Nível auto-atribuído:** 2 (Proficiente)
* **Evidência:** 
  * Projeto multi-módulo Maven (`servico-pedido` e `servico-produto`).
  * Recursos REST implementados no Nível 2 do Modelo de Maturidade de Richardson, utilizando verbos HTTP semânticos (GET, POST, PUT, PATCH, DELETE) e códigos de status coerentes (200, 201 com cabeçalho `Location`, 204, 404, 409).
  * Injeção de dependências e escopo gerenciados nativamente por CDI (`@ApplicationScoped`).
  * **Classes:** `br.gov.caixa.webmarket.pedido.application.controller.PedidoResource` e `br.gov.caixa.webmarket.produto.application.controller.ProdutoResource`.
  * **Testes que provam:** `PedidoResourceTest.deveCriarPedidoComSucesso()`, `PedidoResourceTest.deveRetornar404ParaPedidoInexistente()`, `ProdutoResourceTest.deveCriarProdutoComLocation()`, `ProdutoResourceTest.deveExcluirProdutoComSucesso()`.

### 2. Integração entre serviços e tolerância a falhas
* **Nível auto-atribuído:** 0 (Em maturação - Previsto para a Aula 5 / 09/11)
* **Evidência:** Planejada a integração síncrona via MicroProfile REST Client (`ProdutoClient`) no `servico-pedido`, consumindo `servico-produto` com anotações de Fault Tolerance (`@Timeout`, `@Fallback`, `@CircuitBreaker`).

### 3. Persistência com Panache
* **Nível auto-atribuído:** 3 (Avançado)
* **Evidência:** 
  * Persistência com Hibernate ORM com Panache utilizando banco de dados H2 em memória (`jdbc:h2:mem:...`).
  * Adoção justificada do padrão **Active Record** (`PanacheEntityBase`) para permitir controle explícito da estratégia de chave primária através de `@SequenceGenerator` (`allocationSize = 1`, `initialValue = 10`), evitando colisões de chave com dados pré-carregados pelo `import.sql`.
  * Operações de escrita protegidas por transações explícitas via `@Transactional`.
  * Carga de dados de teste isolada para ambientes `%dev` e `%test`.
  * Implementação de consultas customizadas além do CRUD básico:
    * `Produto.findComEstoqueDisponivel()` e `Produto.findDisponivelParaVenda()`
    * `Pedido.findByStatus()`, `Pedido.findByCliente()` e `Pedido.countByStatus()`
  * **Classes:** `br.gov.caixa.webmarket.domain.entity.Produto`, `br.gov.caixa.webmarket.pedido.domain.entity.Pedido` e `ItemPedido`.
  * **Testes que provam:** `ProdutoResourceTest.deveFiltrarProdutosComEstoque()`, `ProdutoResourceTest.deveExcluirProdutoComSucesso()`, `PedidoResourceTest.deveFiltrarPedidosPorStatus()`.

### 4. Validação e tratamento de erros
* **Nível auto-atribuído:** 1 (Básico - Conclusão do corpo único prevista para Aula 6 / 11/11)
* **Evidência:** Validações de entrada aplicadas nas entidades e DTOs via Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@Min`, `@DecimalMin`, `@Valid`).
* **Testes que provam:** Validações automáticas disparadas nos endpoints POST/PUT.

### 5. Segurança
* **Nível auto-atribuído:** 0 (Em maturação - Previsto para as Aulas 7 e 8 / 13/11 e 16/11)
* **Evidência:** Planejada a autenticação com usuários em banco (BCrypt) e controle de acesso RBAC via `@RolesAllowed`.

### 6. Config, health e métricas
* **Nível auto-atribuído:** 3 (Avançado)
* **Evidência:** 
  * Segregação estrita de configurações em `application.properties` por perfis (`%dev`, `%test`, `%prod`), garantindo que o perfil `%prod` não utilize recriação destrutiva (`update`) nem carregue o `import.sql`.
  * URL do serviço consumido externalizada em propriedade (`servico.produto.url`).
  * Endpoints `/q/health/live` e `/q/health/ready` ativos com Health Checks customizados via MicroProfile Health (`PedidoHealthCheck` e `ProdutoHealthCheck`).
  * Registro e exportação de métricas próprias de negócio visíveis em `/q/metrics` com padrão Prometheus:
    * `pedidos_consultados_total`
    * `produtos_consultados_total`
  * **Classes:** `PedidoHealthCheck.java`, `ProdutoHealthCheck.java`, `PedidoResource.java`, `ProdutoResource.java`.
  * **Testes que provam:** `PedidoResourceTest.deveResponderHealthCheckUp()`, `PedidoResourceTest.deveExporMetricasPrometheus()`, `ProdutoResourceTest.deveResponderHealthCheckUp()`, `ProdutoResourceTest.deveExporMetricasPrometheus()`.

### 7. Testes
* **Nível auto-atribuído:** 2 (Proficiente)
* **Evidência:** 
  * Suíte automatizada com cobertura de caminhos felizes e caminhos de erro (404, estoque insuficiente, remoção).
  * Execução em memória sem dependência de Docker, brokers ou banco externo.
  * `mvn verify` executado com sucesso e status verde a partir da raiz do monorepo agregador.
  * **Contagem de testes:** 12 testes no total (6 no `servico-produto` e 6 no `servico-pedido`), todos passando.
  * **Classes:** `ProdutoResourceTest.java` e `PedidoResourceTest.java`.

### 8. Uso crítico de IA
* **Como usamos IA e o que validamos manualmente:** 
  * **Uso da IA:** A IA foi utilizada para auxiliar na refatoração e limpeza do projeto herdado do módulo BE-JV-010 (Spring Boot com mensageria assíncrona), eliminando dependências de RabbitMQ, Kafka e outbox pattern, e estruturando os recursos REST com Panache e anotações do MicroProfile/Micrometer.
  * **Onde a IA falhou / O que conferimos manualmente:** A IA inicialmente gerou entidades estendendo `PanacheEntity` padrão com identificadores autoincrementais começando em `1`. Isso provocou erro `500` por colisão de chave primária (`Unique index violation`) durante a execução do `POST /produtos`, pois o script `import.sql` já utilizava os IDs 1, 2 e 3. Identificamos e corrigimos o problema manualmente alterando para `PanacheEntityBase` e configurando um `@SequenceGenerator` iniciando em 10. Também validamos manualmente a adequação das dependências do Micrometer Prometheus frente às especificações do MicroProfile exigidas em sala.

### 9. Execução
* **Nível auto-atribuído:** 2 (Proficiente)
* **Como rodar:** 
  * Instruções 100% reproduzíveis sem necessidade de Docker:
    * Terminal 1: `mvnw quarkus:dev -pl servico-produto` (Porta 8081)
    * Terminal 2: `mvnw quarkus:dev -pl servico-pedido` (Porta 8080)
    * Validação da suíte: `mvnw clean verify` na raiz.

---

## Opcionais entregues
* [Se o grupo tiver 4 integrantes, listar o item opcional escolhido aqui; caso sejam 3 integrantes, manter apenas o escopo CORE].