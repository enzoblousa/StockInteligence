# Tasks: Controle de Saldo de Estoque e Alerta de Estoque Baixo

**Input:** `spec.md`, `plan.md`
**Convenção:** `[P]` = pode ser executada em paralelo com outras `[P]` da
mesma fase (arquivos distintos, sem dependência entre si). Tarefas sem `[P]`
são sequenciais dentro da fase. Fases são sequenciais entre si — nenhuma task
de uma fase começa antes de todas as tasks obrigatórias da fase anterior.

---

## Fase 1 — Setup (mensageria, novo neste projeto)

- [x] **T001** Adicionar `quarkus-messaging-kafka` ao `pom.xml`
  (`plan.md` › Configuração de mensageria).
- [x] **T002** `[P]` Adicionar dependência de teste
  `io.smallrye.reactive:smallrye-reactive-messaging-in-memory` (escopo
  `test`) ao `pom.xml`.
- [x] **T003** `[P]` Criar pacotes novos: `domain.event` e
  `infrastructure.adapter.out.messaging`.

**Checkpoint:** projeto compila com a nova dependência; `./mvnw quarkus:dev`
sobe Dev Services de Kafka/Redpanda sem configuração manual.

---

## Fase 2 — Domínio (`plan.md` › Modelo de dados; Princípios I e III)

- [x] **T004** `[P]` Escrever testes unitários do value object `Quantidade`
  (nunca negativo, normalização de escala para 3 casas, `somar`/`subtrair`,
  `ehZero`) — antes da implementação.
- [x] **T005** Implementar value object `Quantidade` em `domain.model`
  satisfazendo T004.
- [x] **T006** `[P]` Criar `DomainEvent` (interface marcadora pura) em
  `domain.event` — primeiro uso real deste mecanismo no projeto.
- [x] **T007** `[P]` Criar `EstoqueBaixoAtingido` (record) em `domain.event`
  implementando `DomainEvent`.
- [x] **T008** Escrever testes unitários do agregado `SaldoEstoque` cobrindo
  as invariantes de `plan.md` (FR-004 a FR-008): entrada soma; entrada com
  quantidade zero/negativa rejeita; saída subtrai; saída maior que o saldo
  lança `SaldoInsuficienteException` sem alterar o saldo; saída com
  quantidade zero/negativa rejeita; saída que cruza o limiar (estava acima
  do mínimo, cai para igual/abaixo) adiciona exatamente 1
  `EstoqueBaixoAtingido` a `eventosPendentes`; saída subsequente que
  mantém o saldo já abaixo do mínimo **não** adiciona novo evento; uma
  entrada que devolve o saldo para acima do mínimo, seguida de nova saída
  que cruza de novo, adiciona um novo evento; `limparEventosPendentes()`
  esvazia a lista — antes da implementação.
- [x] **T009** Implementar agregado `SaldoEstoque` em `domain.model`
  satisfazendo T008, incluindo `iniciar(...)`, `reconstituir(...)`,
  `registrarEntrada(...)`, `registrarSaida(...)`, `eventosPendentes()`,
  `limparEventosPendentes()`.
- [x] **T010** `[P]` Criar exceções de domínio `SaldoInsuficienteException`,
  `SaldoEstoqueNaoEncontradoException`, `SaldoEstoqueJaExisteException` em
  `domain.model`.
- [x] **T011** Criar interface `SaldoEstoqueRepository` em
  `domain.repository` com `salvar(SaldoEstoque)`,
  `buscarPorProdutoId(UUID)`, `existePorProdutoId(UUID)`.

**Checkpoint:** `domain` (incluindo `domain.event`) compila e testa
isoladamente, sem nenhuma dependência de `infrastructure`.

---

## Fase 3 — Application: Command side (FR-001 a FR-009)

- [x] **T012** `[P]` Criar `IniciarSaldoEstoqueCommand` (record) em
  `application.command`.
- [x] **T013** Escrever teste de `IniciarSaldoEstoqueCommandHandler` com
  `SaldoEstoqueRepository` e `ProdutoRepository` mockados: caso feliz (US-1),
  produto inexistente rejeita (`ProdutoNaoEncontradoException`), saldo já
  existente rejeita (`SaldoEstoqueJaExisteException`) — antes da
  implementação.
- [x] **T014** Implementar `IniciarSaldoEstoqueCommandHandler` satisfazendo
  T013.
- [x] **T015** `[P]` Criar `RegistrarEntradaEstoqueCommand` (record) em
  `application.command`.
- [x] **T016** Escrever teste de `RegistrarEntradaEstoqueCommandHandler` com
  `SaldoEstoqueRepository` mockado: caso feliz (US-2), saldo inexistente
  rejeita — antes da implementação.
- [x] **T017** Implementar `RegistrarEntradaEstoqueCommandHandler`
  satisfazendo T016.
- [x] **T018** `[P]` Criar `RegistrarSaidaEstoqueCommand` (record) em
  `application.command`.
- [x] **T019** Escrever teste de `RegistrarSaidaEstoqueCommandHandler` com
  `SaldoEstoqueRepository` e `Event<EstoqueBaixoAtingido>` mockados: caso
  feliz sem cruzar o limiar (`.fire()` nunca chamado); caso que cruza o
  limiar (`.fire()` chamado exatamente 1 vez com o evento esperado); saldo
  insuficiente rejeita e `.fire()` nunca é chamado; saldo inexistente
  rejeita — antes da implementação.
- [x] **T020** Implementar `RegistrarSaidaEstoqueCommandHandler` satisfazendo
  T019, com `@Transactional` no método inteiro (`plan.md` › Decisões
  técnicas, desvio deliberado do padrão de 001).

**Checkpoint:** `application.command` compila e testa isoladamente com
mocks, sem subir o Quarkus.

---

## Fase 4 — Application: Query side (FR-010)

- [x] **T021** `[P]` Criar `SaldoEstoqueResult` (record) em
  `application.query`.
- [x] **T022** `[P]` Criar `BuscarSaldoEstoquePorProdutoIdQuery` e
  `BuscarSaldoEstoquePorSkuQuery` (records) em `application.query`.
- [x] **T023** Criar interface `SaldoEstoqueQueryRepository` em
  `application.query` com `buscarPorProdutoId(UUID)` e `buscarPorSku(String)`.
- [x] **T024** Implementar `BuscarSaldoEstoqueQueryHandler` em
  `application.query`, lançando `SaldoEstoqueNaoEncontradoException`
  quando não encontrado.

**Checkpoint:** `application.query` compila; nenhuma classe deste pacote
depende de `SaldoEstoqueRepository` (write) nem do agregado `SaldoEstoque`.

---

## Fase 5 — Persistência write (`plan.md` › Schema físico)

- [x] **T025** Criar migração `db/migration/V2__create_saldo_estoque_table.sql`
  com a tabela `saldo_estoque`, `CHECK` de não-negatividade e índice único
  `uq_saldo_estoque_produto_id`.
- [x] **T026** Criar `SaldoEstoqueJpaEntity` em
  `infrastructure.adapter.out.persistence.write`, mapeando 1:1 a tabela.
- [x] **T027** `[P]` Criar `SaldoEstoquePanacheRepository` (Panache técnico)
  no mesmo pacote.
- [x] **T028** Implementar `SaldoEstoqueRepositoryImpl` satisfazendo
  `SaldoEstoqueRepository`, convertendo agregado ↔ entidade JPA.
- [x] **T029** Escrever teste `@QuarkusTest` de `SaldoEstoqueRepositoryImpl`:
  round-trip salvar/buscar por produtoId; `existePorProdutoId` reflete
  estado persistido; violação do índice único ao salvar dois saldos para o
  mesmo `produtoId`.

**Checkpoint:** `mvnw test -Dtest=SaldoEstoqueRepositoryImplTest` passa
contra PostgreSQL real via Dev Services.

---

## Fase 6 — Persistência read (`plan.md` › Endpoints REST)

- [x] **T030** Implementar `SaldoEstoqueQueryRepositoryImpl` em
  `infrastructure.adapter.out.persistence.read`, projeção JPQL direto para
  `SaldoEstoqueResult` com `abaixoDoMinimo` calculado via `CASE WHEN`.
- [x] **T031** Escrever teste `@QuarkusTest` de
  `SaldoEstoqueQueryRepositoryImpl`: busca por produtoId existente/inexistente,
  busca por SKU existente/inexistente, `abaixoDoMinimo` correto nos dois
  sentidos (acima e igual/abaixo do mínimo).

**Checkpoint:** `mvnw test -Dtest=SaldoEstoqueQueryRepositoryImplTest` passa.

---

## Fase 7 — Web (`plan.md` › Endpoints REST)

- [x] **T032** `[P]` Criar DTOs `IniciarSaldoEstoqueRequest`,
  `RegistrarMovimentoEstoqueRequest`, `SaldoEstoqueResponse` em
  `infrastructure.adapter.in.web.dto`.
- [x] **T033** Implementar `SaldoEstoqueResource` (raiz `/api/produtos`,
  4 rotas) e `SaldoEstoquePorSkuResource` (raiz `/api/saldo-estoque`, busca
  por SKU) — divididas em duas classes porque misturar raízes JAX-RS de
  granularidade diferente numa única classe quebra o roteamento do
  RESTEasy Reactive em runtime (`plan.md` › Decisões técnicas), despachando
  para os handlers das Fases 3-4.
- [x] **T034** `[P]` Criar `ExceptionMapper`s para `SaldoInsuficienteException`
  (409), `SaldoEstoqueNaoEncontradoException` (404) e
  `SaldoEstoqueJaExisteException` (409) em
  `infrastructure.adapter.in.web.exception`. (`ProdutoNaoEncontradoException`
  já tem mapper de 001, reaproveitado sem alteração.)
- [x] **T035** Escrever teste `@QuarkusTest` + RestAssured de
  `SaldoEstoqueResource`: 1 caso feliz por rota (5) + 1 exemplo de `400`
  (quantidade negativa) + 1 de `404` (saldo inexistente) + 1 de `409`
  (saldo já existe **ou** saldo insuficiente).

**Checkpoint:** `mvnw test -Dtest=SaldoEstoqueResourceTest` passa.

---

## Fase 8 — Infraestrutura de mensageria (US-5, FR-008/FR-009)

- [x] **T036** `[P]` Criar `EstoqueBaixoAtingidoMensagem` (record) em
  `infrastructure.adapter.out.messaging`, com `de(EstoqueBaixoAtingido)`.
- [x] **T037** Implementar `EstoqueBaixoAtingidoKafkaPublisher` observando
  `EstoqueBaixoAtingido` com `@Observes(during = TransactionPhase.AFTER_SUCCESS)`
  e publicando via `Emitter<EstoqueBaixoAtingidoMensagem>` no canal
  `estoque-baixo-atingido`.
- [x] **T038** Configurar o canal outgoing em `application.properties`
  (`mp.messaging.outgoing.estoque-baixo-atingido.*`), override `%prod` via
  `KAFKA_BOOTSTRAP_SERVERS`, override `%test` para conector
  `smallrye-in-memory`.
- [x] **T039** Escrever teste `@QuarkusTest` do publisher usando
  `InMemoryConnector`, cobrindo especificamente o requisito central desta
  feature: uma saída que cruza o limiar dentro de uma transação que
  **comita** produz exatamente 1 mensagem no sink in-memory; uma operação
  cujo evento seria disparado mas a transação sofre **rollback** não produz
  nenhuma mensagem (prova a semântica de `AFTER_SUCCESS`, FR-009).

**Checkpoint:** `mvnw test -Dtest=EstoqueBaixoAtingidoKafkaPublisherTest`
passa sem depender de um broker Kafka real.

---

## Fase 9 — Validação final

- [x] **T040** Rodar `./mvnw test` completo — toda a suíte (domínio puro,
  handlers com mocks, `@QuarkusTest` de persistência/web/messaging) deve
  passar.
- [x] **T041** Atualizar `memory/tech-stack.md` com a seção "Mensageria —
  publicação assíncrona" e a referência ao primeiro uso real de Domain
  Events.
- [x] **T042** Atualizar `memory/constitution.md` (PATCH 2.1.1 → 2.1.2):
  adicionar `adapter/out/messaging/` ao diagrama de convenção de pacotes.
- [x] **T043** Revisar rastreabilidade: cada FR-001 a FR-011 de `spec.md`
  tem pelo menos uma task nesta lista e um teste correspondente.

**Checkpoint:** feature 002 completa, testada e documentada; roadmap
(`memory` do usuário) segue para a feature 003 (serviço consumidor
dedicado) e as etapas de infraestrutura cloud (AWS, depois OpenShift) em
sessões futuras.
