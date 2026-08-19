# ADR-0003: PostgreSQL + Flyway

Status: **Aceito** · Data: 2026-08-19

## Contexto

O domínio exige consistência transacional forte (saldo de estoque nunca pode ficar negativo,
concorrência precisa ser tratada explicitamente — ver ADR-0006) e tipos numéricos exatos para
quantidade/valores monetários. O schema também precisa evoluir de forma rastreável desde o M1.

## Decisão

- **PostgreSQL** como banco relacional único do sistema.
- **Flyway** para migrations versionadas em `backend/src/main/resources/db/migration`;
  `hibernate.hbm2ddl` nunca é `update`/`create` fora de testes (ver `CLAUDE.md` regra 2).
- Tipos: `NUMERIC`/`BIGINT` para quantidade e valores monetários (nunca `FLOAT`/`DOUBLE`),
  mapeados para `BigDecimal`/`int` em Java (ver `CLAUDE.md` regra 4).

## Alternativas consideradas

- **MySQL:** também atenderia, mas Postgres tem melhor suporte em free tiers gerenciados
  relevantes (RDS free tier, e alternativas como Neon/Supabase caso o ADR-0007 seja revisitado)
  e semântica de `NUMERIC` mais previsível.
- **MongoDB:** rejeitado — o domínio é fundamentalmente relacional e precisa de transações
  ACID entre a atualização de saldo e a inserção do registro de movimentação (ver
  `docs/spec/02-domain-model.md`).
- **H2/banco embarcado em produção:** só usado em testes (via Quarkus Dev Services/Testcontainers
  para Postgres real em teste de integração); nunca em produção.

## Consequências

- Toda mudança de schema passa por migration nova — nunca editar uma migration já aplicada em
  qualquer ambiente compartilhado.
- Dev local usa Quarkus Dev Services (Testcontainers) para subir Postgres automaticamente sem
  exigir Docker Compose manual.
