# ADR-0001: Backend em Java 21 + Quarkus 3 + Maven

Status: **Aceito** · Data: 2026-08-18

## Contexto

Precisamos de um backend Java para um projeto de portfólio nível sênior, com boa DX, startup
rápido para dev local/testes com Testcontainers, e ecossistema maduro de segurança/observabilidade.

## Decisão

Usar **Java 21** (LTS), **Quarkus 3.x** como framework, e **Maven** como build tool.

## Alternativas consideradas

- **Spring Boot**: ecossistema maior, mas Quarkus diferencia mais o portfólio (menos comum,
  mostra adaptação) e tem startup/testes com Testcontainers mais rápidos via dev services.
- **Gradle**: build mais flexível, porém Maven é o padrão mais visto em ambientes enterprise Java
  e reduz atrito de quem for avaliar o projeto.
- **Java 17**: também LTS, mas 21 traz virtual threads estáveis, úteis se o projeto evoluir para
  cargas de I/O mais pesadas.

## Consequências

- Ganhamos Dev Services do Quarkus (Postgres/Keycloak sobem automaticamente em dev/test).
- Equipe (mesmo que seja só o autor) precisa estar confortável com o modelo de extensões Quarkus.
- Build nativo (GraalVM) fica disponível como stretch goal, não obrigatório no MVP.
