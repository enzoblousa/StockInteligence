# Implementation Plan: Controle de Saldo de Estoque e Alerta de Estoque Baixo

**Feature ID:** 002-alerta-estoque-baixo
**Spec:** `specs/002-alerta-estoque-baixo/spec.md`
**Constitution:** `memory/constitution.md` v2.1.1
**Tech stack base:** `memory/tech-stack.md`

## Resumo técnico

Um novo agregado `SaldoEstoque` concentra as invariantes de negócio (saldo
nunca negativo, sinalização de estoque baixo). O lado de escrita (US-1,
US-2, US-3) passa por `Command` → `CommandHandler` → agregado →
`SaldoEstoqueRepository`. O lado de leitura (US-4) passa por `Query` →
`QueryHandler` → projeção direta via JPQL, sem reconstruir o agregado,
retornando `SaldoEstoqueResult`.

Esta feature também introduz o **primeiro Domain Event real do projeto**:
`EstoqueBaixoAtingido` é capturado de forma pura pelo agregado, drenado e
disparado in-process via CDI `Event<T>` pelo `CommandHandler` de saída, e
publicado no Kafka por um adapter de infraestrutura que observa apenas após
o commit da transação.

## Constitution Check

| Princípio | Conformidade |
|---|---|
| I. DDD tático | ✅ `SaldoEstoque` é aggregate root; `Quantidade` é value object novo; `EstoqueBaixoAtingido` é o primeiro uso real de Domain Event no projeto, usando o pacote `domain/event/` já previsto genericamente pela constitution. |
| II. CQRS | ✅ Commands/Queries em pacotes separados; `SaldoEstoqueQueryRepository` (application.query) nunca reconstrói o agregado nem usa `SaldoEstoqueRepository` (write). |
| III. Pureza do domínio | ✅ `SaldoEstoque`, `Quantidade`, `EstoqueBaixoAtingido`, `DomainEvent` são Java puro — zero anotação Quarkus/CDI/JPA. Conversão para `SaldoEstoqueJpaEntity` fica no adapter de persistência; conversão do evento de domínio para `EstoqueBaixoAtingidoMensagem` (Kafka) fica em `infrastructure/adapter/out/messaging`, nunca no domínio. |
| IV. Contratos explícitos | ✅ `SaldoEstoqueRepository` (domain.repository) e `SaldoEstoqueQueryRepository` (application.query) são interfaces; `SaldoEstoqueResource` só conhece Commands/Queries. |
| V. Testabilidade | ✅ `CommandHandler`s testados com repositório(s) e `Event<EstoqueBaixoAtingido>` mockados (sem Quarkus); agregado/VO com testes unitários puros, incluindo a regra de transição; `QueryHandler`/repositórios via `@QuarkusTest`; publisher Kafka testado com conector `smallrye-in-memory`, provando a semântica de `TransactionPhase.AFTER_SUCCESS`. |
| VI. Simplicidade/YAGNI | ✅ Mensageria externa só entra agora porque esta spec concreta (US-5/FR-009) exige publicação de um alerta para consumo fora deste bounded context — não é especulativo, é exatamente o gatilho que `memory/tech-stack.md` já previa. Sem outbox transacional completo (ver Decisões técnicas). Sem edição de quantidade mínima nem multi-depósito (fora de escopo da spec). |

Nenhum desvio a registrar em Complexity Tracking além do documentado nas
Decisões técnicas abaixo.

## Decisões técnicas

- **`Quantidade` (VO)** — `BigDecimal`, escala 3, nunca negativo. Escala 3
  (não 2, como `Preco`) porque `UnidadeMedida` de 001 já inclui unidades
  fracionárias (KG, L, ML) — quantidade de estoque não pode ser tratada
  como inteiro.
- **`SKU` reaproveitado** do domínio de 001 (já é VO puro) dentro de
  `SaldoEstoque`. O valor é denormalizado na tabela `saldo_estoque` (coluna
  `sku`) para permitir consulta por SKU e popular o evento sem join.
- **Mecanismo de Domain Event (novo padrão do projeto, primeiro uso real):**
  `domain/event/DomainEvent.java` é uma interface marcadora pura;
  `domain/event/EstoqueBaixoAtingido.java` é um `record` implementando-a.
  `SaldoEstoque` mantém uma lista interna `eventosPendentes` (pura, sem
  framework); `registrarSaida` adiciona o evento **apenas na transição**
  (compara a quantidade **antes** e **depois** da subtração contra a
  mínima — decisão de negócio confirmada com o usuário, FR-008); expõe
  `eventosPendentes()` (cópia imutável) e `limparEventosPendentes()`, que o
  `CommandHandler` usa para drenar após persistir.
- **Semântica "só na transição" (FR-008):** `registrarSaida` calcula
  `estavaAcimaDoMinimo` **antes** de subtrair; só adiciona o evento se
  `estavaAcimaDoMinimo && quantidadeAtual <= quantidadeMinima` depois da
  subtração. Uma entrada que devolve o saldo para acima do mínimo rearma a
  próxima transição — não há nenhum estado adicional persistido para isso,
  a comparação a cada saída já é suficiente.
- **`RegistrarSaidaEstoqueCommandHandler` é `@Transactional` no método
  inteiro** — desvio deliberado do padrão estreito de 001 (lá,
  `@Transactional` fica só no repositório). Necessário para que o CDI
  `Event<EstoqueBaixoAtingido>.fire(...)` seja disparado com uma transação
  JTA ainda ativa: só assim `TransactionPhase.AFTER_SUCCESS` no observador
  tem efeito real — sem isso, o evento seria entregue imediatamente, fora
  de qualquer transação, tornando a fase decorativa.
  `RegistrarEntradaEstoqueCommandHandler` (sem evento) mantém o padrão
  estreito de 001.
- **Separação evento de domínio vs. mensagem Kafka:** `EstoqueBaixoAtingido`
  (domínio, puro) é convertido para `EstoqueBaixoAtingidoMensagem` (infra,
  `infrastructure/adapter/out/messaging`) antes de ir para o `Emitter`. O
  domínio nunca conhece o formato de serialização Kafka (Princípio III).
- **Limitação conhecida — não é Transactional Outbox completo:** entre o
  commit da transação PostgreSQL e a chamada `emitter.send(...)`, não há
  garantia de dual-write; se o processo cair nesse intervalo, o alerta é
  perdido silenciosamente (sem tabela de outbox, sem poller). Aceitável
  para o MVP desta spec — perda pontual de um alerta não é catastrófica, o
  saldo consultado continua refletindo a realidade. Evolução futura, se a
  confiabilidade de entrega virar requisito: outbox table + poller ou CDC
  (ex. Debezium) — não implementado aqui (YAGNI, Princípio VI).
- **Nome do tópico:** `estoque.baixo-atingido` (padrão `contexto.evento`).
- **`IniciarSaldoEstoqueCommand`** existe porque a quantidade mínima é um
  dado de negócio específico por produto e precisa ser definida antes de
  qualquer entrada/saída — sem ele, "entrada"/"saída" não teriam contra o
  que operar (FR-001).
- **Índice único em `produto_id`:** materializa fisicamente a premissa "sem
  multi-depósito, saldo global por produto" (FR-011).
- **Duas classes JAX-RS em vez de uma** (`SaldoEstoqueResource` na raiz
  `/api/produtos`, igual a `ProdutoResource`; `SaldoEstoquePorSkuResource` na
  raiz `/api/saldo-estoque`) — descoberto durante a implementação: misturar,
  em uma única classe, uma raiz curta (`/api`) com sub-rotas profundas
  (`/produtos/{id}/saldo-estoque`) ao lado de outra classe já registrada com
  raiz mais específica (`/api/produtos`) faz o roteador do RESTEasy Reactive
  falhar em casar a rota em runtime — mesmo listando-a corretamente na
  página de diagnóstico 404. Confirmado via teste manual (`curl` contra
  `quarkus:dev`) antes de ajustar `SaldoEstoqueResourceTest`. Lição: raízes
  JAX-RS de classes diferentes que compartilham prefixo devem ter a mesma
  granularidade.
- **Dependência de teste `smallrye-reactive-messaging-in-memory`** troca o
  conector `smallrye-kafka` por `smallrye-in-memory` em `%test`, permitindo
  testar a semântica de `AFTER_SUCCESS` sem depender de um broker Kafka
  real durante a suíte de testes.

## Estrutura de pacotes desta feature

```
domain/
  model/
    SaldoEstoque.java                    # aggregate root
    Quantidade.java                       # value object
    SaldoInsuficienteException.java
    SaldoEstoqueNaoEncontradoException.java
    SaldoEstoqueJaExisteException.java
  event/
    DomainEvent.java                      # marker interface (novo pacote)
    EstoqueBaixoAtingido.java
  repository/
    SaldoEstoqueRepository.java

application/
  command/
    IniciarSaldoEstoqueCommand.java / CommandHandler
    RegistrarEntradaEstoqueCommand.java / CommandHandler
    RegistrarSaidaEstoqueCommand.java / CommandHandler
  query/
    SaldoEstoqueResult.java
    BuscarSaldoEstoquePorProdutoIdQuery.java
    BuscarSaldoEstoquePorSkuQuery.java
    SaldoEstoqueQueryRepository.java
    BuscarSaldoEstoqueQueryHandler.java

infrastructure/
  adapter/in/web/
    SaldoEstoqueResource.java           # raiz "/api/produtos" (igual a ProdutoResource)
    SaldoEstoquePorSkuResource.java      # raiz "/api/saldo-estoque" (busca por SKU)
    dto/ IniciarSaldoEstoqueRequest.java, RegistrarMovimentoEstoqueRequest.java, SaldoEstoqueResponse.java
    exception/ SaldoInsuficienteExceptionMapper.java, SaldoEstoqueNaoEncontradoExceptionMapper.java, SaldoEstoqueJaExisteExceptionMapper.java
  adapter/out/persistence/write/
    SaldoEstoqueJpaEntity.java, SaldoEstoquePanacheRepository.java, SaldoEstoqueRepositoryImpl.java
  adapter/out/persistence/read/
    SaldoEstoqueQueryRepositoryImpl.java
  adapter/out/messaging/                  # pacote novo, não existia em 001
    EstoqueBaixoAtingidoMensagem.java
    EstoqueBaixoAtingidoKafkaPublisher.java
```

## Modelo de dados

### Agregado `SaldoEstoque`

| Campo | Tipo | Regra |
|---|---|---|
| `id` | `UUID` | Gerado na criação, imutável. |
| `produtoId` | `UUID` | Referencia `Produto` (001); imutável. |
| `sku` | `SKU` (VO) | Denormalizado a partir do produto no momento de `iniciar`. |
| `quantidadeAtual` | `Quantidade` (VO) | Nunca negativo; soma em entrada, subtrai em saída. |
| `quantidadeMinima` | `Quantidade` (VO) | Definido em `iniciar`, imutável nesta feature. |

**Invariantes:**
1. Saída nunca deixa `quantidadeAtual` negativo — lança
   `SaldoInsuficienteException` antes de mutar o estado.
2. Entrada e saída exigem quantidade > 0 (`IllegalArgumentException` caso
   contrário).
3. `EstoqueBaixoAtingido` só é sinalizado na transição
   acima-do-mínimo → igual-ou-abaixo-do-mínimo, nunca em saídas
   subsequentes que apenas mantêm o saldo já baixo.

### Schema físico (PostgreSQL) — `saldo_estoque`

```sql
CREATE TABLE saldo_estoque (
    id                 UUID PRIMARY KEY,
    produto_id         UUID            NOT NULL REFERENCES produto(id),
    sku                VARCHAR(50)     NOT NULL,
    quantidade         NUMERIC(14,3)   NOT NULL CHECK (quantidade >= 0),
    quantidade_minima  NUMERIC(14,3)   NOT NULL CHECK (quantidade_minima >= 0),
    atualizado_em      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_saldo_estoque_produto_id ON saldo_estoque (produto_id);
CREATE INDEX ix_saldo_estoque_sku ON saldo_estoque (sku);
```

`SaldoEstoqueJpaEntity` mapeia 1:1 para `saldo_estoque`;
`SaldoEstoqueRepositoryImpl` converte `SaldoEstoque` ↔
`SaldoEstoqueJpaEntity` (o agregado nunca é anotado com JPA).
`SaldoEstoqueQueryRepositoryImpl` lê a mesma tabela via projeção JPQL direto
para `SaldoEstoqueResult`, calculando `abaixoDoMinimo` na própria query.

## Endpoints REST (mapeamento User Story → endpoint)

| User Story | Método | Rota | Command/Query |
|---|---|---|---|
| US-1 | `POST` | `/api/produtos/{produtoId}/saldo-estoque` | `IniciarSaldoEstoqueCommand` |
| US-2 | `POST` | `/api/produtos/{produtoId}/saldo-estoque/entradas` | `RegistrarEntradaEstoqueCommand` |
| US-3 | `POST` | `/api/produtos/{produtoId}/saldo-estoque/saidas` | `RegistrarSaidaEstoqueCommand` |
| US-4 | `GET` | `/api/produtos/{produtoId}/saldo-estoque` | `BuscarSaldoEstoquePorProdutoIdQuery` |
| US-4 | `GET` | `/api/saldo-estoque/sku/{sku}` | `BuscarSaldoEstoquePorSkuQuery` |

Erros: `400` dado inválido, `404` produto/saldo não encontrado, `409` saldo
já existe / saldo insuficiente.

## Configuração de mensageria (`application.properties`)

```properties
mp.messaging.outgoing.estoque-baixo-atingido.connector=smallrye-kafka
mp.messaging.outgoing.estoque-baixo-atingido.topic=estoque.baixo-atingido
mp.messaging.outgoing.estoque-baixo-atingido.value.serializer=io.quarkus.kafka.client.serialization.ObjectMapperSerializer

# Produção: mesmo padrão do datasource (variável de ambiente).
%prod.kafka.bootstrap.servers=${KAFKA_BOOTSTRAP_SERVERS}

# Testes: conector in-memory, sem depender de broker real.
%test.mp.messaging.outgoing.estoque-baixo-atingido.connector=smallrye-in-memory
```

Em dev, o Quarkus Dev Services sobe um broker Kafka/Redpanda automaticamente
ao detectar `quarkus-messaging-kafka` no classpath sem `bootstrap.servers`
configurado — mesmo padrão zero-config já usado pelo PostgreSQL.

## Quickstart

> Para o passo a passo completo de como ver e testar a mensageria (testes
> automatizados, fluxo end-to-end via HTTP e inspeção da mensagem crua no
> tópico Kafka), ver `testando-mensageria.md`.

```bash
./mvnw quarkus:dev
```

Dev Services sobe PostgreSQL **e** um broker Kafka/Redpanda automaticamente;
Flyway aplica a migração `V2__create_saldo_estoque_table.sql` na subida.
Swagger UI em `http://localhost:8080/q/swagger-ui`.

```bash
# 1. Cadastrar um produto (001)
curl -X POST http://localhost:8080/api/produtos \
  -H "Content-Type: application/json" \
  -d '{"sku":"BEB-001","nome":"Refrigerante 2L","categoria":"BEBIDAS","unidadeMedida":"UN","precoCusto":4.50,"precoVenda":7.90}'

# 2. Definir saldo inicial (use o id retornado acima)
curl -X POST http://localhost:8080/api/produtos/{id}/saldo-estoque \
  -H "Content-Type: application/json" \
  -d '{"quantidadeInicial": 10, "quantidadeMinima": 5}'

# 3. Registrar saídas até cruzar o mínimo — a partir da saída que leva o
#    saldo para <= 5, o publisher deve emitir a mensagem só após a
#    resposta HTTP de sucesso (conferir no log/console em dev).
curl -X POST http://localhost:8080/api/produtos/{id}/saldo-estoque/saidas \
  -H "Content-Type: application/json" -d '{"quantidade": 6}'
```

```bash
./mvnw test
```

Testes de domínio/handlers rodam puros (JUnit 5 + Mockito, sem subir o
Quarkus); testes de `SaldoEstoqueResource`, repositórios e do publisher
Kafka são `@QuarkusTest` (banco real via Dev Services; Kafka via conector
in-memory em `%test`).

## Fase seguinte

`tasks.md` — gerado a partir deste plano.

## Complexity Tracking

Nenhum desvio da constitution nesta feature além do já documentado em
Decisões técnicas (mensageria externa introduzida por esta spec concreta,
conforme Princípio VI já previa; limitação de não ser um outbox
transacional completo, aceita como MVP).
