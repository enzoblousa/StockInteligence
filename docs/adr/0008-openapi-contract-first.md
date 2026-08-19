# ADR-0008: Contrato de API como fonte de verdade (OpenAPI), client do frontend gerado

Status: **Aceito** · Data: 2026-08-19

## Contexto

O frontend só existe para consumir a API corretamente (ver ADR-0005). Escrever o client HTTP à
mão convida a dessincronização silenciosa entre backend e frontend — exatamente o tipo de bug
que este projeto quer evitar por princípio de engenharia (contrato explícito > convenção
implícita).

## Decisão

- `docs/spec/04-api-contract.md` é o **contrato legível por humano**, escrito/atualizado antes de
  qualquer endpoint ser implementado (fluxo SDD, ver `CLAUDE.md`).
- O backend expõe o contrato **machine-readable** via **SmallRye OpenAPI** (anotações JAX-RS
  padrão do Quarkus), disponível em `/q/openapi` em qualquer ambiente (incluindo produção, sem
  dado sensível nisso).
- O **client TypeScript do frontend é gerado** a partir desse OpenAPI (`openapi-typescript`),
  nunca escrito manualmente — divergência de contrato vira erro de compilação no frontend, não
  bug silencioso em runtime.
- Erros seguem RFC 7807 (`application/problem+json`) em toda a API, documentados no contrato.

## Alternativas consideradas

- **Contract-first "puro"** (escrever `openapi.yaml` primeiro, gerar stubs de servidor a partir
  dele): mais rigoroso, mas Quarkus/SmallRye favorece o caminho inverso (anotações no código
  geram o OpenAPI) e a ferramenta de geração de stubs JAX-RS a partir de YAML adiciona
  complexidade de build não essencial para o tamanho do MVP. O contrato em Markdown
  (`04-api-contract.md`) cumpre o papel de "escrito antes do código" sem essa ferramenta extra.
- **Client do frontend escrito à mão:** rejeitado pelo motivo já descrito no Contexto.
- **GraphQL:** rejeitado — REST é suficiente para o volume de casos de uso do MVP e mantém o
  contrato mais simples de documentar/ler.

## Consequências

- Qualquer mudança de endpoint precisa primeiro atualizar `04-api-contract.md`, depois as
  anotações no backend, depois regenerar o client do frontend — nessa ordem (regra do SDD).
- CI do frontend (M4) inclui um passo de regeneração do client a partir do OpenAPI publicado
  pelo backend em build, para pegar drift automaticamente.
