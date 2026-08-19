# 04 — Contrato de API (visão de alto nível)

Status: **Rascunho** · Última revisão: 2026-08-19

Este documento fixa o **formato e as regras de acesso** dos endpoints do MVP antes da
implementação (ADR-0008, contract-first). O YAML OpenAPI definitivo é gerado a partir das
anotações do código durante o M0/M1 e passa a viver em `docs/spec/openapi.yaml`
(commitado como artefato de referência, revisado em PR a cada mudança de contrato).

Prefixo base: `/api/v1`. Todas as respostas de erro em `application/problem+json` (RNF-9).
Listagens sempre paginadas: `?page=0&size=20` (defaults), resposta com `content`, `page`,
`size`, `totalElements`.

| Recurso | Método/rota | Papel mínimo | Observação |
|---|---|---|---|
| Produtos | `GET /products` | qualquer autenticado | paginado, filtros `q`, `categoryId` |
| Produtos | `GET /products/{id}` | qualquer autenticado | |
| Produtos | `POST /products` | `ADMIN` | |
| Produtos | `PUT /products/{id}` | `ADMIN` | |
| Produtos | `DELETE /products/{id}` | `ADMIN` | soft-delete (RF-CAT-3) |
| Categorias | `GET/POST/PUT/DELETE /categories` | leitura: qualquer; escrita: `ADMIN` | |
| Parceiros | `GET/POST/PUT /partners` | leitura: qualquer; escrita: `ADMIN` | filtro `role=SUPPLIER\|CUSTOMER` |
| Saldo de estoque | `GET /inventory/{productId}` | qualquer autenticado | retorna `onHand`, `reserved`, `available` |
| Movimentações | `GET /inventory/{productId}/movements` | qualquer autenticado | paginado, filtro por período/tipo |
| Ajuste manual | `POST /inventory/{productId}/adjustments` | `ADMIN`, `ESTOQUISTA` | exige `reason` |
| Pedido de compra | `POST /purchase-orders` | `ADMIN`, `ESTOQUISTA` | cria em `DRAFT` |
| Pedido de compra | `POST /purchase-orders/{id}/confirm` | `ADMIN`, `ESTOQUISTA` | |
| Pedido de compra | `POST /purchase-orders/{id}/receive` | `ADMIN`, `ESTOQUISTA` | gera `StockMovement` (idempotente) |
| Pedido de compra | `POST /purchase-orders/{id}/cancel` | `ADMIN`, `ESTOQUISTA` | |
| Pedido de compra | `GET /purchase-orders`, `GET /purchase-orders/{id}` | qualquer autenticado | |
| Pedido de venda | `POST /sales-orders` | `ADMIN`, `VENDEDOR` | cria em `DRAFT` |
| Pedido de venda | `POST /sales-orders/{id}/confirm` | `ADMIN`, `VENDEDOR` | reserva estoque; 409 se conflito/insuficiente |
| Pedido de venda | `POST /sales-orders/{id}/invoice` | `ADMIN`, `VENDEDOR` | baixa estoque |
| Pedido de venda | `POST /sales-orders/{id}/cancel` | `ADMIN`, `VENDEDOR` | |
| Pedido de venda | `GET /sales-orders`, `GET /sales-orders/{id}` | qualquer autenticado | |
| Alertas | `GET /alerts/low-stock` | qualquer autenticado | RF-ALR-1/2 |
| Relatórios | `GET /reports/low-stock`, `GET /reports/movements` | qualquer autenticado | RF-REL-1/2 |

Erros de negócio conhecidos (mapeados para HTTP + `type` no problem detail):

| Situação | HTTP | `type` |
|---|---|---|
| Estoque insuficiente ao confirmar venda | 422 | `urn:stockmaster:insufficient-stock` |
| Conflito de concorrência (versão de estoque) | 409 | `urn:stockmaster:stock-conflict` |
| Transição de status inválida (ex: receber pedido cancelado) | 409 | `urn:stockmaster:invalid-transition` |
| Documento (CPF/CNPJ) ou SKU duplicado | 409 | `urn:stockmaster:duplicate-key` |
| Validação de campo (Bean Validation) | 400 | `urn:stockmaster:validation-error` |

Este documento é atualizado sempre que um endpoint muda de forma incompatível; o `openapi.yaml`
gerado é a fonte definitiva de tipos, este markdown é a visão de leitura rápida + regras de acesso.
