# Tasks: Cadastro de Produto

**Input:** `spec.md`, `plan.md`
**Convenção:** `[P]` = pode ser executada em paralelo com outras `[P]` da
mesma fase (arquivos distintos, sem dependência entre si). Tarefas sem `[P]`
são sequenciais dentro da fase. Fases são sequenciais entre si — nenhuma task
de uma fase começa antes de todas as tasks obrigatórias da fase anterior.

---

## Fase 1 — Setup do projeto (uma vez, não se repete nas próximas features)

- [x] **T001** Gerar esqueleto do projeto Quarkus (`quarkus create app`) com
  groupId/artifactId do projeto, Java 21, Maven, e as extensões listadas em
  `memory/tech-stack.md`: `rest`, `rest-jackson`, `hibernate-validator`,
  `smallrye-openapi`, `hibernate-orm-panache`, `jdbc-postgresql`, `flyway`,
  `smallrye-health`.
- [x] **T002** Adicionar ao `pom.xml` as dependências de teste não cobertas
  pelo `quarkus create app`: `mockito-core` (escopo `test`; `assertj-core` e
  `quarkus-junit5` já vêm pelo BOM/arquétipo).
- [x] **T003** Criar a estrutura de pacotes base sob
  `com.stockinteligence.estoque` conforme `plan.md`: `domain.model`,
  `domain.repository`, `application.command`, `application.query`,
  `infrastructure.adapter.in.web`, `infrastructure.adapter.in.web.dto`,
  `infrastructure.adapter.out.persistence.write`,
  `infrastructure.adapter.out.persistence.read`.
- [x] **T004** `[P]` Configurar `application.properties` mínimo (nome da
  aplicação, log level; datasource fica implícito via Dev Services em
  dev/test).

**Checkpoint:** projeto compila, `./mvnw test` roda, `./mvnw quarkus:dev`
sobe sem erro.

---

## Fase 2 — Domínio (`plan.md` › Modelo de dados; Princípios I e III)

- [x] **T005** `[P]` Criar enum `StatusProduto` (`ATIVO`, `INATIVO`) em
  `domain.model`.
- [x] **T006** `[P]` Criar enum `Categoria` em `domain.model` com os valores
  de `plan.md` › Decisões técnicas.
- [x] **T007** `[P]` Criar enum `UnidadeMedida` em `domain.model` com os
  valores de `plan.md` › Decisões técnicas.
- [x] **T008** `[P]` Escrever testes unitários do value object `SKU`
  (formato válido/inválido, normalização para maiúsculas) — antes da
  implementação.
- [x] **T009** Implementar value object `SKU` em `domain.model` satisfazendo
  T008 (regex `^[A-Z0-9\-]{3,50}$`, imutável, `equals`/`hashCode` por valor).
- [x] **T010** `[P]` Escrever testes unitários do value object `Preco`
  (rejeita negativo e nulo) — antes da implementação.
- [x] **T011** Implementar value object `Preco` em `domain.model`
  satisfazendo T010.
- [x] **T012** Escrever testes unitários do agregado `Produto` cobrindo as
  invariantes de `plan.md` (criação válida com `id` gerado via
  `UUID.randomUUID()`; SKU imutável — sem setter/método de alteração; preços
  negativos rejeitados na criação; `inativar()` só a partir de `ATIVO`;
  `reativar()` só a partir de `INATIVO`; transição inválida lança exceção de
  domínio) — antes da implementação.
- [x] **T013** Implementar agregado `Produto` em `domain.model` satisfazendo
  T012, incluindo método de fábrica estático (`Produto.cadastrar(...)`),
  `atualizarDados(...)`, `inativar()`, `reativar()`.
- [x] **T014** `[P]` Criar exceção de domínio
  `TransicaoDeStatusInvalidaException` em `domain.model`, lançada por T013.
- [x] **T015** Criar interface `ProdutoRepository` em `domain.repository`
  com as operações necessárias ao command side: `salvar(Produto)`,
  `buscarPorId(UUID)`, `buscarPorSku(SKU)`, `existeAtivoComSku(SKU)`.

**Checkpoint:** `domain` compila e testa isoladamente, sem nenhuma
dependência de `infrastructure`.

---

## Fase 3 — Application: Command side (FR-001, FR-004, FR-007, FR-008, FR-009)

- [x] **T016** `[P]` Criar `CadastrarProdutoCommand` (record) em
  `application.command`.
- [x] **T017** Escrever teste de `CadastrarProdutoCommandHandler` com
  `ProdutoRepository` mockado: caso feliz (US-1) e caso de SKU duplicado
  entre ativos (US-1, `existeAtivoComSku` retorna true → rejeita) — antes da
  implementação.
- [x] **T018** Implementar `CadastrarProdutoCommandHandler` satisfazendo
  T017.
- [x] **T019** `[P]` Criar `AtualizarProdutoCommand` (record, sem campo
  `sku` — FR-004) em `application.command`.
- [x] **T020** Escrever teste de `AtualizarProdutoCommandHandler`: caso
  feliz (US-4) e produto inexistente (404 de domínio) — antes da
  implementação.
- [x] **T021** Implementar `AtualizarProdutoCommandHandler` satisfazendo
  T020.
- [x] **T022** `[P]` Criar `InativarProdutoCommand` e
  `ReativarProdutoCommand` (records) em `application.command`.
- [x] **T023** Escrever testes de `InativarProdutoCommandHandler` e
  `ReativarProdutoCommandHandler`: caso feliz de cada um (US-5) e
  reativação bloqueada por SKU em uso por outro produto ativo — antes da
  implementação. Não retestar aqui a invalidade da transição em si
  (já-inativo/já-ativo): isso é regra do agregado, já coberta
  exaustivamente em T012 (ver `memory/testing-strategy.md`).
- [x] **T024** Implementar `InativarProdutoCommandHandler` e
  `ReativarProdutoCommandHandler` satisfazendo T023.

**Checkpoint:** todos os `CommandHandler`s passam em teste puro (sem
Quarkus); regras de negócio de US-1, US-4, US-5 cobertas.

---

## Fase 4 — Application: Query side (FR-005, FR-006, Princípio II)

- [x] **T025** `[P]` Criar `ProdutoResult` (único DTO de leitura, sem regra
  de negócio) em `application.query`.
- [x] **T026** `[P]` Criar `BuscarProdutoPorIdQuery`, `BuscarProdutoPorSkuQuery`
  e `ListarProdutosQuery` (records) em `application.query`.
- [x] **T027** Definir interface `ProdutoQueryRepository` (porta de leitura)
  em `application.query` — exclusiva do read side, não usada pelo command
  side.
- [x] **T028** Implementar `BuscarProdutoQueryHandler` (atende as duas
  queries de busca) usando `ProdutoQueryRepository`.
- [x] **T029** Implementar `ListarProdutosQueryHandler` (com filtro de
  categoria/status e paginação) usando `ProdutoQueryRepository`.

**Checkpoint:** query side compila sem depender de `Produto` (agregado) nem
de `ProdutoRepository` (write) — só de `ProdutoQueryRepository`.

---

## Fase 5 — Infraestrutura: Persistência (write) (`plan.md` › Modelo de dados)

- [x] **T030** Criar migração Flyway
  `src/main/resources/db/migration/V1__create_produto_table.sql` com o DDL
  de `plan.md` (tabela `produto`, índice único parcial, índices auxiliares).
- [x] **T031** Criar `ProdutoJpaEntity` (Panache) em
  `infrastructure.adapter.out.persistence.write`, mapeando 1:1 a tabela
  `produto` — sem nenhuma regra de negócio, só mapeamento.
- [x] **T032** Implementar `ProdutoRepositoryImpl` satisfazendo a interface
  `ProdutoRepository` (T015), convertendo `Produto` ↔ `ProdutoJpaEntity` nas
  duas direções.
- [x] **T033** Escrever teste `@QuarkusTest` de `ProdutoRepositoryImpl`
  contra PostgreSQL real (Dev Services): salvar, buscar por id, buscar por
  sku, `existeAtivoComSku` true/false, e violação do índice único parcial ao
  tentar persistir dois ativos com mesmo SKU em paralelo.

**Checkpoint:** write side persiste e recupera `Produto` corretamente contra
banco real.

---

## Fase 6 — Infraestrutura: Persistência (read) (`plan.md` › Modelo de dados)

- [x] **T034** Implementar `ProdutoQueryRepositoryImpl` (T027) via
  `EntityManager`/Panache com projeção JPQL (constructor expression) para
  `ProdutoResult`, incluindo filtro opcional de categoria/status e paginação
  (query de `plan.md`).
- [x] **T035** Escrever teste `@QuarkusTest` de `ProdutoQueryRepositoryImpl`
  contra PostgreSQL real: busca por id/sku existente e inexistente, listagem
  sem filtro, listagem com filtro de categoria, listagem com filtro de
  status, paginação.

**Checkpoint:** read side não instancia `Produto` nem `ProdutoJpaEntity`
completo em nenhum momento — validável por leitura de código nesta task.

---

## Fase 7 — Infraestrutura: Web (`plan.md` › Endpoints REST)

- [x] **T036** `[P]` Criar DTOs `CadastrarProdutoRequest`,
  `AtualizarProdutoRequest` e `ProdutoResponse` em
  `infrastructure.adapter.in.web.dto`, com anotações Bean Validation.
- [x] **T037** Criar `ProdutoResource` (JAX-RS) em
  `infrastructure.adapter.in.web` implementando as 7 rotas de `plan.md`,
  traduzindo request → Command/Query e despachando para os handlers das
  Fases 3 e 4 (via CDI).
- [x] **T038** Criar `ExceptionMapper` para exceções de domínio (SKU
  duplicado → 409, produto não encontrado → 404, transição de status
  inválida → 409, validação de VO → 400).
- [x] **T039** Escrever testes `@QuarkusTest` + RestAssured de
  `ProdutoResource`: um caso feliz por endpoint (7) + um exemplo de cada
  família de erro (`400`, `404`, `409`) — total, não por endpoint, já que o
  `ExceptionMapper` é único e compartilhado. **Não** reimplementar a matriz
  de Given/When/Then de `spec.md` aqui — essa cobertura já é exaustiva em
  domínio (T012) e handlers (T017/T020/T023); ver
  `memory/testing-strategy.md`.

**Checkpoint:** todos os endpoints de `plan.md` respondem conforme
especificado; Swagger UI acessível em `/q/swagger-ui` (gerado
automaticamente pelo `smallrye-openapi`, sem contrato escrito à mão).

---

## Fase 8 — Validação final

- [x] **T040** Rodar o roteiro de `plan.md` › Quickstart manualmente (ou
  script equivalente) do início ao fim contra `quarkus:dev`, conferindo cada
  endpoint.
- [x] **T041** Revisar `spec.md` → confirmar que todo FR-001 a FR-010 tem
  pelo menos um teste automatizado que o cobre, na camada dona correta
  (rastreabilidade — ver `memory/testing-strategy.md`).

---

## Dependências entre fases

```
Fase 1 (Setup)
   └─▶ Fase 2 (Domínio)
          └─▶ Fase 3 (Command) ──┐
          └─▶ Fase 4 (Query)  ───┤
                                  ├─▶ Fase 5 (Persistência write) ──┐
                                  └─▶ Fase 6 (Persistência read)  ──┤
                                                                     ├─▶ Fase 7 (Web) ─▶ Fase 8 (Validação)
```

Fases 3 e 4 podem ser trabalhadas em paralelo entre si (não têm dependência
mútua). O mesmo vale para Fases 5 e 6. Fase 7 depende de ambas as
persistências estarem prontas, pois `ProdutoResource` despacha para
handlers dos dois lados.
