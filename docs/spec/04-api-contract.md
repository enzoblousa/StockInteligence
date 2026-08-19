# 04 — Contrato de API

Status: **Aceito** · Última revisão: 2026-08-19

Base path: `/api`. Sem autenticação no MVP (ver ADR-0004) — todo recurso de negócio é escopado
por `tenantId` explícito na URL. Erros seguem RFC 7807 (`application/problem+json`, ver
`03-architecture.md`). Fonte de verdade machine-readable é o OpenAPI publicado pelo backend em
`/q/openapi` (ver ADR-0008) — esta tabela é o contrato de referência legível por humano.

## Lojas (`Tenant`)

| Método | Rota | Body | Sucesso | Erros |
|---|---|---|---|---|
| POST | `/api/tenants` | `{ nome }` | `201` `{ id, nome, criadoEm }` | `400` |
| GET | `/api/tenants/{tenantId}` | — | `200` `{ id, nome, criadoEm }` | `404` |

## Produtos

| Método | Rota | Body | Sucesso | Erros |
|---|---|---|---|---|
| POST | `/api/tenants/{tenantId}/produtos` | `{ sku, nome, unidadeMedida, estoqueMinimo, custoUnitario?, precoVenda? }` | `201` produto criado (`saldoAtual = 0`) | `400`, `404` (tenant), `409` (sku duplicado) |
| GET | `/api/tenants/{tenantId}/produtos?lowStock=true&ativo=true` | — | `200` lista de produtos | `404` (tenant) |
| GET | `/api/tenants/{tenantId}/produtos/{produtoId}` | — | `200` produto | `404` |
| PATCH | `/api/tenants/{tenantId}/produtos/{produtoId}` | `{ nome?, estoqueMinimo?, custoUnitario?, precoVenda?, ativo? }` | `200` produto atualizado | `400`, `404` |

Objeto `Produto` (resposta): `{ id, tenantId, sku, nome, unidadeMedida, custoUnitario, precoVenda,
estoqueMinimo, saldoAtual, ativo, version }`. `PATCH` nunca aceita `saldoAtual` no body — é
ignorado se enviado (saldo só muda via movimentação).

## Movimentações de estoque

| Método | Rota | Body | Sucesso | Erros |
|---|---|---|---|---|
| POST | `/api/tenants/{tenantId}/produtos/{produtoId}/movimentos` | `{ tipo: "ENTRADA"\|"SAIDA"\|"AJUSTE", quantidade, motivo? }` | `201` `{ id, tipo, quantidade, motivo, saldoResultante, criadoEm }` | `400`, `404` (produto), `422` (saldo insuficiente), `409` (conflito de concorrência) |
| GET | `/api/tenants/{tenantId}/produtos/{produtoId}/movimentos?page=0&size=20` | — | `200` `{ items: [...], page, size, total }` (mais recente primeiro) | `404` |

`motivo` é obrigatório quando `tipo = "AJUSTE"` (`400` se ausente). Em `SAIDA`/`ENTRADA`,
`quantidade` deve ser `> 0`; em `AJUSTE`, `quantidade` é um delta com sinal.

## Formato de erro (RFC 7807)

```json
{
  "type": "https://stockmaster.dev/errors/insufficient-stock",
  "title": "Saldo insuficiente",
  "status": 422,
  "detail": "Produto 'Camiseta P' tem saldo 3, saída de 5 unidades foi rejeitada.",
  "instance": "/api/tenants/.../produtos/.../movimentos"
}
```

Erros de validação (`400`) adicionam `"errors": [{ "field": "quantidade", "message": "deve ser maior que zero" }]`.
