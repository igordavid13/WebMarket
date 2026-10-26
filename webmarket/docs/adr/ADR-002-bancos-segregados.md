# ADR-002: Bancos de Dados Segregados por Microsserviço

## Status
Aceito

## Contexto
Microsserviços devem possuir autonomia total sobre seus dados. O compartilhamento de tabelas ou banco de dados cria acoplamento oculto e impede a escalabilidade e a evolução independente dos esquemas.

## Decisão
Adotar o padrão *Database-per-Service* com instâncias lógicas isoladas de H2:
- `ms-pedido`: H2 em memória (`jdbc:h2:mem:pedidos`) rodando na porta 8082.
- `ms-produto`: H2 baseado em arquivo persistente (`jdbc:h2:file:./data/webmarket-produto`) rodando na porta 8083.

## Alternativas consideradas
- **Banco H2 único compartilhado:** Rejeitado por violar a segregação de dados e permitir junções (JOINs) indevidas entre tabelas de contextos distintos.
- **PostgreSQL / MySQL em contêineres:** Considerado viável, mas descartado para simplificar a execução local sem comprometer o isolamento físico exigido.

## Consequências
- **Ganho:** Impossibilidade de queries cruzadas, total independência de schema e isolamento de falhas.
- **Custo:** Perda de transações ACID globais entre pedido e estoque.
- **Mitigação:** Coordenação via mensageria assíncrona e padrão Transactional Outbox.