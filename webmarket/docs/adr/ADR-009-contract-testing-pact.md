# ADR-009: Testes de Contrato Automatizados com Pact JVM

## Status
Aceito

## Contexto
A integração HTTP síncrona entre `ms-pedido` (consumidor) e `ms-produto` (provedor) para consulta de dados do produto pode ser quebrada se o provedor alterar nomes de campos ou tipos de retorno. Testes end-to-end integrados são lentos e instáveis.

## Decisão
Adotar *Consumer-Driven Contract Testing* com Pact JVM (especificação V4):
- O consumidor `ms-pedido` define suas expectativas em `ProdutoConsumerContractTest.java` e gera o arquivo `ms-pedido-ms-produto.json`.
- O provedor `ms-produto` valida as interações contra o Spring Boot real em `ProdutoProviderContractTest.java`, isolando recursos externos com `@MockitoBean`.

## Alternativas consideradas
- **Spring Cloud Contract:** Rejeitado por exigir configuração mais verbosa e ter menor interoperabilidade fora do ecossistema Spring.
- **Testes manuais ou testes E2E com ambos os serviços de pé:** Rejeitado pela fragilidade, lentidão e dificuldade de automação em pipelines de CI/CD.

## Consequências
- **Ganho:** Verificação estática e determinística de contratos em milissegundos, garantindo que o provedor não introduza *breaking changes*.
- **Custo:** Manutenção dos testes de contrato e necessidade de sincronização do arquivo pact gerado.
- **Mitigação:** Compartilhamento direto do arquivo JSON via diretório de build Maven (`target/pacts`).