# Constitution — StockInteligence

**Versão:** 2.2.0
**Ratificada em:** 2026-08-20
**Última alteração:** 2026-08-20

## Propósito

Este documento define os princípios não-negociáveis do projeto StockInteligence
(sistema de gestão de estoque/inventário). Toda `spec.md`, `plan.md` e `tasks.md`
gerada a partir deste ponto **deve** estar em conformidade com estes princípios.
Divergências precisam ser justificadas explicitamente na seção "Complexity
Tracking" do `plan.md` correspondente ou disparar uma emenda a esta constitution.

> **Nota de versão:** a v2.0.0 substitui a arquitetura hexagonal (ports &
> adapters) por **Domain-Driven Design (tático) + CQRS sem Event Sourcing**,
> com write model e read model compartilhando o mesmo banco de dados.

---

## Princípios Centrais

### I. Domain-Driven Design (tático) como base do domínio

O domínio é modelado com as ferramentas táticas de DDD:

- **Agregados** — fronteira de consistência transacional; toda alteração de
  estado passa pela raiz do agregado (*aggregate root*), nunca diretamente por
  uma entidade interna.
- **Entidades** — identidade própria, ciclo de vida.
- **Value Objects** — imutáveis, sem identidade, validados na construção
  (ex.: `SKU`, `Quantidade`, `Endereco`).
- **Domain Events** — fatos relevantes de negócio já ocorridos (ex.:
  `EstoqueBaixado`, `ProdutoCadastrado`), usados para desacoplar efeitos
  colaterais (notificações, atualização de projeções) do fluxo principal do
  agregado. Não há event store — eventos são publicados e consumidos
  in-process ou via extensão de mensageria do Quarkus, quando necessário.
- **Repositórios** — interfaces definidas no domínio, uma por agregado,
  expondo apenas as operações que o modelo de escrita precisa (nunca CRUD
  genérico). Implementação concreta fica na infraestrutura.
- **Domain Services** — usados apenas quando uma regra não pertence
  naturalmente a nenhum agregado específico.

**Justificativa:** DDD tático mantém a regra de negócio centralizada e
explícita, evitando um "anemic domain model" e dando vocabulário comum
(ubiquitous language) entre spec, código e negócio.

### II. CQRS — separação entre Comandos e Queries

O lado de escrita e o lado de leitura são explicitamente separados no código,
ainda que compartilhem o mesmo banco de dados:

- **Command side** — um `Command` (DTO de intenção) é tratado por um
  `CommandHandler`, que carrega o agregado via repositório, executa a regra de
  negócio e persiste o novo estado. É o único caminho que passa pelo modelo de
  domínio (Princípio I).
- **Query side** — uma `Query` é tratada por um `QueryHandler` que **não**
  passa pelos agregados de domínio: lê diretamente via projeção/DTO otimizada
  para leitura (ex.: Panache projection, JPQL/SQL nativo), retornando um
  `Result`/DTO plano. Read model não tem regra de negócio nem invariantes.
- Controllers (adapters de entrada) apenas traduzem request → Command/Query e
  despacham para o handler correspondente via CDI — sem lógica de negócio no
  controller.

**Justificativa:** evita que consultas complexas forcem o modelo de domínio a
carregar dados que não precisa (ou a virar um modelo genérico demais), e deixa
claro, por design, onde mora regra de negócio (sempre no command side).

### III. Pureza do domínio (sem vazamento de framework)

Agregados, entidades, value objects, domain events e interfaces de repositório
não usam anotações do Quarkus, JPA/Hibernate, JAX-RS/RESTEasy ou qualquer
biblioteca de infraestrutura. O domínio é Java puro (+ eventualmente uma
biblioteca de validação estrutural, se justificada na seção de decisões
técnicas do `plan.md`). Mapeamento entre agregado e entidade de persistência,
quando necessário, é
responsabilidade da implementação do repositório, não do domínio.

**Justificativa:** garante que o domínio seja testável isoladamente (testes
unitários puros, sem subir contexto Quarkus) e que a regra de negócio sobreviva
a trocas de framework ou de mecanismo de persistência.

### IV. Contratos explícitos (Commands, Queries e Repositórios)

Toda comunicação entre camadas acontece através de tipos e interfaces
explícitos, nunca por acesso direto a implementação concreta fora da sua
própria camada:

- Um `Command`/`Query` é o único jeito de acionar um caso de uso a partir de
  um adapter de entrada.
- Um repositório de agregado é o único jeito de um `CommandHandler` persistir
  ou carregar estado.
- Nenhum adapter de infraestrutura é referenciado por tipo concreto fora de
  `infrastructure`; CDI resolve a implementação em tempo de execução.

**Justificativa:** mantém baixo acoplamento entre camadas e torna trivial
substituir um repositório ou handler por um dublê (mock/fake) em teste.

### V. Testabilidade como requisito de design

- `CommandHandler`s são testados isolando o repositório via mock/fake — sem
  subir o Quarkus.
- Agregados, entidades e value objects são testados com testes unitários
  puros.
- `QueryHandler`s e repositórios (implementações) são cobertos por testes de
  integração (`@QuarkusTest`), validando consultas contra um banco real
  (Testcontainers ou Dev Services do Quarkus).
- Toda `spec.md` deve conter critérios de aceite verificáveis; toda
  `tasks.md` deve gerar pelo menos um teste correspondente por
  Command/Query relevante antes ou junto da implementação.
- Cada regra de negócio tem **uma única camada dona** da sua cobertura
  exaustiva (o domínio, na grande maioria dos casos); as demais camadas
  testam apenas o que é exclusivo delas, nunca reafirmam o mesmo cenário —
  ver `memory/testing-strategy.md` para a divisão completa de
  responsabilidade por camada.

**Justificativa:** design testável por construção é a evidência mais direta de
que a separação DDD/CQRS está sendo respeitada de fato, não só na intenção.
Cobertura duplicada entre camadas eleva o custo de mudança sem reduzir risco
proporcionalmente.

### VI. Simplicidade e YAGNI

Começar com um único módulo Maven, um único bounded context (Estoque) e um
único banco de dados compartilhado entre command e query side. Event Sourcing,
bancos de leitura separados, bus de mensageria externo ou múltiplos bounded
contexts só são introduzidos quando uma `spec.md` concreta os exigir — nunca
especulativamente. Toda complexidade adicional deve ser justificada por
escrito no `plan.md` da feature que a motivou.

**Justificativa:** evita over-engineering; DDD + CQRS sem event sourcing já
traz separação suficiente para o tamanho inicial do projeto.

---

## Stack Tecnológica (decisões fixas do projeto)

| Item | Decisão | Observação |
|---|---|---|
| Linguagem | Java 21 (LTS) | LTS mais recente amplamente suportado pelo Quarkus no momento; revisitar se houver motivo concreto em `research.md`. |
| Framework | Quarkus | Requisito do projeto. |
| Build | Maven | Definido pelo usuário. |
| Padrão arquitetural | DDD tático + CQRS, sem Event Sourcing | Write model (agregados) e read model (DTO/projeção) no mesmo banco de dados. |
| Testes de domínio/command handlers | JUnit 5 + AssertJ + Mockito | Testes puros, sem contexto Quarkus. |
| Testes de query handlers/repositórios | JUnit 5 + `@QuarkusTest` (+ RestAssured para REST, Testcontainers/Dev Services para banco) | Testes de integração contra banco real. |
| Enforcement arquitetural | ArchUnit (adiado) | Não obrigatório nas primeiras features — adotar quando o projeto crescer o suficiente (mais features, mais pessoas) para justificar o custo de manutenção do teste. Até lá, as regras de dependência entre camadas são garantidas por revisão manual. |
| Persistência | PostgreSQL + Hibernate ORM com Panache (blocking) | Mesmo banco para write e read model. Detalhamento das extensões Quarkus em `memory/tech-stack.md`. |

Convenção de pacotes (base, sob `com.stockinteligence.<contexto>`):

```
domain/
  model/              # agregados, entidades, value objects
  event/              # domain events
  repository/         # interfaces de repositório (write model)
application/
  command/
    <UseCase>Command.java
    <UseCase>CommandHandler.java
  query/
    <UseCase>Query.java
    <UseCase>QueryHandler.java
    <UseCase>Result.java       # DTO de leitura
infrastructure/
  adapter/in/web/       # controllers REST, DTOs de request/response
  adapter/out/persistence/
    write/               # implementações de repositório (agregados)
    read/                # implementações de query (projeções/DTO)
  config/                # configuração específica de framework
```

---

## Frontend

O frontend é uma SPA separada do backend (projeto Node/Vite próprio, fora do
módulo Maven), que consome a API REST documentada em `/q/openapi`. Não segue
a arquitetura DDD/CQRS dos Princípios I-VI — é tratado com convenções
próprias, proporcionais ao seu escopo (painel CRUD sobre a API já pronta).

### Stack

| Item | Decisão | Observação |
|---|---|---|
| Linguagem | JavaScript (ES2022+) | Sem TypeScript por decisão deliberada — nível júnior, foco em fundamentos. Evolução natural para TS pode ser revisitada depois do MVP. |
| Build | Vite | Padrão de fato para SPA React hoje; dev server rápido. |
| Framework UI | React 18 | Stack mais demandada em vagas júnior de frontend/fullstack. |
| Roteamento | React Router | Rotas simples: listagem, novo produto, editar produto. |
| HTTP client | Axios | API mais simples que `fetch` puro para tratamento de erro/interceptors. |
| Estilo | Bootstrap / React-Bootstrap | Componentes prontos (tabela, formulário, alerta) sem exigir CSS avançado. |
| Gerenciamento de estado | Nenhuma lib — `useState`/`useEffect` nativos | Sem Redux/Zustand/TanStack Query enquanto o escopo não justificar (mesmo espírito do Princípio VI). |

### Convenções

- Um único client Axios centralizado (`src/api/client.js`), com `baseURL`
  vinda de variável de ambiente (`VITE_API_BASE_URL`) — nunca hardcoded em
  componentes.
- Toda chamada à API passa por uma camada de serviço
  (`src/api/produtoService.js`); nenhum componente chama Axios diretamente —
  mesmo raciocínio do backend de não misturar infraestrutura com
  apresentação.
- Erros de negócio do backend (`400`/`404`/`409`, corpo `{ mensagem }`) são
  capturados nessa camada de serviço e propagados de forma uniforme para a
  UI exibir via um componente de alerta reutilizável — nunca `try/catch`
  duplicado em cada componente.
- Componentes organizados por tela (`src/pages/`) + componentes
  reutilizáveis (`src/components/`) — sem abstração prematura de design
  system.

---

## Fluxo de Desenvolvimento (Spec-Driven Development)

1. **`constitution.md`** (este documento) — princípios do projeto, alterado
   raramente e apenas por emenda explícita.
2. **`spec.md`** por feature — o quê e para quem, em linguagem de negócio,
   sem detalhes técnicos. Ambiguidades marcadas como `[NEEDS CLARIFICATION]`.
3. **`plan.md`** por feature — tradução técnica da spec respeitando esta
   constitution: quais agregados, quais Commands/Queries, quais
   repositórios, decisões técnicas e modelo de dados. Um único documento por
   feature (sem `research.md`/`data-model.md`/`contracts/` separados) — o
   contrato OpenAPI é gerado automaticamente pelo `smallrye-openapi` a partir
   do código, não escrito à mão.
4. **`tasks.md`** por feature — tarefas atômicas, ordenadas por dependência
   (domínio → command/query handlers → infraestrutura → testes de
   integração), cada uma rastreável até uma seção da spec ou do plan.
5. Implementação segue as tasks em ordem; nenhuma task de infraestrutura é
   iniciada antes das tasks de domínio/aplicação que ela depende.

Todo `plan.md` deve conter um "Constitution Check" explícito, confirmando
conformidade com os seis princípios acima antes de detalhar o design técnico.

---

## Governança

- Esta constitution tem precedência sobre qualquer prática ad hoc adotada
  durante a implementação.
- Alterações exigem: (a) motivo registrado, (b) atualização do número de
  versão seguindo semver — MAJOR para remoção/redefinição incompatível de
  princípio, MINOR para novo princípio ou seção material, PATCH para
  clarificação/redação — e (c) atualização da data de "Última alteração".
- `plan.md` que não passar no "Constitution Check" deve justificar o desvio em
  "Complexity Tracking" ou ser revisado até estar em conformidade.

## Histórico de versões

- **2.2.0** (2026-08-20) — adicionada seção Frontend: SPA React 18 + Vite +
  JavaScript + Axios + React Router + Bootstrap, com convenções próprias
  (client HTTP centralizado, camada de serviço, tratamento uniforme de
  erro). Não segue os Princípios I-VI (exclusivos do backend DDD/CQRS).
- **2.1.1** (2026-08-20) — Princípio V detalhado com regra de "uma camada
  dona por regra de negócio", evitando cobertura duplicada entre domínio,
  handler e testes de integração. Divisão de responsabilidade em
  `memory/testing-strategy.md`.
- **2.1.0** (2026-08-20) — simplificações de processo: ArchUnit adiado (não
  obrigatório nas primeiras features); documentação por feature consolidada
  em `spec.md` + `plan.md` + `tasks.md` (sem `research.md`/`data-model.md`/
  `contracts/` separados); contrato OpenAPI passa a ser gerado pelo código
  via `smallrye-openapi`, não escrito à mão.
- **2.0.1** (2026-08-20) — decisão de persistência resolvida: PostgreSQL +
  Hibernate ORM com Panache (blocking). Extensões Quarkus detalhadas em
  `memory/tech-stack.md`.
- **2.0.0** (2026-08-20) — substituída arquitetura hexagonal por DDD tático +
  CQRS sem Event Sourcing, write/read model no mesmo banco.
- **1.0.0** (2026-08-20) — versão inicial, baseada em arquitetura hexagonal
  (ports & adapters).
