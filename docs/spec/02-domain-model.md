# 02 — Modelo de Domínio

Status: **Aceito** · Última revisão: 2026-08-19

## Bounded context

Um único bounded context no MVP: **Estoque**. O domínio é pequeno o suficiente para não
justificar múltiplos contextos ainda (ver ADR-0002).

## Agregados

### `Tenant` (Loja) — raiz de agregado

| Campo | Tipo | Regra |
|---|---|---|
| `id` | UUID | gerado no cadastro |
| `nome` | String | obrigatório, não vazio |
| `criadoEm` | timestamp | imutável |

### `Produto` — raiz de agregado

| Campo | Tipo | Regra |
|---|---|---|
| `id` | UUID | gerado no cadastro |
| `tenantId` | UUID | FK lógica para `Tenant`, imutável |
| `sku` | String | único **dentro da loja** (constraint composta `tenantId+sku`) |
| `nome` | String | obrigatório |
| `unidadeMedida` | String | ex.: "un", "kg", "cx" |
| `custoUnitario` | BigDecimal, nullable | opcional no MVP |
| `precoVenda` | BigDecimal, nullable | opcional no MVP |
| `estoqueMinimo` | Integer | `>= 0`, usado para alerta de estoque baixo |
| `saldoAtual` | Integer | **invariante: nunca `< 0`**; só muda via `MovimentoEstoque` |
| `ativo` | boolean | default `true` |
| `version` | Long | controle de concorrência otimista (ADR-0006) |

**Invariantes:**
- `saldoAtual >= 0` sempre, em qualquer momento observável.
- `sku` único por `tenantId`.
- `saldoAtual` só é alterado como efeito colateral transacional de registrar um
  `MovimentoEstoque` — nunca por uma edição direta de metadados (RF-04).

### `MovimentoEstoque` — registro imutável (ledger), não é agregado próprio no sentido de ter
regra de negócio interna além da criação; referencia `Produto`.

| Campo | Tipo | Regra |
|---|---|---|
| `id` | UUID | gerado na criação |
| `tenantId` | UUID | denormalizado do produto, para consulta/isolamento direto |
| `produtoId` | UUID | FK para `Produto` |
| `tipo` | Enum `ENTRADA \| SAIDA \| AJUSTE` | obrigatório |
| `quantidade` | Integer | `ENTRADA`/`SAIDA`: `> 0`. `AJUSTE`: delta com sinal (pode ser negativo) |
| `motivo` | String, nullable | obrigatório quando `tipo = AJUSTE`; opcional nos demais |
| `criadoEm` | timestamp | gerado na criação |

**Invariantes:**
- Imutável: sem endpoint de update/delete (RF-08).
- Uma `SAIDA` (ou `AJUSTE` negativo) que resultaria em `saldoAtual < 0` no `Produto` é rejeitada
  **antes** de qualquer escrita (nenhum efeito parcial).

## Serviço de domínio/aplicação: registrar movimentação

Fluxo (camada `application`, ver `03-architecture.md`), dentro de **uma única transação**:

1. Carregar `Produto` (com `version` atual) por `tenantId` + `produtoId`.
2. Calcular `novoSaldo = saldoAtual + delta(tipo, quantidade)`.
3. Se `novoSaldo < 0` → rejeitar (erro de negócio, HTTP 422, ver `04-api-contract.md`), nada é
   persistido.
4. Persistir `Produto.saldoAtual = novoSaldo` (escrita otimista via `version`) e inserir o
   `MovimentoEstoque` correspondente.
5. Se a escrita do `Produto` falhar por conflito de versão (`OptimisticLockException`) →
   propagar como conflito (HTTP 409) — nada é persistido, cliente decide se tenta de novo.

## Consulta derivada: estoque baixo

Não é uma entidade armazenada. É uma query: produtos de um `tenantId` onde
`saldoAtual <= estoqueMinimo AND ativo = true` (RF-10, RF-05).

## Extensões conceituais fora do MVP (não implementadas)

- Evento de domínio explícito (`MovimentoRegistrado`) publicado num barramento — hoje o próprio
  registro persistido de `MovimentoEstoque` já cumpre o papel de "fato ocorrido"; um barramento
  de eventos só se justificaria se surgir um segundo consumidor (ex.: notificação assíncrona).
- `Pedido` (compra/venda multi-item) como agregado que gera várias `MovimentoEstoque` de uma vez
  — ver `05-roadmap.md`, pós-MVP.
- `Usuario`/`Papel` vinculados a `Tenant` — entra junto com autenticação real (ADR-0004).
