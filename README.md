'# StockInteligence

Sistema de gestão de estoque/inventário — backend Java/Quarkus + frontend
React, construído do zero seguindo **Spec-Driven Development (SDD)**: cada
funcionalidade nasce de uma especificação de negócio, passa por um plano
técnico revisado e só então vira código.

Este documento existe para dar a qualquer pessoa (recrutador, revisor de
código, ou eu mesmo daqui a 6 meses) uma visão completa do projeto sem
precisar ler o histórico de commits.

---

## O que é

Um painel de gestão de estoque com cadastro de produtos: criar, consultar,
listar com filtro, editar e inativar/reativar produtos — com as regras de
negócio de verdade (SKU único entre produtos ativos, sem exclusão física,
preços não-negativos) implementadas no backend, não só validadas na tela.

O escopo hoje é a primeira fatia vertical completa (cadastro de produto,
ponta a ponta); o roadmap natural é movimentação de estoque (entrada/saída,
saldo por SKU) em cima dessa base.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21 (LTS), Quarkus 3.38, Maven |
| Persistência | PostgreSQL, Hibernate ORM + Panache, Flyway |
| Testes backend | JUnit 5, Mockito, AssertJ, RestAssured, Quarkus Dev Services (Testcontainers) |
| Frontend | React 18, Vite, JavaScript (ES2022+, sem TypeScript por decisão deliberada) |
| HTTP/roteamento | Axios, React Router |
| UI | Bootstrap / React-Bootstrap |
| Infra local | Docker (Dev Services do Quarkus sobe o Postgres sozinho) |

## Arquitetura

**Backend: Domain-Driven Design (tático) + CQRS, sem Event Sourcing.**

- Um agregado (`Produto`) concentra toda a regra de negócio; Value Objects
  (`SKU`, `Preco`) validam invariantes no próprio construtor.
- Lado de escrita e leitura são explicitamente separados: `Command` →
  `CommandHandler` → agregado → repositório de escrita; `Query` →
  `QueryHandler` → projeção direta (sem passar pelo agregado).
- Write e read model dividem o mesmo banco — CQRS aqui é uma separação de
  código/responsabilidade, não de infraestrutura (decisão deliberada,
  registrada em `memory/constitution.md`).

```
backend/src/main/java/.../estoque/
  domain/            → agregado, value objects, exceções de negócio (Java puro, zero framework)
  application/
    command/          → intenção de escrita + handler (1 caso de uso cada)
    query/            → intenção de leitura + handler (nunca toca o agregado)
  infrastructure/
    adapter/in/web/    → REST (JAX-RS), DTOs, exception mappers
    adapter/out/persistence/
      write/            → Panache + entidade JPA, isolada do domínio
      read/             → projeção JPQL direto pro DTO de leitura
```

**Frontend: SPA simples, sem over-engineering.** Client Axios centralizado,
uma camada de serviço única por onde toda chamada HTTP passa (nunca Axios
direto num componente), tratamento uniforme de erro, `useState`/`useEffect`
nativos — sem Redux/TanStack Query, porque o escopo não justifica.

## Metodologia: Spec-Driven Development

Nenhuma linha de código de feature foi escrita antes de existir uma spec.
Fluxo usado em cada funcionalidade:

1. **`memory/constitution.md`** — princípios do projeto (arquitetura, stack,
   estratégia de testes), alterado raramente e só por emenda explícita
   versionada (hoje na v2.2.0, com histórico completo de mudanças).
2. **`specs/NNN-nome/spec.md`** — o quê e para quem, em linguagem de
   negócio, com User Stories e critérios de aceite verificáveis
   (Given/When/Then). Zero menção a framework ou banco.
3. **`specs/NNN-nome/plan.md`** — tradução técnica da spec: agregados,
   contratos, schema, decisões de implementação e um "Constitution Check"
   explícito confirmando conformidade antes de codar.
4. **`specs/NNN-nome/tasks.md`** — tarefas atômicas, ordenadas por
   dependência, cada uma rastreável até uma User Story ou requisito.

Duas features completas seguiram esse fluxo integralmente:
[`001-cadastro-produto`](specs/001-cadastro-produto) (backend) e
[`002-frontend-cadastro-produto`](specs/002-frontend-cadastro-produto)
(frontend) — dá pra ler o histórico de decisão de cada uma direto nesses
arquivos.

## Qualidade e testes

**54 testes automatizados no backend**, organizados por uma regra
explícita (`memory/testing-strategy.md`): **cada regra de negócio tem uma
única camada dona da sua cobertura exaustiva** — o domínio, na maioria dos
casos. As demais camadas testam só o que é exclusivo delas (orquestração,
SQL, HTTP), nunca reafirmam o mesmo cenário. Isso existiu desde o início
como decisão consciente, não como limpeza tardia — ver a evolução real
dessa ideia na conversa que gerou `testing-strategy.md`.

| Camada | O que testa | Ferramenta |
|---|---|---|
| Domínio (agregado, VOs) | Toda regra de negócio, exaustivamente | JUnit 5 + AssertJ, sem Quarkus |
| Command/Query Handlers | Só orquestração (não repete regra do domínio) | JUnit 5 + Mockito |
| Repositórios | Round-trip de mapeamento + constraints do banco | `@QuarkusTest` contra Postgres real |
| REST Resource | Só fiação HTTP (status code, request→command) | `@QuarkusTest` + RestAssured |

## Decisões de engenharia que valem destacar

- **Arquitetura mudou de rumo no meio do processo, por escolha, não por
  erro**: começou como hexagonal, foi para DDD tático + CQRS depois de uma
  conversa explícita sobre trade-offs — a decisão e o motivo estão
  registrados no histórico de versões da constitution, não escondidos.
- **CQRS foi mantido mesmo sendo "demais" para um CRUD simples** — decisão
  do responsável pelo projeto, registrada como tal (`plan.md` da feature
  001), não uma escolha não-examinada.
- **ArchUnit foi deliberadamente adiado**: a decisão foi "isso protege
  fronteiras que importam quando o projeto cresce; agora é custo sem
  retorno imediato" — registrada e revisável, não esquecida.
- **Um bug de CORS real foi encontrado e corrigido durante a validação
  manual do frontend com um navegador de verdade** — testes de API
  (RestAssured, `curl`) não capturam CORS porque não aplicam política de
  mesma origem; só apareceu ao testar via Chrome. Documentado em
  `specs/002-frontend-cadastro-produto/plan.md` § "Descobertas durante a
  implementação", incluindo a causa raiz (renomeação silenciosa de
  `quarkus.http.cors` para `quarkus.http.cors.enabled` em versões
  recentes do Quarkus).

## Como rodar

Pré-requisitos: JDK 21, Node 18+, Docker (para o Postgres do backend).

```bash
# Backend
cd backend
./mvnw quarkus:dev
# API em http://localhost:8080, Swagger UI em /q/swagger-ui

# Frontend (em outro terminal)
cd frontend
npm install
cp .env.example .env
npm run dev
# UI em http://localhost:5173/produtos
```

Rodar os testes do backend: `cd backend && ./mvnw test` (precisa do Docker
rodando — os testes de integração usam Dev Services/Testcontainers).

## Estrutura do repositório

```
StockInteligence/
  memory/                          # documentação de processo (constitution, tech stack, testes)
  specs/
    001-cadastro-produto/          # spec, plan e tasks do backend
    002-frontend-cadastro-produto/ # spec, plan e tasks do frontend
  backend/                         # Quarkus — domain / application / infrastructure
  frontend/                        # React — api / components / pages
```

## Roadmap

- Movimentação de estoque (entrada/saída, saldo por SKU) — próxima feature
  natural sobre o cadastro de produto já pronto.
- Deploy (avaliado: OpenShift Developer Sandbox e AWS Free Tier, em
  paralelo, como exercício de multi-cloud).
- Testes automatizados de frontend.

## Links

- PR #1 (backend): https://github.com/enzoblousa/StockInteligence/pull/1
- PR #2 (frontend): https://github.com/enzoblousa/StockInteligence/pull/2
- Constitution do projeto: [`memory/constitution.md`](memory/constitution.md)
