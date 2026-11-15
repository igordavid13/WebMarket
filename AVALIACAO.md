# Auto-avaliação

**WebMarket - Gestão de Pedidos e Estoque**
* **Grupo:** Davi Fernandez, Igor David, Marcos Ferreira
* **Tema/domínio:** Ecossistema simplificado de e-commerce com segregação entre gestão de pedidos de compra (com controle de posse por cliente) e catálogo/estoque de produtos com verificação síncrona de disponibilidade.
* **Versões:** Quarkus 3.20.3 | Java 21

---

## Evidências por critério

### 1. Serviços e API REST
* **Nível auto-atribuído:** 3 (Avançado)
* **Evidência:**
  * Projeto multi-módulo Maven (`servico-pedido` na porta 8080 e `servico-produto` na porta 8081) sob um `pom.xml` agregador raiz.
  * Arquitetura em três camadas estritas (`Resource -> Service -> Repository`), com regras de negócio e transações isoladas nos serviços.
  * Recursos REST implementados no Nível 2 do Modelo de Maturidade de Richardson, utilizando verbos HTTP semânticos (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`) e códigos de status coerentes (`200`, `201` com cabeçalho `Location`, `204`, `400`, `401`, `403`, `404`, `409`, `422`, `503`).
  * Injeção de dependências e escopo gerenciados nativamente por CDI (`@ApplicationScoped` e `@Inject`).
  * **Classes:** `br.gov.caixa.webmarket.pedido.application.controller.PedidoResource` e `br.gov.caixa.webmarket.application.controller.ProdutoResource`.
  * **Testes que provam:** `PedidoResourceTest.deveCriarPedidoComSucesso()`, `PedidoResourceTest.deveRetornar404ParaPedidoInexistente()`, `ProdutoResourceTest.deveCriarProdutoComLocation()`, `ProdutoResourceTest.deveExcluirProdutoComSucesso()`.

### 2. Integração entre serviços e tolerância a falhas
* **Nível auto-atribuído:** 3 (Avançado)
* **Evidência:**
  * Comunicação síncrona via MicroProfile REST Client (`ProdutoClient`) no `servico-pedido`, com URL externalizada em `quarkus.rest-client.produtos.url` e autenticação propagada via `ProdutoAuthHeaderFactory`.
  * Separação explícita entre respostas de negócio e falhas técnicas via `@ClientExceptionMapper`: produto inexistente (`404`) ou estoque insuficiente (`409`) viram `ProdutoIndisponivelNegocioException` (HTTP `422`), configurada com `abortOn` no `@Retry` e `skipOn` no `@CircuitBreaker` e `@Fallback`.
  * Falhas técnicas (timeout de `500 ms`, recusa de conexão ou `5xx`) são protegidas por `@Timeout`, `@Retry(maxRetries = 2)`, `@CircuitBreaker(requestVolumeThreshold = 4)` e `@Fallback` no `ProdutoGateway`, devolvendo HTTP `503` sem persistir o pedido.
  * **Classes:** `ProdutoClient.java`, `ProdutoAuthHeaderFactory.java`, `ProdutoGateway.java`.
  * **Testes que provam:** `ProdutoGatewayTest.falhaTecnicaTentaTresVezesEDevolve503SemGravar()`, `ProdutoGatewayTest.erroDeNegocioDevolve422SemRetry()`, `ProdutoGatewayTest.circuitoAbertoNemChamaServicoDeProdutos()`.

### 3. Persistência com Panache
* **Nível auto-atribuído:** 3 (Avançado)
* **Evidência:**
  * Persistência com Hibernate ORM com Panache utilizando instâncias isoladas de banco H2 em memória (`jdbc:h2:mem:...`).
  * Padrão **Repository (`PanacheRepository<T>`)** adotado em ambos os serviços, mantendo as entidades como POJOs JPA limpos, com justificativa documentada no `README.md`.
  * Estratégia de geração de IDs via `@SequenceGenerator(initialValue = 10, allocationSize = 1)` para evitar colisões com os registros iniciais carregados pelo `import.sql`.
  * Restrição única no banco (`@UniqueConstraint`) para impedir produtos duplicados por nome.
  * Transações gerenciadas explicitamente nos métodos de escrita dos serviços via `@Transactional` (`jakarta.transaction`).
  * Consultas customizadas além do CRUD básico:
    * `ProdutoRepository.findComEstoqueDisponivel()`, `findByNome()` e `findDisponivelParaVenda()`.
    * `PedidoRepository.findByStatus()`, `findByCliente()` e `countByStatus()`.
  * **Classes:** `ProdutoRepository.java`, `ProdutoService.java`, `PedidoRepository.java`, `PedidoService.java`.
  * **Testes que provam:** `ProdutoRepositoryTest.deveBuscarProdutosComEstoqueDisponivel()`, `ProdutoRepositoryTest.deveBuscarPorNome()`, `PedidoRepositoryTest.deveBuscarPedidosPorStatus()`, `PedidoRepositoryTest.deveContarPedidosPorStatus()`.

### 4. Validação e tratamento de erros
* **Nível auto-atribuído:** 3 (Avançado)
* **Evidência:**
  * Validações de entrada aplicadas na fronteira dos recursos com `@Valid` e anotações Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@NotEmpty`, `@Min`, `@DecimalMin`), com mensagens em português configuradas em `ValidationMessages.properties` e `quarkus.default-locale=pt-BR`.
  * Exceções de domínio puras (`RuntimeException`, desacopladas de classes HTTP) traduzidas centralmente via `@ServerExceptionMapper` na classe `ExceptionMappers` de cada serviço.
  * Corpo JSON único (`ErroResposta` com `status`, `erro` e `mensagens`) padronizado para todos os erros da API (`400`, `403`, `404`, `409`, `422`, `503`), sem mapper genérico de `Throwable` que mascare erros nativos do Quarkus.
  * **Classes:** `ErroResposta.java`, `ExceptionMappers.java`, `ProdutoNaoEncontradoException.java`, `ProdutoDuplicadoException.java`, `EstoqueInsuficienteException.java`, `PedidoNaoEncontradoException.java`, `ProdutoIndisponivelNegocioException.java`, `ServicoProdutoIndisponivelException.java`.
  * **Testes que provam:** `ProdutoResourceTest.deveRetornar400ComCorpoPadrao()`, `ProdutoResourceTest.deveRetornar409ProdutoDuplicado()`, `PedidoResourceTest.deveRetornar400PedidoInvalido()`, `PedidoResourceTest.deveRetornar404ParaPedidoInexistente()`, `ProdutoGatewayTest.erroDeNegocioDevolve422SemRetry()`.

### 5. Segurança
* **Nível auto-atribuído:** 3 (Avançado)
* **Evidência:**
  * Autenticação HTTP Basic (`quarkus.http.auth.basic=true`) integrada ao banco de dados H2 via `quarkus-security-jpa` (`@UserDefinition`), com senhas armazenadas exclusivamente como hash **BCrypt** (`BcryptUtil.bcryptHash`) na inicialização (`CargaDeUsuarios`).
  * Autorização baseada em papéis (RBAC) com JSR-250 (`@DenyAll` na classe dos recursos e abertura granular por `@RolesAllowed` para `cliente` e `admin`).
  * Regra de posse (autorização baseada no dado) implementada no `PedidoService` via `SecurityIdentity`: usuários com papel `cliente` só listam, consultam e criam pedidos em seu próprio nome (recebendo `403 Forbidden` ao tentar acessar ou criar pedidos de terceiros), enquanto `admin` possui visão irrestrita.
  * Segurança por caminho no `application.properties`: `/q/health` aberto (`policy=permit`) para orquestradores e `/q/metrics` restrito ao papel `admin` (`policy=somente-admin`).
  * CORS configurado para origem `http://localhost:3000`.
  * **Classes:** `Usuario.java`, `UsuarioRepository.java`, `CargaDeUsuarios.java`, `Papeis.java`, `PedidoService.java`.
  * **Testes que provam:** `AutenticacaoTest.semCredencialDevolve401ComDesafioBasic()`, `AutenticacaoTest.senhaErradaDevolve401()`, `AutenticacaoTest.credencialCorretaDevolve200()`, `AutenticacaoTest.senhaFicaGuardadaComoHashBcrypt()`, `AutorizacaoTest.clientePodeLerMasNaoAlterarProdutos()`, `AutorizacaoTest.clienteRecebe403AoAcessarPedidoDeOutro()`, `AutorizacaoTest.metricasRestritasAoAdmin()`.

### 6. Config, health e métricas
* **Nível auto-atribuído:** 3 (Avançado)
* **Evidência:**
  * Segregação estrita de configurações em `application.properties` por perfis (`%dev`, `%test`, `%prod`), garantindo que `%prod` utilize `update` e não execute scripts destrutivos ou cargas de teste, e que `%test` use `quarkus.http.test-port=0`.
  * URL do serviço consumido externalizada em `quarkus.rest-client.produtos.url`.
  * Endpoints `/q/health/live` e `/q/health/ready` ativos com Health Checks customizados via MicroProfile Health (`PedidoHealthCheck` e `ProdutoHealthCheck`).
  * Métricas próprias de negócio registradas com `@Counted` e expostas em `/q/metrics` no formato Prometheus (`pedidos_consultados_total` e `produtos_consultados_total`).
  * **Classes:** `PedidoHealthCheck.java`, `ProdutoHealthCheck.java`, `PedidoResource.java`, `ProdutoResource.java`.
  * **Testes que provam:** `PedidoResourceTest.deveResponderHealthCheckUp()`, `PedidoResourceTest.deveExporMetricasPrometheus()`, `ProdutoResourceTest.deveResponderHealthCheckUp()`, `ProdutoResourceTest.deveExporMetricasPrometheus()`.

### 7. Testes
* **Nível auto-atribuído:** 3 (Avançado)
* **Evidência:**
  * Suíte automatizada completa com `@QuarkusTest`, REST-assured, H2 em memória e `@InjectMock @RestClient`, sem dependência de Docker ou serviços externos rodando em paralelo.
  * Cobertura de caminhos felizes, regras de repositório, falhas técnicas de rede (retry/circuit breaker/fallback), validações (`400`), conflitos (`409`), regras de negócio (`422`), autenticação (`401`) e autorização por papel e posse (`403`).
  * **Contagem real de testes:** **34 testes executados com status verde (`0 falhas, 0 erros`)** no build agregador (`mvn clean verify`), sendo 21 testes no `servico-pedido` e 13 testes no `servico-produto`.

### 8. Uso crítico de IA
* **Como usamos IA e o que validamos manualmente:**
  * **Uso da IA:** A IA auxiliou no porte do projeto legado em Spring Boot (BE-JV-010) para Quarkus, removendo dependências assíncronas (RabbitMQ, Kafka e Outbox Pattern) e estruturando a arquitetura em camadas (`Resource -> Service -> Repository`), mapeadores de exceção e suítes de teste.
  * **Onde a IA falhou / O que conferimos manualmente:**
    1. *Colisão de IDs com `import.sql`:* A IA inicialmente utilizou `PanacheEntity` padrão, causando violação de chave única (`500`) nos testes de `POST` devido aos IDs 1 a 3 já existirem no `import.sql`. Corrigimos manualmente configurando `@SequenceGenerator(initialValue = 10)` nas entidades.
    2. *Proxy de Mock no REST Client:* Ao adicionar `@ClientHeaderParam` com método `default` na interface `ProdutoClient`, os testes com `@InjectMock @RestClient` falharam com `TestInstantiationException`. Identificamos o conflito de proxy CDI e isolamos o cabeçalho de autenticação em uma `ClientHeadersFactory` dedicada (`ProdutoAuthHeaderFactory`).
    3. *Separação de contexto nos testes:* Validamos manualmente o isolamento de endpoints entre os módulos `servico-pedido` e `servico-produto` e a limpeza de estado do `CircuitBreakerMaintenance` entre execuções de teste.

### 9. Execução
* **Nível auto-atribuído:** 2 (Proficiente)
* **Como rodar:**
  * Execução 100% local e reproduzível sem Docker:
    * Terminal 1: `.\mvnw.cmd quarkus:dev -pl servico-produto` (Porta 8081)
    * Terminal 2: `.\mvnw.cmd quarkus:dev -pl servico-pedido` (Porta 8080)
    * Build e verificação completa: `.\mvnw.cmd clean verify` na raiz.

---

## Opcionais entregues
* Não aplicável — grupo composto por **3 integrantes**, mantendo o foco integral na entrega completa e validada do escopo CORE.