# WebMarket - Quarkus

Monorepo Maven com Java 21 e Quarkus 3.20.3. A raiz agrega os modulos;
cada microsservico tem seu processo e seu executavel. Nao execute Quarkus na raiz.

```text
webmarket/
  pom.xml
  shared-contracts/    DTOs, comandos e eventos; biblioteca Java, nao um servico
  pedido-api/          API publica, porta 8081
  pedido-gravador/     Persistencia e API interna, porta 8082
  compose.yaml        RabbitMQ e Kafka locais
  infra/rabbitmq/     Exchanges, filas e DLQ
```

## Fluxo

```text
POST /pedidos -> pedido-api -> RabbitMQ (gravacao.ex / gravacao.q)
                                      -> pedido-gravador -> H2 + outbox
                                                           -> Kafka (pedido-gravado)
                                                           -> pedido-api (log de confirmacao)
GET /pedidos/{id} -> pedido-api -> REST -> pedido-gravador -> H2
```

O segundo servico nao tinha implementacao funcional no projeto original; foi
criado como `pedido-api`, usando os contratos de pedidos existentes. Os exemplos
inconsistentes de Pix/Comprovante foram substituidos pelo dominio de pedidos.
Cliente e produto sao identificadores: nao ha cadastro, consulta a catalogo ou
calculo de precos, pois esses dados nao existem nos contratos atuais.

## Requisitos

- JDK 21; `JAVA_HOME` deve apontar para ele e o PATH deve conter seu `bin`.
- Maven 3.9.11, ou o Maven Wrapper incluido.
- Docker com Compose v2 e suporte a containers Linux para executar os brokers.
- Acesso aos repositorios Maven e de imagens. No ambiente corporativo, configure
  o proxy de acordo com sua rede. Nao coloque credenciais no repositorio.

Confira `java -version` e `mvn -version`: ambos devem indicar Java 21.
No Windows desta maquina, o JDK 21 esta em `C:\desenvolvimento\FERRAMENTAS\JDK\java-21`.

## Build E Testes

Na raiz, usando PowerShell:

```powershell
mvn clean install
```

Ou, quando o Wrapper conseguir baixar sua distribuicao:

```powershell
.\mvnw.cmd clean install
```

O `install` disponibiliza o parent e os contratos no repositorio local para
iniciar cada servico separadamente. Os testes usam HTTP real com QuarkusTest,
H2 em memoria e conectores de mensageria em memoria; nao precisam de Docker.
Os mocks do cliente REST nao verificam uma conexao real entre os processos.

Se o VS Code ainda mostrar `Missing artifact` ou `Project configuration is not
up-to-date` depois do `install`, execute `Java: Update Project` pela paleta de
comandos e aguarde a reimportacao dos modulos. Se persistir, use
`Java: Clean Java Language Server Workspace` e permita a reimportacao.

## Infraestrutura Local

```powershell
docker compose up -d rabbitmq kafka
docker compose run --rm kafka-init
docker compose ps
```

O segundo comando aguarda o Kafka saudavel e cria `pedido-gravado` com 3 particoes
e fator de replicacao 1. Aguarde o RabbitMQ aparecer saudavel antes de iniciar os servicos.
As definicoes importadas pelo RabbitMQ criam fila e bindings mesmo antes do gravador subir.
Se usar brokers externos, provisione o mesmo topico e topologia antes de liberar trafego.

RabbitMQ: `localhost:5672`. Painel: <http://localhost:15672>, usuario e senha
`webmarket`. Essas credenciais sao publicas e exclusivas do ambiente local.
Kafka: `localhost:9092`. O Compose publica as portas somente em loopback.

## Desenvolvimento

Depois do `install`, abra dois terminais na raiz:

```powershell
# Terminal 1: gravador
mvn -f pedido-gravador/pom.xml quarkus:dev "-Ddebug=5006"
```

```powershell
# Terminal 2: API
mvn -f pedido-api/pom.xml quarkus:dev "-Ddebug=5005"
```

Pode substituir `mvn` por `.\mvnw.cmd`. Encerre cada processo com Ctrl+C.

- API: <http://localhost:8081/q/health>
- Gravador: <http://localhost:8082/q/health>
- Dev UI: `/q/dev-ui` na porta de cada servico, apenas no modo dev.

As classes `SpringBootApplication` nao sao necessarias: o plugin Quarkus gera o bootstrap.

## Experimentar A API

```powershell
$body = @{
    cliente = 1
    itens = @(@{ produto = 10; quantidade = 2 })
    dataPedido = "2026-10-30T12:00:00"
} | ConvertTo-Json -Depth 4
$pedido = Invoke-RestMethod -Method Post -Uri "http://localhost:8081/pedidos" -ContentType "application/json" -Body $body
$pedido
Invoke-RestMethod -Uri "http://localhost:8081/pedidos/$($pedido.identificadorPedido)"
```

O POST retorna `202 Accepted`, `identificadorPedido`, `situacao=RECEBIDO` e
`Location` somente depois da confirmacao de publicacao do RabbitMQ. Isso nao
significa que o pedido ja foi persistido. Se o GET inicial retornar `404`, repita
a consulta depois que o gravador consumir a fila. Falhas de comunicacao retornam `503`.
Um timeout de publicacao nao prova que a mensagem nao chegou; nao repita POSTs
automaticamente esperando idempotencia da API de entrada.

Cliente, produto e quantidade devem ser positivos; itens nao podem estar vazios
e dataPedido e obrigatoria. JSON invalido ou violacoes de validacao retornam `400`.

O gravador preserva as rotas `POST /interno/pedidos` e `GET /interno/pedidos/{id}`.
Seu POST recebe `{"identificadorPedido":"UUID","dados":{...}}` e retorna `200`.
Reentrega com o mesmo identificador nao sobrescreve o pedido nem cria outro evento.

## Persistencia E Entrega

O H2 do gravador usa arquivo em `./data/pedidos`, relativo ao diretorio de
trabalho do processo. O diretorio e ignorado pelo Git; nao ha banco no pedido-api.
O schema e atualizado automaticamente para desenvolvimento. Para producao,
use banco externo, migrations versionadas, autenticacao e gerenciamento de segredos.

Pedido, itens, situacao inicial CRIADO e evento pendente sao gravados na mesma
transacao. A cada segundo, o publisher envia eventos pendentes ao Kafka e remove
somente os confirmados. Uma falha deixa os pendentes para a proxima execucao.
Pode haver duplicatas se o processo cair depois do envio e antes do commit:
consumidores devem deduplicar por identificadorPedido (entrega pelo menos uma vez).

O listener RabbitMQ faz no maximo 3 tentativas de processamento por entrega.
Apos falha, rejeita a mensagem para `gravacao.dlq`, via `gravacao.dlx`.
A DLQ nao tem consumidor automatico: inspecione e reprocesse pelo painel apos
corrigir a causa. Comandos sao persistentes e filas/exchanges sao duraveis.

## Configuracao Externa

| Variavel | Padrao | Onde |
| --- | --- | --- |
| `RABBIT_HOST` / `RABBIT_PORT` | `localhost` / `5672` | Ambos |
| `RABBIT_USER` / `RABBIT_PASS` | `webmarket` / `webmarket` | Ambos |
| `KAFKA` | `localhost:9092` | Ambos |
| `GRAVADOR_URL` | `http://localhost:8082` | API |
| `PEDIDOS_JDBC_URL` | H2 arquivo `./data/pedidos` | Gravador |

## Executaveis JVM

O build gera um `target/quarkus-app` por servico. Em dois terminais, na raiz:

```powershell
java -jar pedido-gravador/target/quarkus-app/quarkus-run.jar
```

```powershell
java -jar pedido-api/target/quarkus-app/quarkus-run.jar
```

Distribua a pasta `quarkus-app` inteira, nao apenas o JAR. Os brokers precisam
continuar ativos. Para parar a infraestrutura sem apagar os volumes:

```powershell
docker compose down
```

## Limites Da Validacao Local

`mvn clean install` concluiu com sucesso: 8 testes, nenhuma falha ou erro,
e os dois diretorios `target/quarkus-app` gerados. O JSON do RabbitMQ e a
sintaxe YAML do Compose tambem foram validados, sem iniciar containers.

Os testes cobrem contratos, REST, validacao, persistencia, idempotencia e os
limites de mensageria em memoria. Docker nao esta instalado na maquina usada
na migracao: Compose, confirmacoes reais do RabbitMQ, DLQ e fluxo Kafka entre
os dois processos precisam ser exercitados em um ambiente com Docker/brokers.
O Wrapper encontrou HTTP 407 no proxy; o build usa o Maven instalado.