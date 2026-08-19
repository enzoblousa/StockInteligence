# 05 — Roadmap

Status: **Aceito** · Última revisão: 2026-08-19

## M0 — Spec (concluído em 2026-08-19)

Visão, requisitos, modelo de domínio, arquitetura, contrato de API e ADRs 0001–0008. Base para
todo trabalho de código a partir daqui (ver `CLAUDE.md`, fluxo SDD).

## M1 — Fundação do backend

- Scaffold Quarkus/Maven (`backend/`).
- Migrations Flyway: `tenants`, `produtos` (com `version`), `movimentos_estoque`.
- Entidades de domínio + portas de repositório (`domain/`), sem dependência de framework.
- Testes unitários puros das invariantes (saldo nunca negativo, cálculo de delta por tipo).

## M2 — Casos de uso + API

- `application/`: `TenantService`, `ProdutoService`, `MovimentoEstoqueService` (transação
  descrita em `02-domain-model.md`).
- `infrastructure/rest`: `TenantResource`, `ProdutoResource`, `MovimentoResource`, mapeamento
  RFC 7807 (`03-architecture.md`).
- Testes de integração com Testcontainers, incluindo o teste de concorrência (ADR-0006).
- OpenAPI exposto em `/q/openapi`, validado manualmente contra `04-api-contract.md`.

## M3 — Frontend MVP

- Scaffold Vite + React + TS (`frontend/`).
- Client gerado via `openapi-typescript` a partir do OpenAPI do backend (ADR-0008).
- Telas: seleção/criação de loja, lista de produtos (com filtro de estoque baixo), formulário de
  produto, formulário de movimentação (tratando `422`/`409`), histórico de movimentações.

## M4 — Deploy em cloud

- Terraform (`infra/`): ECR, App Runner, RDS Postgres free tier, IAM/SSM (ADR-0007).
- GitHub Actions: CI (build+test em todo push/PR) e workflow de deploy manual
  (`workflow_dispatch`).
- Frontend publicado na Vercel.
- AWS Budgets com alerta; README documenta `terraform destroy` para desligar tudo entre demos.

## M5 — Polimento de demo

- Script de seed com dados de exemplo (loja + produtos + movimentações).
- README com passo a passo, URL pública, screenshots/GIF.
- Texto curto ligando as decisões de arquitetura aos ADRs correspondentes, para leitura de
  portfólio.

## Pós-MVP (direção declarada, não implementada agora)

Ordem de prioridade — autenticação vem antes de qualquer feature de negócio nova, por causa do
risco aceito no ADR-0004:

1. **Autenticação real** (usuário + papel por loja) — supersede ADR-0004; provavelmente API key
   simples como passo intermediário antes de OIDC completo (ver alternativas do ADR-0004).
2. **Pedidos multi-item** (compra/venda como documento com várias linhas, gerando várias
   movimentações atomicamente) — evolução de `02-domain-model.md`.
3. Notificação ativa de estoque baixo (email/webhook), hoje é só consulta (RF-10).
4. Relatórios e exportação de dados.
5. Revisitar ADR-0007 se o custo pós free-tier da AWS virar problema (candidata: Fly.io/Render +
   Neon).
