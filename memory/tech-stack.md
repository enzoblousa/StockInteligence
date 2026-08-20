# Tech Stack — Extensões Quarkus

**Complementa:** `memory/constitution.md` (v2.0.1)
**Decisões-base:** Java 21, Maven, PostgreSQL, Hibernate ORM com Panache (blocking).

Este documento lista as extensões Quarkus e dependências de teste do projeto,
organizadas por onde entram na arquitetura DDD + CQRS definida na
constitution. Extensões são adicionadas apenas quando uma necessidade real
existe (Princípio VI — Simplicidade e YAGNI); qualquer extensão fora desta
lista deve ser justificada em `research.md` da feature que a motivou.

---

## Adapter de entrada — Web (`infrastructure/adapter/in/web`)

| Extensão | Artifact | Papel |
|---|---|---|
| Quarkus REST | `io.quarkus:quarkus-rest` | Controllers REST que traduzem request → Command/Query e despacham para o handler. |
| Quarkus REST Jackson | `io.quarkus:quarkus-rest-jackson` | Serialização/deserialização JSON. |
| Hibernate Validator | `io.quarkus:quarkus-hibernate-validator` | Bean Validation nos DTOs de request, na borda — não substitui a validação de invariantes dentro do agregado. |
| SmallRye OpenAPI | `io.quarkus:quarkus-smallrye-openapi` | Gera o contrato OpenAPI/Swagger UI a partir dos endpoints; alimenta o artefato `contracts/` do `plan.md` de cada feature. |

## Persistência — Write model (`infrastructure/adapter/out/persistence/write`)

| Extensão | Artifact | Papel |
|---|---|---|
| Hibernate ORM com Panache | `io.quarkus:quarkus-hibernate-orm-panache` | Implementação dos repositórios de agregado definidos no domínio. Entidades JPA de persistência ficam aqui, isoladas — nunca são o agregado de domínio (Princípio III). |
| Driver PostgreSQL | `io.quarkus:quarkus-jdbc-postgresql` | Driver JDBC + Dev Services (sobe um Postgres via container automaticamente em dev/test, sem configuração manual). |
| Flyway | `io.quarkus:quarkus-flyway` | Migrações de schema versionadas (`src/main/resources/db/migration`), única fonte de verdade do schema. |

## Persistência — Read model (`infrastructure/adapter/out/persistence/read`)

| Extensão | Artifact | Papel |
|---|---|---|
| Hibernate ORM com Panache | *(mesma extensão acima)* | `QueryHandler`s leem via projeções JPQL/`EntityManager` nativo, retornando DTOs planos — sem passar pelo agregado. Reaproveita a mesma extensão do write side; não é uma dependência nova. |

> Se, ao especificar uma feature concreta, uma consulta exigir SQL muito
> elaborado além do que JPQL/projeções resolvem bem, avaliar `jOOQ`
> (`io.quarkiverse.jooq:quarkus-jooq`, extensão community) — decisão a
> registrar no `research.md` daquela feature, não adotada preventivamente.

## Domain Events (in-process)

Nenhuma extensão adicional: eventos de domínio são publicados/observados via
CDI (`jakarta.enterprise.event.Event<T>` / `@Observes`), já disponível pela
extensão núcleo `quarkus-arc` (presente em todo projeto Quarkus, não listada
à parte). Mensageria externa (Kafka, RabbitMQ via `quarkus-messaging-*`) só
entra se uma spec futura exigir integração assíncrona entre bounded
contexts — não faz parte do MVP.

## Observabilidade

| Extensão | Artifact | Papel |
|---|---|---|
| SmallRye Health | `io.quarkus:quarkus-smallrye-health` | Endpoints `/q/health/live` e `/q/health/ready`. |

> Métricas (`quarkus-micrometer` + `quarkus-micrometer-registry-prometheus`)
> ficam como candidatas para quando houver necessidade real de observar o
> sistema em produção — não incluídas no bootstrap inicial (YAGNI).

## Testes

| Dependência | Artifact | Papel |
|---|---|---|
| Quarkus JUnit5 | `io.quarkus:quarkus-junit5` | Integração `@QuarkusTest` para testes de infraestrutura (adapters). |
| REST Assured | `io.rest-assured:rest-assured` | Testes de integração dos endpoints REST. |
| AssertJ | `org.assertj:assertj-core` | Assertions fluentes, usadas tanto em testes puros (domínio/handlers) quanto em `@QuarkusTest`. |
| Mockito | `org.mockito:mockito-core` | Mock de repositórios nos testes puros de `CommandHandler`. |
| ArchUnit *(adiado)* | `com.tngtech.archunit:archunit-junit5` | Não adicionado nas primeiras features (ver `memory/constitution.md` v2.1.0) — entra quando o projeto crescer o suficiente pra justificar o custo de manutenção do teste. |

Dev Services do Quarkus (via `quarkus-jdbc-postgresql`) sobe um PostgreSQL em
container automaticamente para `@QuarkusTest` e `mvn quarkus:dev`, sem exigir
configuração manual de Testcontainers.

---

## Resumo — extensões a adicionar via `quarkus create app` / `code.quarkus.io`

```
rest
rest-jackson
hibernate-validator
smallrye-openapi
hibernate-orm-panache
jdbc-postgresql
flyway
smallrye-health
```

(`arc` é incluída automaticamente por ser núcleo do Quarkus.)

Dependências de teste extras (`assertj-core`, `mockito-core`, se não vierem
pelo BOM) são adicionadas manualmente no `pom.xml`, fora do fluxo do
`quarkus create app`. `archunit-junit5` fica de fora por enquanto (adiado —
ver tabela de Testes acima).
