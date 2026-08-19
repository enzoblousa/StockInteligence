# ADR-0003: PostgreSQL + Flyway como camada de persistência

Status: **Aceito** · Data: 2026-08-18

## Contexto

Precisamos de um banco relacional com suporte maduro a transações, constraints, e controle de
concorrência otimista (`@Version`) para o agregado `StockBalance` (RNF-1).

## Decisão

**PostgreSQL** como banco primário, com **Flyway** para versionamento de schema. Nenhum ambiente
além de testes unitários usa `hibernate.hbm2ddl.auto=update`/`create`.

## Alternativas consideradas

- **MySQL/MariaDB**: também viável, mas Postgres tem melhor suporte a tipos avançados (JSONB,
  arrays) que podem ser úteis em relatórios futuros, e é o mais comum em vagas sênior que usam
  Quarkus.
- **Flyway vs Liquibase**: Flyway escolhido pela simplicidade de SQL puro versionado (mais fácil
  de revisar em PR do que XML/YAML do Liquibase).

## Consequências

- Toda mudança de schema é um arquivo `V<N>__descricao.sql` versionado e revisado.
- Dev local usa Dev Services do Quarkus (container Postgres efêmero); Testcontainers nos testes
  de integração garante que a mesma engine roda em CI.
