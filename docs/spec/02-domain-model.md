# 02 — Modelo de Domínio

Status: **Aceito** · Última revisão: 2026-08-19

Nomes de domínio em inglês no código (classes, campos), documentação em português. Isso evita
mistura de idiomas dentro do código-fonte.

## Bounded contexts

| Contexto | Responsabilidade | Módulo/pacote sugerido |
|---|---|---|
| Catalog | Produtos e categorias | `catalog` |
| Partners | Fornecedores e clientes | `partners` |
| Inventory | Saldo e movimentações de estoque | `inventory` |
| Purchasing | Pedidos de compra | `purchasing` |
| Sales | Pedidos de venda | `sales` |
| Notifications | Alertas (estoque baixo) | `notifications` |
| Identity | Delegado ao Keycloak (fora do código da app) | — |

`Inventory` é o contexto central — `Purchasing` e `Sales` dependem dele, mas ele não depende de
nenhum dos dois (evita acoplamento circular). Comunicação `Purchasing`/`Sales` → `Inventory` é
feita via uma porta de aplicação (interface, `InventoryPort`), nunca acesso direto a repositório
de outro módulo.

> **Nota de futuro (não-MVP)**: essa mesma `InventoryPort` é o ponto onde um futuro adaptador de
> IoT (leitor de código de barras, câmera) entraria como mais um chamador de `receive`/`adjust`,
> igual a `Purchasing` hoje — não exige mudar `Inventory`. Mencionado aqui só para justificar por
> que a porta é modelada como interface desde o MVP; nenhum código de IoT existe ainda (ver
> `00-vision.md`, "Visão de futuro").

## Agregados e invariantes

### `Product` (Catalog) — aggregate root

- Campos: `id`, `sku` (único), `name`, `categoryId`, `unitOfMeasure`, `costPrice` (BigDecimal),
  `salePrice` (BigDecimal), `minimumStock` (int), `active` (bool).
- Invariante: `costPrice >= 0`, `salePrice >= 0`, `minimumStock >= 0`.
- Não pode ser removido fisicamente se possuir `StockMovement` associado — apenas `active = false`.

### `Partner` (Partners) — aggregate root

- Campos: `id`, `name`, `document` (CPF/CNPJ, único), `roles` (`SUPPLIER`, `CUSTOMER` — conjunto,
  não exclusivo), `contactInfo`.
- Invariante: `document` válido (dígito verificador) e único.

### `StockBalance` (Inventory) — aggregate root

- Campos: `productId` (chave), `quantityOnHand` (int), `quantityReserved` (int), `version` (long,
  controle de concorrência otimista).
- Derivado: `quantityAvailable = quantityOnHand - quantityReserved`.
- Invariante: `quantityOnHand >= 0`, `quantityReserved >= 0`, `quantityReserved <= quantityOnHand`.
- Toda mutação passa por um método de domínio (`receive`, `reserve`, `release`, `ship`, `adjust`)
  que valida a invariante antes de aplicar — nunca setter público de quantidade.

### `StockMovement` (Inventory) — entidade imutável, parte do histórico

- Campos: `id`, `productId`, `type` (`PURCHASE_IN`, `SALE_OUT`, `ADJUSTMENT_POSITIVE`,
  `ADJUSTMENT_NEGATIVE`), `quantity`, `resultingBalance`, `sourceDocumentType` (`PURCHASE_ORDER` |
  `SALES_ORDER` | `MANUAL`), `sourceDocumentId` (nullable para ajuste manual), `reason`
  (obrigatório se `MANUAL`), `performedBy` (subject do usuário Keycloak), `occurredAt`.
- Nunca é alterado ou removido após criado (append-only).

### `PurchaseOrder` (Purchasing) — aggregate root

- Campos: `id`, `supplierId`, `status` (`DRAFT`, `CONFIRMED`, `RECEIVED`, `CANCELLED`), `lines`
  (`productId`, `quantity`, `unitCost`), `createdAt`, timestamps de transição.
- Transições válidas: `DRAFT → CONFIRMED → RECEIVED`, `DRAFT|CONFIRMED → CANCELLED`. Qualquer
  outra transição é rejeitada.
- Ao `RECEIVED`: para cada linha, chama `Inventory.receive(productId, quantity, sourceDoc)` —
  gera `StockMovement` tipo `PURCHASE_IN`.

### `SalesOrder` (Sales) — aggregate root

- Campos: `id`, `customerId`, `status` (`DRAFT`, `CONFIRMED`, `INVOICED`, `CANCELLED`), `lines`
  (`productId`, `quantity`, `unitPrice`), timestamps de transição.
- Transições válidas: `DRAFT → CONFIRMED → INVOICED`, `DRAFT|CONFIRMED → CANCELLED`.
- Ao `CONFIRMED`: para cada linha, chama `Inventory.reserve(productId, quantity)` — falha o
  pedido inteiro (nenhuma reserva parcial) se qualquer linha não tiver disponível suficiente.
- Ao `INVOICED`: chama `Inventory.ship(productId, quantity)` — converte reserva em
  `StockMovement` tipo `SALE_OUT`.
- Ao `CANCELLED` a partir de `CONFIRMED`: chama `Inventory.release(productId, quantity)` para
  cada linha.

## Eventos de domínio (in-process no MVP; ver ADR-0006 sobre mensageria futura)

- `StockMovementRecorded` — publicado após qualquer movimentação; consumido por
  `Notifications` para checar limiar de estoque mínimo.
- `ProductLowStockReached` — publicado por `Notifications` quando `quantityOnHand <=
  minimumStock` (com de-duplicação: não repete alerta se já ativo).
- `PurchaseOrderReceived` / `SalesOrderInvoiced` — úteis para futura auditoria/relatórios.

No MVP estes eventos são consumidos de forma síncrona in-process (CDI `Event<T>`), documentando o
ponto de extensão para trocar por mensageria assíncrona sem reescrever regra de negócio
(ver ADR-0006).

## Regras de negócio explícitas (para teste)

1. Confirmar `SalesOrder` com qualquer linha sem estoque disponível → toda a operação falha
   (transação atômica), erro de negócio `INSUFFICIENT_STOCK` com detalhe de qual produto.
2. Duas confirmações concorrentes de `SalesOrder` disputando o último item: exatamente uma
   sucede; a outra recebe conflito e deve poder ser re-tentada com estado atualizado.
3. Cancelar `SalesOrder` já `INVOICED` é inválido (estoque já baixou, não pode "desfazer" sem um
   fluxo de devolução — fora do MVP, listado como débito técnico).
4. Receber `PurchaseOrder` gera exatamente uma `StockMovement` por linha, nunca duas (idempotência
   se o endpoint for chamado 2x — ver `03-architecture.md`, seção de idempotência).
