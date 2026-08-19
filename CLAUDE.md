# Stock Master — Guia do Projeto (Spec-Driven Development)

> Nome de trabalho: **Stock Master** (troque livremente em `docs/spec/00-vision.md`,
> é só atualizar o spec, não há acoplamento em código ainda).

Controle de estoque inteligente para microempreendedores e lojistas de médio porte, com compras e
vendas integradas, construído como projeto de portfólio nível sênior em **Java + Quarkus**
(backend), **React + TypeScript** (frontend simples) e **AWS** (infraestrutura cloud). Visão de
médio prazo: features de IA (previsão de demanda, sugestão de reposição) e integração com IoT
(leitores de código de barras, câmeras para contagem automática) — ver `docs/spec/00-vision.md`.

Este arquivo é a porta de entrada para qualquer pessoa (ou instância de Claude Code) que for
trabalhar neste repositório. Leia isto antes de tocar em código.

## Princípio central: Spec-Driven Development (SDD)

**A spec é a fonte de verdade, não o código.** Nenhuma funcionalidade nova ou mudança de
arquitetura é implementada antes de estar refletida em `docs/spec/`. Fluxo obrigatório para
qualquer mudança não-trivial:

1. **Specify** — atualize ou crie o documento em `docs/spec/` que descreve o comportamento
   desejado (requisito funcional/não-funcional, regra de domínio). Se já existe um documento que
   cobre o caso, confirme que ele está atualizado.
2. **Decide** — se a mudança envolve escolha de tecnologia, padrão arquitetural ou troca de uma
   decisão anterior, registre um novo ADR em `docs/adr/` (nunca edite um ADR aceito — ele é
   histórico; uma mudança de decisão gera um novo ADR que **supersede** o antigo).
3. **Plan** — quebre em tarefas e posicione no `docs/spec/05-roadmap.md` (marco existente ou
   novo).
4. **Implement** — escreva o código seguindo a spec e a arquitetura descrita em
   `docs/spec/03-architecture.md`.
5. **Reconcile** — se durante a implementação a realidade divergir da spec (e a divergência for a
   decisão certa), volte e atualize a spec. A spec nunca pode ficar desatualizada em relação ao
   código por muito tempo.

Isso vale tanto para humanos quanto para agentes trabalhando neste repo.

## Mapa da documentação

| Documento | Conteúdo |
|---|---|
| `docs/spec/00-vision.md` | Problema, proposta de valor, personas, objetivos e não-objetivos |
| `docs/spec/01-requirements.md` | Requisitos funcionais por módulo + requisitos não-funcionais |
| `docs/spec/02-domain-model.md` | Bounded contexts, agregados, invariantes, eventos de domínio |
| `docs/spec/03-architecture.md` | Arquitetura, stack completa, diagramas, estratégia de testes |
| `docs/adr/000X-*.md` | Registro histórico de decisões arquiteturais (imutável) |
| `docs/spec/05-roadmap.md` | Marcos de entrega e ordem de implementação |

## Constituição (regras não-negociáveis)

Estas regras existem para manter o projeto em nível sênior de qualidade. Mudá-las exige um ADR.

1. **Sem lógica de negócio em REST resources.** Resources (controllers) só traduzem
   HTTP ↔ chamadas de use case. Regra de negócio vive na camada de domínio/aplicação
   (ver `03-architecture.md`).
2. **Persistência via migrations.** Schema do banco só muda via Flyway. `hibernate.hbm2ddl` nunca
   é `update`/`create` fora de testes.
3. **Contract-first na API.** Todo endpoint é documentado via OpenAPI (SmallRye OpenAPI); o
   client TypeScript do frontend é gerado a partir do contrato, não escrito à mão.
4. **Dinheiro e quantidade nunca em `double`/`float`.** Use `BigDecimal` para valores monetários e
   tipos inteiros/decimais explícitos para quantidades, com escala definida.
5. **Toda movimentação de estoque é imutável e rastreável.** Nunca fazemos `UPDATE` em saldo sem
   passar por um registro de movimentação (ledger) que aponta para o documento de origem
   (pedido de compra/venda/ajuste) e o usuário responsável.
6. **Concorrência de estoque é tratada explicitamente.** Toda escrita de saldo usa controle de
   concorrência otimista (`@Version`) — nunca "ler, calcular, gravar" sem proteção contra corrida.
7. **Autenticação/autorização real desde o início.** Endpoints protegidos por papel via
   Keycloak/OIDC (`@RolesAllowed`), nunca "sem auth temporário" em código que vai para o roadmap
   além do M0.
8. **Segredos nunca commitados.** Credenciais/URLs sensíveis vêm de variáveis de ambiente ou
   secrets do CI/CD; `.env` de exemplo (`.env.example`) é o único versionado.
9. **Testes obrigatórios para regra de domínio.** Lógica de domínio (agregados, invariantes) tem
   teste unitário puro (sem framework); fluxos críticos (compra→estoque, venda→estoque) têm teste
   de integração com Testcontainers.
10. **Erros seguem RFC 7807 (`application/problem+json`)** em toda a API — nunca stack trace cru
    para o cliente.

## Stack (resumo — detalhes e justificativa em `03-architecture.md` e nos ADRs)

- **Backend:** Java 21, Quarkus 3.x, Maven, Hibernate ORM + Panache, PostgreSQL, Flyway,
  SmallRye OpenAPI, Quarkus OIDC (Keycloak), Micrometer + OpenTelemetry.
- **Frontend:** React 18 + TypeScript, Vite, TanStack Query, React Hook Form + Zod,
  Tailwind + shadcn/ui, client OpenAPI-gerado, keycloak-js para login. Escopo deliberadamente
  enxuto (poucas telas) — o investimento de profundidade é no backend.
- **Infra:** Docker Compose (dev local: Postgres + Keycloak), GitHub Actions (CI), Terraform
  (IaC), deploy em **AWS** (App Runner + RDS Postgres para backend/Keycloak) e Vercel/Netlify
  (frontend estático) — ver ADR-0009.

## Layout do repositório (alvo, será criado nas próximas fases)

```
/backend      -> serviço Quarkus (Maven)
/frontend     -> SPA React + Vite
/infra        -> docker-compose.yml, módulos Terraform (AWS), infra/keycloak/realm-export.json
/.github      -> workflows de CI/CD
/docs/spec    -> especificações vivas
/docs/adr     -> Architecture Decision Records
```

## Estado atual

Fase de **spec** concluída para o MVP (M0–M6, ver roadmap). Scaffolding de código ainda não
iniciado — próximo passo é M0 (fundação) conforme `docs/spec/05-roadmap.md`.
