# Stock Master — Guia do Projeto (Spec-Driven Development)

> Nome de trabalho: **Stock Master**. Controlador de estoque para pequenos lojistas.

Backend em **Java + Quarkus**, frontend simples em **React + TypeScript**, banco **PostgreSQL**,
infraestrutura em **AWS free tier**. Projeto de portfólio/estudo com foco de profundidade em
backend (domínio, concorrência, arquitetura) e infraestrutura cloud — o frontend é
deliberadamente enxuto. Ver `docs/spec/00-vision.md` para o porquê.

Este arquivo é a porta de entrada para qualquer pessoa (ou instância de Claude Code) que for
trabalhar neste repositório. Leia isto antes de tocar em código.

## Princípio central: Spec-Driven Development (SDD)

**A spec é a fonte de verdade, não o código.** Nenhuma funcionalidade nova ou mudança de
arquitetura é implementada antes de estar refletida em `docs/spec/`. Fluxo obrigatório para
qualquer mudança não-trivial:

1. **Specify** — atualize ou crie o documento em `docs/spec/` que descreve o comportamento
   desejado. Se já existe um documento que cobre o caso, confirme que ele está atualizado.
2. **Decide** — se a mudança envolve escolha de tecnologia, padrão arquitetural ou troca de uma
   decisão anterior, registre um novo ADR em `docs/adr/` (**nunca edite um ADR aceito** — ele é
   histórico; uma mudança de decisão gera um novo ADR que **supersede** o antigo).
3. **Plan** — posicione a mudança em `docs/spec/05-roadmap.md` (marco existente ou novo).
4. **Implement** — escreva o código seguindo a spec e a arquitetura de `docs/spec/03-architecture.md`.
5. **Reconcile** — se durante a implementação a realidade divergir da spec (e a divergência for a
   decisão certa), volte e atualize a spec antes de seguir. A spec nunca fica desatualizada em
   relação ao código por muito tempo.

Vale tanto para humanos quanto para agentes trabalhando neste repo.

## Mapa da documentação

| Documento | Conteúdo |
|---|---|
| `docs/spec/00-vision.md` | Problema, proposta de valor, escopo do MVP, não-objetivos |
| `docs/spec/01-requirements.md` | Requisitos funcionais por módulo + requisitos não-funcionais |
| `docs/spec/02-domain-model.md` | Agregados, invariantes, fluxo transacional |
| `docs/spec/03-architecture.md` | Stack, estrutura de pacotes, tratamento de erro, testes, CI/CD |
| `docs/spec/04-api-contract.md` | Contrato de API (referência humana; machine-readable em `/q/openapi`) |
| `docs/adr/000X-*.md` | Registro histórico de decisões arquiteturais (imutável) |
| `docs/spec/05-roadmap.md` | Marcos de entrega e ordem de implementação |

## Constituição (regras não-negociáveis)

Mudar qualquer uma destas regras exige um ADR novo.

1. **Sem lógica de negócio em REST resources.** Resources só traduzem HTTP ↔ chamada de caso de
   uso. Regra de negócio vive em `domain`/`application` (ver `03-architecture.md`).
2. **Persistência via migrations.** Schema do banco só muda via Flyway. `hibernate.hbm2ddl` nunca
   é `update`/`create` fora de testes.
3. **Contrato de API antes do código.** Todo endpoint é primeiro descrito em
   `docs/spec/04-api-contract.md`, depois implementado; o client TypeScript do frontend é
   **gerado** a partir do OpenAPI, nunca escrito à mão (ADR-0008).
4. **Dinheiro e quantidade nunca em `double`/`float`.** Use `BigDecimal` para valores monetários
   e `Integer`/tipos decimais explícitos para quantidades.
5. **Toda movimentação de estoque é imutável e rastreável.** Nunca fazemos alteração de saldo sem
   um registro de `MovimentoEstoque` correspondente — sem endpoint de editar/apagar movimentação.
6. **Concorrência de estoque é tratada explicitamente.** Toda escrita de saldo usa controle de
   concorrência otimista (`@Version`, ADR-0006) — nunca "ler, calcular, gravar" sem essa proteção.
7. **O MVP roda sem autenticação — decisão explícita e documentada (ADR-0004), não um "TODO
   esquecido".** Tenant é isolado por UUID na URL; não há controle de acesso real. Nenhuma
   feature de negócio nova deve ser priorizada no roadmap antes de autenticação real, uma vez que
   o MVP esteja demonstrado publicamente.
8. **Segredos nunca commitados.** Credenciais/URLs sensíveis vêm de variáveis de ambiente ou
   secrets do CI/CD/SSM; `.env.example` é o único arquivo de ambiente versionado.
9. **Testes obrigatórios para regra de domínio.** Invariantes (`domain`) têm teste unitário puro
   (sem framework); o fluxo de registrar movimentação tem teste de integração com Testcontainers,
   incluindo cenário de concorrência.
10. **Erros seguem RFC 7807 (`application/problem+json`)** em toda a API — nunca stack trace cru
    para o cliente.

## Stack (resumo — detalhes e justificativa em `03-architecture.md` e nos ADRs)

- **Backend:** Java 21, Quarkus 3.x, Maven (módulo único), Hibernate ORM + Panache, PostgreSQL,
  Flyway, SmallRye OpenAPI.
- **Frontend:** React 18 + TypeScript, Vite, TanStack Query, client OpenAPI-gerado, CSS simples
  (sem design system). Escopo deliberadamente enxuto — o investimento de profundidade é no
  backend e na infra.
- **Infra:** Quarkus Dev Services (Postgres via Testcontainers em dev/teste), GitHub Actions
  (CI + deploy manual), Terraform (App Runner + RDS + ECR na AWS, free tier), Vercel (frontend).

## Layout do repositório (alvo — ainda não criado, próximo passo é M1)

```
/backend      -> serviço Quarkus (Maven)
/frontend     -> SPA React + Vite
/infra        -> módulos Terraform (AWS: App Runner, RDS, ECR, IAM/SSM)
/.github      -> workflows de CI/CD
/docs/spec    -> especificações vivas
/docs/adr     -> Architecture Decision Records
```

## Estado atual

Fase de **spec concluída** para o MVP (M0, ver `docs/spec/05-roadmap.md`). Scaffolding de código
ainda não iniciado — próximo passo é **M1** (fundação do backend).
