# 03 — Arquitetura

Status: **Aceito** · Última revisão: 2026-08-19

## Stack

- **Backend:** Java 21, Quarkus 3.x, Maven (módulo único), Hibernate ORM + Panache, PostgreSQL,
  Flyway, SmallRye OpenAPI. (ADR-0001, ADR-0002, ADR-0003, ADR-0008)
- **Frontend:** React 18 + TypeScript, Vite, TanStack Query, client OpenAPI-gerado, CSS simples
  sem design system. (ADR-0005)
- **Dados:** PostgreSQL (RDS free tier em produção, Dev Services/Testcontainers em dev/teste).
  (ADR-0003, ADR-0007)
- **Infra:** Docker (imagem do backend), Terraform (App Runner, RDS, ECR, IAM/SSM), GitHub
  Actions (CI + deploy manual), Vercel (frontend). (ADR-0007)

## Estrutura do backend (pacotes)

```
backend/src/main/java/com/stockmaster/
  domain/
    Tenant.java, Produto.java, MovimentoEstoque.java, TipoMovimento.java
    ProdutoRepository.java (porta/interface), MovimentoRepository.java (porta/interface)
    SaldoInsuficienteException.java, SkuDuplicadoException.java, ...
  application/
    TenantService.java
    ProdutoService.java
    MovimentoEstoqueService.java   <- orquestra a transação descrita em 02-domain-model.md
  infrastructure/
    rest/
      TenantResource.java, ProdutoResource.java, MovimentoResource.java
      ProblemDetailsMapper.java (RFC 7807, ver CLAUDE.md regra 10)
    persistence/
      ProdutoRepositoryImpl.java, MovimentoRepositoryImpl.java (Panache, implementam as portas)
    config/
      (application.properties, perfis dev/test/prod)
  backend/src/main/resources/db/migration/
    V1__create_tenants.sql, V2__create_produtos.sql, V3__create_movimentos.sql, ...
```

Regra dura: `domain` não importa nada de `jakarta.persistence`, `jakarta.ws.rs` ou Panache.
`infrastructure` depende de `domain` (implementa suas portas), nunca o contrário.

## Tratamento de erro (RFC 7807)

| Situação | Status | `type` (sufixo) |
|---|---|---|
| Validação de entrada (campo obrigatório, formato) | 400 | `validation-error` |
| Loja/produto não encontrado | 404 | `not-found` |
| SKU duplicado na loja | 409 | `sku-conflict` |
| Conflito de concorrência otimista (ADR-0006) | 409 | `concurrent-update` |
| Saldo insuficiente para SAIDA (regra de negócio) | 422 | `insufficient-stock` |

Corpo padrão: `{ type, title, status, detail, instance }`; erros de validação incluem
`errors: [{ field, message }]`.

## Estratégia de testes

- **Unitário puro (sem Quarkus):** invariantes de `Produto`/`MovimentoEstoque` (ex.: saldo nunca
  negativo, cálculo de delta por tipo de movimento) — roda em milissegundos, sem Spring/Quarkus
  context.
- **Integração (Testcontainers Postgres):** repositórios reais, e um teste dedicado de
  **concorrência**: duas movimentações simultâneas no mesmo produto (ex.: duas `SAIDA` que juntas
  excederiam o saldo) — assert que uma é aceita, a outra falha (409) ou é rejeitada por saldo
  insuficiente, e o saldo final nunca fica negativo.
- **REST (RestAssured):** fluxos principais (criar loja → produto → movimentação → consultar
  saldo/histórico) e mapeamento de erro (RFC 7807) para cada caso da tabela acima.
- Frontend: sem exigência de cobertura pesada no MVP (é a camada deliberadamente simples); testes
  de componente pontuais são bônus, não bloqueantes.

## Frontend — telas do MVP

1. **Seleção/criação de loja** — tela inicial; `tenantId` escolhido fica em `localStorage`
   (não há login, ver ADR-0004).
2. **Lista de produtos** — com destaque visual e filtro para "abaixo do estoque mínimo".
3. **Formulário de produto** — criar/editar metadados (RF-03, RF-04).
4. **Formulário de movimentação** — registrar entrada/saída/ajuste (RF-06), trata erro 422
   (saldo insuficiente) e 409 (conflito de concorrência) com mensagem específica.
5. **Histórico de movimentações** — por produto, paginado (RF-09).

## CI/CD

- **CI (todo push/PR):** build + testes do backend (Maven) e do frontend (npm), lint.
- **Deploy (`workflow_dispatch`, manual):** build/push da imagem do backend para ECR,
  `terraform apply` da infra AWS, deploy do frontend na Vercel. Deploy manual (não a cada merge)
  para controlar custo/reaplicação durante o desenvolvimento do MVP (ver ADR-0007).

## Dev local

Quarkus Dev Services sobe um Postgres via Testcontainers automaticamente em `quarkus:dev` e nos
testes — não é necessário Docker Compose manual para cobrir o dev básico do backend. Frontend
roda com `vite dev` apontando para o backend local (`http://localhost:8080`).
