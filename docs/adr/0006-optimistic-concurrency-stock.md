# ADR-0006: Concorrência otimista no saldo de estoque; eventos in-process no MVP

Status: **Aceito** · Data: 2026-08-18

## Contexto

RF-VND-5/RNF-1 exigem que duas confirmações de venda concorrentes disputando o mesmo saldo nunca
resultem em overselling. Também precisamos decidir como os eventos de domínio (`StockMovementRecorded`,
`ProductLowStockReached`) são propagados sem sobre-engenhar o MVP.

## Decisão

1. **Concorrência**: `StockBalance` usa `@Version` (lock otimista do Hibernate). Ao confirmar
   `SalesOrder`/receber `PurchaseOrder`, conflito de versão gera HTTP 409 e o cliente decide
   re-tentar. Rejeitamos lock pessimista (`SELECT ... FOR UPDATE`) como padrão, pois criaria
   contenção desnecessária para o volume esperado; pode ser revisitado pontualmente se um teste
   de carga mostrar taxa de conflito alta.
2. **Eventos de domínio**: publicados e consumidos **in-process** via CDI (`Event<T>` síncrono ou
   `@ObservesAsync`), sem broker de mensageria no MVP. A interface do publisher já é desenhada
   como porta (`DomainEventPublisher`) para permitir trocar a implementação por
   Kafka/SmallRye Reactive Messaging depois sem tocar em regra de negócio.

## Alternativas consideradas

- **Lock pessimista em toda escrita de estoque**: mais simples de raciocinar, mas serializa
  operações desnecessariamente; rejeitado como padrão.
- **Fila/broker (Kafka) desde o MVP**: adiciona infraestrutura (broker, DLQ, serialização) sem
  necessidade real no volume do MVP — overengineering explícito a evitar. Fica como M7 (stretch)
  se o projeto quiser demonstrar arquitetura orientada a eventos completa.

## Consequências

- Frontend precisa tratar HTTP 409 em telas de confirmação de venda (re-buscar e permitir retry).
- Teste de integração dedicado simula duas requisições concorrentes contra o mesmo `StockBalance`
  (ver `03-architecture.md`, estratégia de testes, item 3).
- Migrar para mensageria assíncrona no futuro é uma troca de implementação atrás da porta
  `DomainEventPublisher`, não uma reescrita de `Inventory`/`Sales`/`Purchasing`.
