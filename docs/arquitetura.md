# Arquitetura da Solução - WebMarket

Este documento detalha o desenho arquitetural do **WebMarket**, descrevendo a separação de responsabilidades entre os microsserviços, a topologia de portas, os protocolos de comunicação, a resiliência, o modelo de segurança (RBAC e posse) e a observabilidade.

---

## 1. Visão Geral dos Serviços

O sistema é concebido como um monorepo multi-módulo composto por dois serviços independentes desenvolvidos em **Quarkus 3.20.3** com **Java 21**:

| Serviço | Porta | Responsabilidade Principal | Modelo de Persistência |
| :--- | :---: | :--- | :--- |
| **`servico-pedido`** | `8080` | Gerenciamento de pedidos de compra, validação de posse por cliente, cálculo de valores totais e consulta síncrona de estoque. | H2 em memória (`pedidodb`) via Panache Repository |
| **`servico-produto`** | `8081` | Catálogo de produtos, restrição de unicidade por nome e verificação de saldos disponíveis em estoque. | H2 em memória (`produtodb`) via Panache Repository |

---

## 2. Diagrama de Comunicação e Topologia

```text
               +-------------------------------------+
               |            Cliente HTTP             |
               |   (Authorization: Basic <base64>)   |
               +-------------------------------------+
                                  |
                                  | 1. POST /pedidos
                                  v
               +-------------------------------------+
               |           servico-pedido            |
               |            (Porta 8080)             |
               |  - Validação (@Valid) + Posse       |
               |  - ProdutoGateway (Fault Tolerance) |
               +-------------------------------------+
                                  |
                                  | 2. GET /produtos/{id}/disponibilidade?quantidade=X
                                  |    (REST Client síncrono + ProdutoAuthHeaderFactory)
                                  v
               +-------------------------------------+
               |           servico-produto           |
               |            (Porta 8081)             |
               +-------------------------------------+
                                  |
                   +--------------+--------------+
                   |                             |
                   v                             v
            [ H2 em Memória ]             [ H2 em Memória ]
              (pedidodb)                    (produtodb)
```

### Fluxo da Chamada:
1. O cliente HTTP envia uma requisição autenticada via **HTTP Basic** (`POST /pedidos`) ao `servico-pedido`.
2. Após validar o formato da requisição (`@Valid`) e verificar a regra de posse (`SecurityIdentity`), o `PedidoService` aciona o `ProdutoGateway`.
3. O `ProdutoGateway` invoca síncronamente o `servico-produto` via **MicroProfile REST Client** (`GET /produtos/{id}/disponibilidade`), propagando credenciais via `ProdutoAuthHeaderFactory`.
4. **Caminho de Sucesso (`200 OK` no catálogo):** O pedido tem seu valor total calculado, recebe o status `CONFIRMADO` e é persistido transacionalmente (`@Transactional`) no banco H2 do `servico-pedido`, retornando `201 Created` com cabeçalho `Location`.
5. **Erro de Negócio (`404` produto inexistente ou `409` estoque insuficiente):** O `@ClientExceptionMapper` converte o erro em `ProdutoIndisponivelNegocioException`, abortando retentativas (`abortOn` / `skipOn`) e devolvendo **HTTP `422 Unprocessable Entity`** padronizado sem gravar o pedido.
6. **Falha Técnica (Timeout de `500 ms`, recusa de conexão ou `5xx`):** As anotações `@Timeout`, `@Retry(maxRetries = 2)`, `@CircuitBreaker(requestVolumeThreshold = 4)` e `@Fallback` atuam em conjunto, devolvendo **HTTP `503 Service Unavailable`** sem persistir o pedido.

---

## 3. Justificativa da Transição de Arquitetura (Porte do Módulo Anterior)

No módulo anterior (BE-JV-010 em Spring Boot), a comunicação operava de forma assíncrona orientada a eventos através de brokers (RabbitMQ para comando e Kafka para evento de conclusão), exigindo Docker Compose e Outbox Pattern.

Para o módulo atual (PE-JV-001 em Quarkus):
* **Comunicação Síncrona Direta:** A mensageria assíncrona foi substituída por **MicroProfile REST Client**, exercitando injeção CDI (`@RestClient`), serialização Jackson e tratamento semântico HTTP (Nível 2 de Richardson).
* **Resiliência na Aplicação:** A confiabilidade da comunicação entre serviços é garantida pelas anotações declarativas do SmallRye Fault Tolerance (`@Timeout`, `@Retry`, `@CircuitBreaker`, `@Fallback`), externalizadas no `application.properties`.
* **Autossuficiência Local:** Cada serviço gerencia sua própria base de dados H2 em memória isolada, permitindo compilação e suíte completa de 34 testes (`mvn clean verify`) sem dependência de containers Docker.

---

## 4. Segurança: Autenticação, RBAC, Posse e CORS

A segurança é estruturada em múltiplas camadas utilizando **Quarkus Security JPA** e **JSR-250**:

* **Autenticação (`quarkus-security-jpa`):** Credenciais armazenadas na tabela `Usuario` com senhas protegidas por hash **BCrypt** (`BcryptUtil.bcryptHash`), carregadas na inicialização via `CargaDeUsuarios` (`StartupEvent`).
* **Autorização por Papel (RBAC):** Todos os recursos REST adotam `@DenyAll` no nível da classe e abertura explícita por método com `@RolesAllowed`.
* **Autorização por Posse (`SecurityIdentity`):** No `PedidoService`, usuários com papel `cliente` são restritos a listar, consultar e criar pedidos vinculados exclusivamente ao seu próprio `username` (retornando `403 Forbidden` em tentativas sobre dados de terceiros).
* **CORS e CSRF:** CORS habilitado para a origem `http://localhost:3000`. A extensão `quarkus-rest-csrf` não é utilizada pois a API é *stateless* JSON autenticada via cabeçalho `Authorization: Basic`, sem cookies de sessão ou formulários HTML.

### Matriz de Papéis e Permissões

| Papel (`Role`) | Usuários de Teste | Escopo de Acesso e Endpoints Permitidos |
| :--- | :--- | :--- |
| **`cliente`** | `maria`, `joao` | **Catálogo (`servico-produto`):** Leitura (`GET /produtos`, `GET /produtos/{id}`, `GET /produtos/{id}/disponibilidade`).<br>**Pedidos (`servico-pedido`):** `POST /pedidos` (apenas em próprio nome), `GET /pedidos` e `GET /pedidos/{id}` (apenas os próprios pedidos). |
| **`admin`** | `admin` | **Acesso irrestrito:** Todas as operações de leitura e escrita em catálogo (`POST`, `PUT`, `DELETE /produtos`), gestão global de pedidos (`GET`, `PATCH /pedidos/{id}/status`, `DELETE /pedidos/{id}`) e leitura de métricas (`/q/metrics`). |

---

## 5. Observabilidade e Telemetria

Ambos os microsserviços expõem endpoints padronizados do SmallRye Health e Micrometer Prometheus, protegidos por regras de caminho no `application.properties`:

* **Health Checks (`/q/health` — Política `permit`):**
  * `/q/health/live`: Verifica a vivacidade da JVM e os checks customizados (`PedidoHealthCheck` e `ProdutoHealthCheck`).
  * `/q/health/ready`: Valida a prontidão do serviço e do pool de conexões H2, mantido aberto para sondas de orquestradores.
* **Métricas Prometheus (`/q/metrics` — Política `somente-admin`):**
  * Restrito ao papel `admin`. Exporta telemetria da JVM/HTTP e os contadores de negócio anotados com `@Counted` (`pedidos_consultados_total` e `produtos_consultados_total`).

---

## 6. Padrão de Camadas Internas, CDI e Tratamento de Erros

Cada microsserviço segue uma arquitetura em três camadas estritas com injeção via CDI (`@ApplicationScoped` e `@Inject`) e mapeamento centralizado de exceções:

```text
[ Requisição HTTP ]
        │
        ▼
[ Resource / Controller ] (@ApplicationScoped, @DenyAll, @RolesAllowed)
  - Validação de fronteira (@Valid) + ValidationMessages.properties (pt-BR)
  - Serialização e status REST Nível 2
  - Métricas (@Counted)
        │
        ▼ @Inject
[ Service de Domínio ] (@ApplicationScoped)
  - Regras de negócio e validação de posse (SecurityIdentity)
  - Controle transacional (@Transactional)
  - Orquestração de chamadas remotas (ProdutoGateway)
        │
        ▼ @Inject
[ Repository Panache ] (@ApplicationScoped, PanacheRepository<T>)
  - Consultas customizadas e operações CRUD
        │
        ▼
[ Banco H2 em Memória ]

[ ExceptionMappers (@ServerExceptionMapper) ]
  └── Intercepta ConstraintViolationException, ForbiddenException e RuntimeExceptions
      de domínio, devolvendo sempre o contrato único ErroResposta (status, erro, mensagens).
```