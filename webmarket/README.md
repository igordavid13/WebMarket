# WebMarket - Quarkus

Monorepo Maven com **Java 21** e **Quarkus 3.20.3** (compatível com a especificação 3.15.7+ do módulo PE-JV-001). A raiz agrega os módulos; cada microsserviço roda de forma independente, com banco de dados H2 em memória, observabilidade e comunicação síncrona via REST Client.

---

## 1. Tema e Problema

O **WebMarket** é uma aplicação simplificada de pedidos e estoque dividida em dois microsserviços:
- **`servico-pedido`**: Gerencia pedidos de compra e seus respectivos itens.
- **`servico-produto`**: Controla o catálogo de produtos e os saldos disponíveis em estoque.

> **Porte do BE-JV-010 para Quarkus:** No módulo anterior, a comunicação era assíncrona por filas e eventos (RabbitMQ/Kafka). Para este módulo, as filas, tópicos e conectores assíncronos foram descartados em conformidade com o edital. A arquitetura foi migrada para chamadas síncronas via **REST Client**, permitindo executar o projeto localmente com H2 em memória, sem necessidade de Docker.

---

## 2. Estrutura do Repositório (Multi-Módulo)

```text
webmarket/
├── pom.xml                 # POM agregador raiz (multi-módulo)
├── README.md               # Instruções de configuração e execução
├── AVALIACAO.md            # Autoavaliação e mapa de evidências por critério
├── docs/
│   └── arquitetura.md      # Desenho dos serviços, portas e fluxo síncrono
├── servico-pedido/         # Microsserviço de pedidos (porta 8080)
└── servico-produto/        # Microsserviço de catálogo e estoque (porta 8081)
```

---

## 3. Arquitetura e Comunicação

```text
[ Cliente HTTP ]
       │
       ▼ (POST /pedidos)
[ servico-pedido ] (Porta 8080)
       │
       │ Consulta síncrona (GET /produtos/{id}/disponibilidade)
       ▼
[ servico-produto ] (Porta 8081)
       │
       ▼
  [ H2 em Memória ]
```

- **`servico-pedido` (8080):** Expõe recursos para criar, listar com filtros e consultar pedidos.
- **`servico-produto` (8081):** Expõe recursos para cadastro de catálogo, verificação de saldos e controle de estoque.
- **Banco de Dados:** Cada microsserviço possui sua própria instância isolada de H2 Database em memória (`jdbc:h2:mem:...`), inicializada com dados de teste em dev/test via `import.sql`.

---

## 4. Requisitos

- **Java Development Kit (JDK) 21** configurado no `JAVA_HOME` e no `PATH`.
- **Maven 3.9+** (ou o Maven Wrapper `mvnw` / `mvnw.cmd` incluído na raiz).
- **Sem Docker:** A aplicação executa localmente de forma nativa.

---

## 5. Como Executar os Serviços

Abra dois terminais a partir da pasta raiz do projeto:

### Terminal 1: Iniciar `servico-produto` (Porta 8081)
```powershell
.\mvnw.cmd quarkus:dev -pl servico-produto
```
*(No Linux/macOS: `./mvnw quarkus:dev -pl servico-produto`)*

### Terminal 2: Iniciar `servico-pedido` (Porta 8080)
```powershell
.\mvnw.cmd quarkus:dev -pl servico-pedido
```
*(No Linux/macOS: `./mvnw quarkus:dev -pl servico-pedido`)*

---

## 6. Endpoints Principais e Observabilidade

### `servico-produto` (Porta 8081)
- **Listar Produtos:** `GET http://localhost:8081/produtos`
- **Filtrar com Estoque:** `GET http://localhost:8081/produtos?somenteDisponiveis=true`
- **Consultar por ID:** `GET http://localhost:8081/produtos/1`
- **Verificar Disponibilidade:** `GET http://localhost:8081/produtos/1/disponibilidade?quantidade=2`
- **Health Checks:** `GET http://localhost:8081/q/health`
- **Métricas Prometheus:** `GET http://localhost:8081/q/metrics`

### `servico-pedido` (Porta 8080)
- **Listar Pedidos:** `GET http://localhost:8080/pedidos`
- **Filtrar por Status:** `GET http://localhost:8080/pedidos?status=CONFIRMADO`
- **Criar Pedido:** `POST http://localhost:8080/pedidos`
- **Health Checks:** `GET http://localhost:8080/q/health`
- **Métricas Prometheus:** `GET http://localhost:8080/q/metrics`

---

## 7. Como Executar os Testes Automatizados

Para rodar todos os testes `@QuarkusTest` dos dois módulos a partir da raiz:

```powershell
.\mvnw.cmd clean verify
```
*(No Linux/macOS: `./mvnw clean verify`)*

A suíte executará os testes de ambos os módulos com H2 em memória, garantindo build verde sem dependências externas.

---

## 8. Perfis de Configuração

Os serviços adotam configurações segregadas em `application.properties`:
- **`%dev`**: Cria o banco em memória com `drop-and-create` e carrega dados de teste via `import.sql`.
- **`%test`**: Cria schema limpo com `drop-and-create` e carrega dados controlados para os testes automatizados.
- **`%prod`**: Configurado com `update`, evitando recriações destrutivas e sem carregar dados fictícios de teste.