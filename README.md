# WebMarket - Quarkus

Monorepo Maven com **Java 21** e **Quarkus 3.20.3** (compatível com a especificação 3.15.7+ do módulo PE-JV-001). A raiz agrega os módulos; cada microsserviço roda de forma independente, com banco de dados H2 em memória, observabilidade, validação padronizada, segurança (Basic Auth + JPA/BCrypt + RBAC/Posse) e comunicação síncrona via REST Client.

---

## 1. Tema e Problema

O **WebMarket** é uma aplicação de e-commerce dividida em dois microsserviços:
- **`servico-pedido`**: Gerencia o ciclo de vida dos pedidos de compra e seus respectivos itens, aplicando regras de posse por cliente e validando disponibilidade no catálogo.
- **`servico-produto`**: Controla o catálogo de produtos e os saldos disponíveis em estoque.

---

## 2. Porte do Módulo Anterior (BE-JV-010 para Quarkus)

No módulo anterior (Spring Boot), o projeto utilizava mensageria assíncrona e recursos específicos do ecossistema Spring. Para atender às diretrizes da banca, foram realizadas as seguintes substituições arquiteturais:

| Conceito | No Projeto Anterior (Spring / BE-JV-010) | No Projeto Atual (Quarkus / PE-JV-001) |
| :--- | :--- | :--- |
| **Comunicação entre Serviços** | Mensageria assíncrona com RabbitMQ, Kafka e Outbox Pattern (exigindo Docker) | Chamadas HTTP síncronas diretas via **MicroProfile REST Client** (`ProdutoClient`) |
| **Consultas Derivadas** | `findByStatus(...)` / `findByClienteNomeContainingIgnoreCase(...)` (Spring Data) | Métodos escritos explicitamente em `PedidoRepository` e `ProdutoRepository` com Panache (`find(...)`) |
| **Tratamento Global de Erros** | `@RestControllerAdvice` + `@ExceptionHandler` + `ResponseEntity` | Classe `ExceptionMappers` com `@ServerExceptionMapper` retornando `RestResponse<ErroResposta>` |
| **Resiliência** | Resilience4j | MicroProfile Fault Tolerance (`@Timeout`, `@Retry`, `@CircuitBreaker`, `@Fallback`) no `ProdutoGateway` |
| **Segurança** | Spring Security (`SecurityFilterChain` + `@PreAuthorize`) | Quarkus Security JPA (`@UserDefinition` + `BcryptUtil`), JSR-250 (`@DenyAll`, `@RolesAllowed`) e regra de posse via `SecurityIdentity` |

---

## 3. Estrutura do Repositório (Multi-Módulo)

```text
webmarket/
├── pom.xml                 # POM agregador raiz (multi-módulo)
├── README.md               # Instruções de configuração, contratos e execução
├── AVALIACAO.md            # Autoavaliação e mapa de evidências por critério
├── docs/
│   └── arquitetura.md      # Desenho dos serviços, portas, fluxo síncrono e matriz RBAC
├── servico-pedido/         # Microsserviço de pedidos (porta 8080)
└── servico-produto/        # Microsserviço de catálogo e estoque (porta 8081)
```

---

## 4. Arquitetura e Comunicação

```text
[ Cliente HTTP ]
       │
       ▼ (POST /pedidos - Basic Auth)
[ servico-pedido ] (Porta 8080)
       │
       │ Consulta síncrona (GET /produtos/{id}/disponibilidade)
       │ com @Timeout, @Retry, @CircuitBreaker e @Fallback
       ▼
[ servico-produto ] (Porta 8081)
       │
       ▼
  [ H2 em Memória ]
```

- **`servico-pedido` (8080):** Expõe recursos para criar, listar com filtros, consultar e gerenciar pedidos.
- **`servico-produto` (8081):** Expõe recursos para cadastro de catálogo, verificação de saldos e controle de estoque.
- **Banco de Dados:** Cada microsserviço possui sua própria instância isolada de H2 Database em memória (`jdbc:h2:mem:...`), inicializada com dados de teste em `%dev` e `%test` via `import.sql` e usuários via `CargaDeUsuarios`.

---

## 5. Decisões Arquiteturais de Persistência

### Padrão Adotado: Panache Repository vs. Active Record
Optou-se pelo padrão **Repository (`PanacheRepository<T>`)** em vez de Active Record por:
1. **Separação Estrita de Responsabilidades:** As entidades (`Produto`, `Pedido`, `ItemPedido`, `Usuario`) permanecem como POJOs JPA sem métodos estáticos acoplados ao banco.
2. **Camadas Claras:** O fluxo segue rigorosamente `Recurso REST -> Serviço de Domínio -> Repositório Panache`, centralizando transações (`@Transactional`) e regras de posse nos serviços.
3. **Testabilidade:** Permite criar testes específicos para a camada de persistência (`ProdutoRepositoryTest`, `PedidoRepositoryTest`) e injetar dependências via CDI (`@ApplicationScoped` e `@Inject`).

### Estratégia de Geração de Identificadores
Foi adotado `@SequenceGenerator` com sequence própria (`initialValue = 10` e `allocationSize = 1`). Essa estratégia evita colisões de chave primária com os registros de carga inicial definidos no `import.sql` (IDs 1 a 3) e garante compatibilidade transparente com bancos relacionais em produção.

---

## 6. Contratos REST e Formato de Erro

Todos os erros da API (validação `400`, autenticação `401`, autorização `403`, não encontrado `404`, conflito `409`, regra de negócio `422` e indisponibilidade `503`) devolvem um corpo JSON único padronizado via `ErroResposta`:
```json
{
  "status": 422,
  "erro": "Produto indisponível",
  "mensagens": ["Estoque insuficiente para o produto solicitado"]
}
```

### `servico-produto` (Porta 8081)
| Operação | Verbo | URI | Papéis Permitidos | Sucesso | Erros |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Listar produtos | `GET` | `/produtos?somenteDisponiveis={bool}` | `cliente`, `admin` | `200 OK` | `401`, `403` |
| Consultar por ID | `GET` | `/produtos/{id}` | `cliente`, `admin` | `200 OK` | `401`, `403`, `404` |
| Checar disponibilidade | `GET` | `/produtos/{id}/disponibilidade?quantidade={q}` | `cliente`, `admin` | `200 OK` | `401`, `403`, `404`, `409` |
| Cadastrar produto | `POST` | `/produtos` | `admin` | `201 Created` (`Location`) | `400`, `401`, `403`, `409` |
| Atualizar produto | `PUT` | `/produtos/{id}` | `admin` | `200 OK` | `400`, `401`, `403`, `404` |
| Excluir produto | `DELETE` | `/produtos/{id}` | `admin` | `204 No Content` | `401`, `403`, `404` |

### `servico-pedido` (Porta 8080)
| Operação | Verbo | URI | Papéis e Regra de Posse | Sucesso | Erros |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Listar pedidos | `GET` | `/pedidos?status={s}&cliente={c}` | `cliente` (só os próprios) / `admin` (todos) | `200 OK` | `401`, `403` |
| Consultar por ID | `GET` | `/pedidos/{id}` | `cliente` (só o próprio) / `admin` (todos) | `200 OK` | `401`, `403`, `404` |
| Criar pedido | `POST` | `/pedidos` | `cliente` (em próprio nome) / `admin` | `201 Created` (`Location`) | `400`, `401`, `403`, `422`, `503` |
| Atualizar status | `PATCH` | `/pedidos/{id}/status?novoStatus={s}` | `admin` | `200 OK` | `400`, `401`, `403`, `404` |
| Excluir pedido | `DELETE` | `/pedidos/{id}` | `admin` | `204 No Content` | `401`, `403`, `404` |

---

## 7. Política de Tolerância a Falhas (Fault Tolerance)

Na integração síncrona entre `servico-pedido` e `servico-produto` (`ProdutoGateway`), distinguimos falhas técnicas de respostas de negócio:
- **Respostas de Negócio (`404` produto inexistente ou `409` estoque insuficiente):** Mapeadas via `@ClientExceptionMapper` para `ProdutoIndisponivelNegocioException`, resultando em **HTTP 422** imediato. Como repetir a consulta não altera o saldo nem cria o produto, configuramos `abortOn` no `@Retry` e `skipOn` no `@CircuitBreaker` e `@Fallback`.
- **Falhas Técnicas (Timeout de 500 ms, recusa de conexão ou HTTP `5xx`):** Tratadas como instabilidades transitórias. A operação `GET` de consulta de estoque é idempotente e segura para retentativas (`@Retry(maxRetries = 2)`). Caso 4 chamadas falhem (`requestVolumeThreshold = 4`), o `@CircuitBreaker` abre para evitar esgotamento de threads, acionando o `@Fallback` que devolve **HTTP 503 Service Unavailable** sem persistir o pedido.

---

## 8. Segurança, Usuários de Teste e Observabilidade

A autenticação utiliza **HTTP Basic** com credenciais persistidas no banco H2 via `quarkus-security-jpa` e hash **BCrypt** (`BcryptUtil.bcryptHash`). Os recursos utilizam `@DenyAll` na classe e abertura granular por `@RolesAllowed`, complementados por verificação de posse no `PedidoService` via `SecurityIdentity`. CORS está habilitado para `http://localhost:3000`.

### Usuários Fictícios Cadastrados na Inicialização (`StartupEvent`)
| Usuário | Senha | Papel | Permissões |
| :--- | :--- | :--- | :--- |
| `maria` | `maria123` | `cliente` | Consulta produtos; cria e consulta apenas os próprios pedidos. |
| `joao` | `joao123` | `cliente` | Consulta produtos; cria e consulta apenas os próprios pedidos. |
| `admin` | `admin123` | `admin` | Gerencia catálogo de produtos, visualiza/gerencia todos os pedidos e acessa `/q/metrics`. |

### Política de Endpoints de Observabilidade (`/q/*`)
- **`/q/health` (`/q/health/live` e `/q/health/ready`):** Aberto (`policy=permit`) para permitir sondas de orquestradores sem credenciais.
- **`/q/metrics`:** Restrito exclusivamente ao papel `admin` (`policy=somente-admin`), protegendo dados operacionais e métricas de negócio (`pedidos_consultados_total` e `produtos_consultados_total`).

---

## 9. Requisitos e Como Executar

### Requisitos
- **JDK 21** configurado no `JAVA_HOME` e no `PATH`.
- **Maven 3.9+** (ou Maven Wrapper `mvnw` / `mvnw.cmd`).
- **Sem Docker:** Execução 100% local e nativa.

### Subir os Serviços em Paralelo (Dois Terminais na Raiz)

**Terminal 1 — `servico-produto` (Porta 8081):**
```powershell
mvn quarkus:dev -pl servico-produto
```

**Terminal 2 — `servico-pedido` (Porta 8080):**
```powershell
mvn quarkus:dev -pl servico-pedido
```

---

## 10. Testes Automatizados e Perfis

Para executar a suíte completa de testes automatizados (`@QuarkusTest`) de ambos os módulos a partir da raiz:

```powershell
mvn clean verify
```

- **Contagem real de testes:** **41 testes passando, 0 falhas** (23 testes no `servico-pedido` + 18 testes no `servico-produto`), cobrindo CRUD REST Nível 2, consultas de repositório, fault tolerance (retry, timeout, circuit breaker), validação de entrada, formatos de erro, autenticação Basic/BCrypt e autorização por papel e posse.

### Perfis de Configuração (`application.properties`)
- **`%dev`**: Banco H2 em memória com `drop-and-create` e carga de dados de desenvolvimento (`import.sql`).
- **`%test`**: Banco H2 em memória limpo com `drop-and-create` e porta HTTP aleatória (`quarkus.http.test-port=0`) para evitar conflito durante a execução paralela.
- **`%prod`**: Configurado com `update`, sem recriação destrutiva de schema e sem carga de dados fictícios.