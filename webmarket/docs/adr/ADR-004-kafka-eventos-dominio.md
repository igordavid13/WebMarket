# ADR-004: Uso de Apache Kafka para Disseminação de Eventos de Domínio

## Status
Aceito

## Contexto
Após a execução do débito de estoque (ou sua falha), o resultado constitui um fato consumado no domínio (`EstoqueBaixadoEvent` ou `BaixaEstoqueFalhouEvent`). Esse evento pode interessar a múltiplos consumidores atuais e futuros (notificações, expedição, analíticos).

## Decisão
Adotar o Apache Kafka como barramento de eventos de domínio no tópico `estoque-eventos`, particionado por `pedidoId`.

## Alternativas consideradas
- **RabbitMQ Fanout / Topic Exchange:** Rejeitado para eventos de domínio devido à ausência de retenção temporal e incapacidade nativa de replay histórico de eventos.
- **Pollying via REST:** Rejeitado por ineficiência de rede e acoplamento desnecessário entre serviços.

## Consequências
- **Ganho:** Suporte a múltiplos assinantes desacoplados, ordenação garantida por partição (`pedidoId`) e persistência durável de histórico.
- **Custo:** Sobrecarga operacional de manter o broker Kafka.
- **Mitigação:** Uso de contêiner leve KRaft (`apache/kafka:latest`) sem dependência de ZooKeeper.