# ADR-005: Redis como Cache-Aside com Invalidação Explícita no Catálogo

## Status
Aceito

## Contexto
O catálogo de produtos possui alta taxa de leitura e baixa taxa de escrita. Consultas frequentes no banco H2 sobrecarregam a persistência desnecessariamente, enquanto o estoque atualizado requer consistência nas visualizações subsequentes.

## Decisão
Implementar o padrão Cache-Aside no `ms-produto` utilizando Redis (`StringRedisTemplate`):
- **Chaves:** `produto:<id>` (detalhe) e `produtos:catalogo` (listagem).
- **TTL:** 10 minutos com serialização Jackson JSON.
- **Invalidação:** Explícita e pontual via `invalidarCache(produtoId)` após a conclusão com sucesso do débito de estoque.

## Alternativas consideradas
- **Cache local em memória (Caffeine/ConcurrentMap):** Rejeitado porque o cache ficava isolado na JVM, impossibilitava visualização externa e gerava inconsistência caso houvesse múltiplas instâncias do serviço.
- **Write-Through / Read-Through no Redis:** Rejeitado para manter o banco H2 como fonte única e definitiva da verdade.

## Consequências
- **Ganho:** Respostas sub-milissegundo para consultas de catálogo e isolamento da carga de leitura do banco relacional.
- **Custo:** Necessidade de gerenciar a invalidação correta das chaves para evitar dados obsoletos.
- **Mitigação:** Invocação imediata de `redisTemplate.delete(...)` sincronizada com a transação de baixa do estoque.