# Arquitetura da Solução - WebMarket

Este documento detalha o desenho arquitetural do **WebMarket**, descrevendo a separação de responsabilidades entre os microsserviços, a topologia de portas, os protocolos de comunicação e os papéis de acesso (RBAC) previstos.

---

## 1. Visão Geral dos Serviços

O sistema é concebido como um monorepo multi-módulo composto por dois serviços independentes desenvolvidos em **Quarkus**:

| Serviço | Porta | Responsabilidade Principal | Modelo de Persistência |
| :--- | :---: | :--- | :--- |
| **`servico-pedido`** | `8080` | Gerenciamento de pedidos de compra, cálculo de valores totais e controle do ciclo de vida do pedido. | H2 em memória (`pedidodb`) via Panache |
| **`servico-produto`**| `8081` | Catálogo de itens e controle transacional de saldo em estoque. | H2 em memória (`produtodb`) via Panache |

---

## 2. Diagrama de Comunicação e Topologia

```text
               +-----------------------------+
               |        Cliente HTTP         |
               +-----------------------------+
                              |
                              | 1. POST /pedidos
                              v
               +-----------------------------+
               |       servico-pedido        |
               |        (Porta 8080)         |
               +-----------------------------+
                              |
                              | 2. GET /produtos/{id}/disponibilidade?quantidade=X
                              |    (MicroProfile REST Client síncrono)
                              v
               +-----------------------------+
               |       servico-produto       |
               |        (Porta 8081)         |
               +-----------------------------+
                              |
               +--------------+--------------+
               |                             |
               v                             v
        [ H2 em Memória ]             [ H2 em Memória ]
          (pedidodb)                    (produtodb)
```

### Fluxo da Chamada:
1. O cliente HTTP submete a intenção de compra via `POST /pedidos` no `servico-pedido`.
2. O `servico-pedido` invoca síncronamente o `servico-produto` via REST Client (`GET /produtos/{id}/disponibilidade`) para validar se há quantidade suficiente de cada item em estoque.
3. Se houver estoque suficiente, o pedido é registrado como `CONFIRMADO` e o `servico-produto` debita as unidades correspondentes.
4. Se o `servico-produto` responder indisponibilidade ou falha na comunicação, as políticas de **Fault Tolerance** (Timeout, Fallback e Circuit Breaker) no `servico-pedido` tratam a exceção, mantendo o pedido em estado pendente ou cancelado sem causar erro 500 não tratado.

---

## 3. Justificativa da Transição de Arquitetura (Porte do Módulo Anterior)

No módulo anterior (BE-JV-010), a comunicação operava de forma assíncrona orientada a eventos através de brokers (RabbitMQ para comando e Kafka para evento de conclusão), exigindo Docker Compose e outbox pattern.

Para o módulo atual (PE-JV-001):
* **Comunicação Síncrona Direta:** A mensageria assíncrona foi substituída por **MicroProfile REST Client**, exercitando injeção CDI, serialização Jackson e tratamento semântico HTTP (Nível 2 de Richardson).
* **Resiliência na Aplicação:** A confiabilidade da rede passa a ser garantida pelas anotações de tolerância a falhas do Quarkus (`@Timeout`, `@Fallback`, `@CircuitBreaker`), reduzindo o acoplamento de infraestrutura.
* **Autossuficiência Local:** Cada serviço gerencia sua própria base de dados em memória isolada (H2), permitindo compilação e suíte completa de testes (`mvn verify`) sem dependência de containers Docker.

---

## 4. Matriz de Segurança e Papéis de Acesso (RBAC)

Para atender aos requisitos de autorização (JSR-250) com segregação por regras de acesso, foram definidos dois perfis de usuários:

| Papel (Role) | Escopo de Acesso | Endpoints Permitidos |
| :--- | :--- | :--- |
| **`CLIENTE`** | Operações de consulta pública e criação de pedidos próprios. | `GET /produtos` (leitura)<br>`POST /pedidos` (criar pedido)<br>`GET /pedidos/{id}` (apenas pedidos próprios) |
| **`GERENTE`** | Administração de catálogo, controle de estoque e auditoria de vendas. | Acesso irrestrito a todos os endpoints:<br>`POST /produtos` (cadastrar item)<br>`PUT /produtos/{id}` (alterar estoque/preço)<br>`DELETE /produtos/{id}`<br>`PATCH /pedidos/{id}/status` |

---

## 5. Observabilidade e Telemetria

Ambos os microsserviços expõem endpoints padronizados de monitoramento nativo:
* **Health Checks (`/q/health`):**
  * `/q/health/live`: Verifica a vivacidade do processo Java/Quarkus e componentes customizados (`PedidoHealthCheck` e `ProdutoHealthCheck`).
  * `/q/health/ready`: Valida a conectividade ativa do pool de conexões do datasource H2.
* **Métricas Prometheus (`/q/metrics`):**
  * Contabilizadores customizados com anotação `@Counted` para medir a volumetria de operações de negócio (`pedidos_consultados_total` e `produtos_consultados_total`), além das métricas padrão de runtime da JVM e requisições HTTP.