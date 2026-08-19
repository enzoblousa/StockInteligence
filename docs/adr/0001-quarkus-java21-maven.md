# ADR-0001: Backend em Java 21 + Quarkus + Maven

Status: **Aceito** · Data: 2026-08-19

## Contexto

O MVP precisa de um backend JVM que (a) tenha bom tempo de startup e footprint de memória
baixos — relevante para rodar em free tier de cloud (App Runner cobra por uso/memória) — e
(b) sirva como peça central de demonstração de arquitetura para portfólio.

## Decisão

- **Linguagem/runtime:** Java 21 (LTS), com virtual threads disponíveis para I/O-bound sem
  complexidade de reativo explícito.
- **Framework:** Quarkus 3.x — startup rápido, footprint baixo, extensões maduras para o que o
  projeto precisa (RESTEasy Reactive/JAX-RS, Hibernate ORM + Panache, Flyway, SmallRye OpenAPI,
  SmallRye JWT quando entrar auth no pós-MVP).
- **Build:** Maven, módulo único (ver ADR-0002). Sem Gradle — Maven é suficiente para o escopo e
  mais universalmente reconhecido.

## Alternativas consideradas

- **Spring Boot:** stack mais usada no mercado, mas startup/footprint maiores e menos
  diferenciação para um projeto de portfólio (é a escolha "óbvia"); Quarkus sinaliza
  familiaridade com stack cloud-native mais atual.
- **Micronaut:** objetivos parecidos com Quarkus (startup rápido, DI em compile-time), mas
  ecossistema/comunidade menor — menos suporte disponível para um projeto solo.
- **Node/NestJS ou outra stack não-JVM:** rejeitado porque o objetivo explícito do projeto é
  demonstrar backend em Java.

## Consequências

- Comunidade Quarkus é menor que a de Spring — menos respostas prontas em fóruns; compensado
  pela documentação oficial ser boa.
- Extensões Quarkus (Panache, SmallRye) impõem algumas convenções que a arquitetura (ADR-0002)
  precisa respeitar sem deixar vazar Panache para a camada de domínio.
