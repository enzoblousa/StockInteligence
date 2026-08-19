# ADR-0008: API contract-first via OpenAPI, client TypeScript gerado

Status: **Aceito** · Data: 2026-08-18

## Contexto

Backend e frontend evoluem de forma desacoplada (ADR-0005). Sem um contrato explícito, é fácil o
frontend divergir silenciosamente do formato real da API (RNF-7).

## Decisão

O backend expõe o contrato via **SmallRye OpenAPI** (anotações JAX-RS + `@Schema` onde necessário
para nomes de domínio em português nos payloads, se aplicável). O frontend **gera** seu client
TypeScript a partir desse contrato (`openapi-typescript` ou `orval`) como parte do pipeline de
build/CI — nunca escreve tipos de request/response à mão.

## Alternativas consideradas

- **Contrato escrito à mão em YAML antes do código (design-first puro)**: mais rigoroso, mas
  adiciona um passo manual de sincronização; anotação no código Quarkus como fonte única é mais
  pragmático para o tamanho do projeto, mantendo o benefício de ter o contrato publicado.
- **Sem geração de client (frontend escreve fetch manual)**: mais rápido no curto prazo, mas é
  exatamente a divergência de contrato que este ADR busca evitar.

## Consequências

- Pipeline de CI do frontend inclui um passo de "gerar client" a partir do OpenAPI publicado pelo
  backend (via artefato de build ou contrato commitado em `docs/spec/openapi.yaml` — a definir na
  implementação do M0).
- Mudança de contrato que quebra compatibilidade é visível como diff no client gerado, revisável
  em PR.
