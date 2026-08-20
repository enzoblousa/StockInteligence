# Implementation Plan: Cadastro de Produto

**Feature ID:** 001-cadastro-produto
**Spec:** `specs/001-cadastro-produto/spec.md`
**Constitution:** `memory/constitution.md` v2.1.0
**Tech stack base:** `memory/tech-stack.md`

## Resumo técnico

Um agregado `Produto` concentra as invariantes de negócio (SKU imutável e
único entre ativos, preços não-negativos, transições de status). O lado de
escrita (US-1, US-4, US-5) passa por `Command` → `CommandHandler` →
agregado → `ProdutoRepository`. O lado de leitura (US-2, US-3) passa por
`Query` → `QueryHandler` → projeção direta via Panache, sem reconstruir o
agregado, retornando um único DTO (`ProdutoResult`) tanto para detalhe
quanto para listagem. Persistência única em PostgreSQL (write e read no
mesmo schema), via Hibernate ORM com Panache.

## Constitution Check

| Princípio | Conformidade |
|---|---|
| I. DDD tático | ✅ `Produto` é aggregate root; `SKU` e `Preco` são value objects; sem entidades internas nesta feature. |
| II. CQRS | ✅ Commands e Queries em pacotes separados; `QueryHandler` não usa o agregado nem o repositório de escrita. |
| III. Pureza do domínio | ✅ `domain/` sem anotações Quarkus/JPA; mapeamento feito em `ProdutoJpaEntity` (infra). |
| IV. Contratos explícitos | ✅ `ProdutoRepository` é interface em `domain/repository`; `ProdutoResource` só conhece Commands/Queries, nunca o repositório diretamente. |
| V. Testabilidade | ✅ `CommandHandler`s testados com `ProdutoRepository` mockado; `QueryHandler`s e `ProdutoResource` cobertos por `@QuarkusTest`. |
| VI. Simplicidade/YAGNI | ✅ Sem domain events (nenhum consumidor existe ainda); sem cadastro dinâmico de Categoria/Unidade de Medida; sem ArchUnit nesta feature (adiado, ver constitution v2.1.0); `id` do agregado é `UUID` puro, sem Value Object dedicado; um único DTO de leitura para detalhe e listagem. |

Nenhum desvio a registrar em Complexity Tracking.

## Decisões técnicas

- **Sem domain events** nesta feature: `ProdutoCadastrado`/`ProdutoInativado`
  etc. seriam prematuros sem nenhum consumidor definido (viola YAGNI).
  Revisitar quando uma feature futura precisar reagir a mudanças de produto
  (ex.: auditoria, notificação).
- **`id` é `UUID` puro**, gerado em `Produto` no momento da criação
  (`UUID.randomUUID()`), não delegado a auto-increment do banco — mantém o
  agregado válido antes da persistência, sem precisar de um Value Object
  dedicado (`ProdutoId` foi cortado por baixo valor prático nesta escala).
- **Sem ArchUnit** nesta feature — adiado por decisão da constitution v2.1.0;
  regras de dependência entre camadas garantidas por revisão manual até o
  projeto justificar o custo do teste.
- **Um único DTO de leitura (`ProdutoResult`)**, usado tanto pela busca por
  id/SKU quanto pela listagem — o agregado tem poucos campos, não há ganho
  em manter um DTO "resumo" separado.
- **Unicidade de SKU entre produtos ativos** garantida por índice único
  parcial no PostgreSQL (`WHERE status = 'ATIVO'`), não apenas por checagem
  em aplicação — evita condição de corrida entre dois cadastros
  concorrentes, e ainda permite reativar um produto reaproveitando um SKU
  que outro produto (inativo) já usou no passado.
  ```sql
  CREATE UNIQUE INDEX uq_produto_sku_ativo ON produto (sku) WHERE status = 'ATIVO';
  ```
- **Formato do SKU**: alfanumérico + hífen, 3 a 50 caracteres, normalizado
  para maiúsculas na criação. Regex `^[A-Z0-9\-]{3,50}$`, validada no
  construtor do Value Object `SKU` (domínio), não apenas na borda.
- **Valores de `Categoria`**: `ALIMENTOS, BEBIDAS, LIMPEZA, ELETRONICOS,
  VESTUARIO, OUTROS` — placeholder de MVP, ajustável quando o catálogo real
  do negócio for definido.
- **Valores de `UnidadeMedida`**: `UN, KG, G, L, ML, CX, PC, M` — mesmo
  caráter de placeholder do item anterior.
- **Validação em duas camadas, deliberadamente**: Bean Validation nos DTOs
  de request (fail-fast na borda) **e** invariantes no construtor/métodos do
  agregado (fonte da verdade, protege qualquer outro caller futuro).
- **Contrato OpenAPI não é escrito à mão** — gerado automaticamente pelo
  `smallrye-openapi` a partir das anotações JAX-RS/DTOs em `ProdutoResource`,
  exposto em `/q/swagger-ui` (constitution v2.1.0).

## Estrutura de pacotes desta feature

```
domain/
  model/
    Produto.java              # aggregate root (id: UUID)
    SKU.java                   # value object
    Preco.java                 # value object (não-negativo)
    Categoria.java             # enum
    UnidadeMedida.java         # enum
    StatusProduto.java         # enum (ATIVO, INATIVO)
    TransicaoDeStatusInvalidaException.java
  repository/
    ProdutoRepository.java     # interface

application/
  command/
    CadastrarProdutoCommand.java
    CadastrarProdutoCommandHandler.java
    AtualizarProdutoCommand.java
    AtualizarProdutoCommandHandler.java
    InativarProdutoCommand.java
    InativarProdutoCommandHandler.java
    ReativarProdutoCommand.java
    ReativarProdutoCommandHandler.java
  query/
    BuscarProdutoPorIdQuery.java
    BuscarProdutoPorSkuQuery.java
    ListarProdutosQuery.java
    ProdutoResult.java              # único DTO de leitura (detalhe + listagem)
    ProdutoQueryRepository.java     # porta de leitura
    BuscarProdutoQueryHandler.java
    ListarProdutosQueryHandler.java

infrastructure/
  adapter/in/web/
    ProdutoResource.java
    dto/
      CadastrarProdutoRequest.java
      AtualizarProdutoRequest.java
      ProdutoResponse.java
  adapter/out/persistence/write/
    ProdutoJpaEntity.java
    ProdutoRepositoryImpl.java      # implementa domain/repository/ProdutoRepository
  adapter/out/persistence/read/
    ProdutoQueryRepositoryImpl.java # implementa application/query/ProdutoQueryRepository
```

## Modelo de dados

### Agregado `Produto`

| Campo | Tipo | Regra |
|---|---|---|
| `id` | `UUID` | Gerado na criação, imutável. |
| `sku` | `SKU` (VO) | Imutável após criação; único entre produtos com status `ATIVO`; formato `^[A-Z0-9\-]{3,50}$`. |
| `nome` | `String` | Obrigatório, não-branco, máx. 200 caracteres. |
| `categoria` | `Categoria` (enum) | Obrigatório. |
| `unidadeMedida` | `UnidadeMedida` (enum) | Obrigatório. |
| `precoCusto` | `Preco` (VO) | Obrigatório, ≥ 0. |
| `precoVenda` | `Preco` (VO) | Obrigatório, ≥ 0. |
| `status` | `StatusProduto` (enum) | `ATIVO` na criação. |

**Invariantes:**
1. SKU não pode ser alterado após a criação (nenhum método expõe alteração).
2. `precoCusto`/`precoVenda` nunca negativos (garantido pelo VO `Preco`).
3. `inativar()` só a partir de `ATIVO`; `reativar()` só a partir de
   `INATIVO` — transição inválida lança `TransicaoDeStatusInvalidaException`.
4. Reativação depende de checagem externa de unicidade de SKU entre ativos
   (responsabilidade do `CommandHandler` via `ProdutoRepository`).

### Schema físico (PostgreSQL) — `produto`

```sql
CREATE TABLE produto (
    id              UUID PRIMARY KEY,
    sku             VARCHAR(50)     NOT NULL,
    nome            VARCHAR(200)    NOT NULL,
    categoria       VARCHAR(30)     NOT NULL,
    unidade_medida  VARCHAR(10)     NOT NULL,
    preco_custo     NUMERIC(12,2)   NOT NULL CHECK (preco_custo >= 0),
    preco_venda     NUMERIC(12,2)   NOT NULL CHECK (preco_venda >= 0),
    status          VARCHAR(10)     NOT NULL,
    criado_em       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_produto_sku_ativo ON produto (sku) WHERE status = 'ATIVO';
CREATE INDEX ix_produto_categoria ON produto (categoria);
CREATE INDEX ix_produto_status ON produto (status);
```

`ProdutoJpaEntity` mapeia 1:1 para `produto`; `ProdutoRepositoryImpl` converte
`Produto` ↔ `ProdutoJpaEntity` (o agregado nunca é anotado com JPA).
`ProdutoQueryRepositoryImpl` lê a mesma tabela via projeção JPQL direto para
`ProdutoResult`, sem instanciar `Produto` nem `ProdutoJpaEntity` completo:

```sql
SELECT p.id, p.sku, p.nome, p.categoria, p.unidade_medida,
       p.preco_custo, p.preco_venda, p.status
FROM produto p
WHERE (:categoria IS NULL OR p.categoria = :categoria)
  AND (:status IS NULL OR p.status = :status)
ORDER BY p.nome
```

## Endpoints REST (mapeamento User Story → endpoint)

| User Story | Método | Rota | Command/Query |
|---|---|---|---|
| US-1 | `POST` | `/api/produtos` | `CadastrarProdutoCommand` |
| US-2 | `GET` | `/api/produtos/{id}` | `BuscarProdutoPorIdQuery` |
| US-2 | `GET` | `/api/produtos/sku/{sku}` | `BuscarProdutoPorSkuQuery` |
| US-3 | `GET` | `/api/produtos?categoria=&status=&page=&size=` | `ListarProdutosQuery` |
| US-4 | `PUT` | `/api/produtos/{id}` | `AtualizarProdutoCommand` |
| US-5 | `PATCH` | `/api/produtos/{id}/inativar` | `InativarProdutoCommand` |
| US-5 | `PATCH` | `/api/produtos/{id}/reativar` | `ReativarProdutoCommand` |

Erros: `400` dado inválido, `404` produto não encontrado, `409` SKU
duplicado/transição de status inválida. Schema exato de request/response
documentado via anotações no código, publicado em `/q/swagger-ui`.

## Quickstart

```bash
./mvnw quarkus:dev
```

Dev Services sobe um PostgreSQL automaticamente; Flyway aplica a migração na
subida. Swagger UI em `http://localhost:8080/q/swagger-ui`.

```bash
curl -X POST http://localhost:8080/api/produtos \
  -H "Content-Type: application/json" \
  -d '{"sku":"BEB-001","nome":"Refrigerante 2L","categoria":"BEBIDAS","unidadeMedida":"UN","precoCusto":4.50,"precoVenda":7.90}'
```

```bash
./mvnw test
```

Testes de domínio/handlers rodam puros (JUnit 5 + Mockito, sem subir o
Quarkus); testes de `ProdutoResource` e dos repositórios são `@QuarkusTest`
contra um PostgreSQL real via Dev Services.

## Fase seguinte

`tasks.md` — gerado a partir deste plano.

## Complexity Tracking

Nenhum desvio da constitution nesta feature.
