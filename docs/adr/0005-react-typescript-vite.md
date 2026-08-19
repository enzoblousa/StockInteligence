# ADR-0005: Frontend em React + TypeScript + Vite, deliberadamente simples

Status: **Aceito** · Data: 2026-08-19

## Contexto

O frontend existe para consumir a API de forma correta e tipada, não para ser o foco de
profundidade do projeto (ver `docs/spec/00-vision.md`). Precisa ser simples de montar e manter,
sem sacrificar type-safety no consumo do contrato de API.

## Decisão

- **React 18 + TypeScript**, bundler **Vite** (dev server rápido, config mínima).
- **TanStack Query** para cache/estado de dados de servidor — evita reimplementar loading/erro/
  refetch manualmente, mantendo o código de tela pequeno.
- **Client de API gerado a partir do OpenAPI** exposto pelo backend (`openapi-typescript`),
  nunca escrito à mão (ver ADR-0008) — garante que o frontend nunca fica dessincronizado do
  contrato sem perceber (erro de compilação).
- **CSS simples** (CSS puro/CSS Modules), sem design system (Tailwind/shadcn) no MVP — o
  investimento visual é mínimo de propósito; pode entrar depois se o projeto quiser evoluir a
  UI.
- Sem roteador client-side "pesado" no MVP — poucas telas, navegação simples (React Router só se
  o número de telas justificar; decisão final no M3 ao implementar).

## Alternativas consideradas

- **Next.js:** SSR/roteamento de arquivo são overkill para uma SPA fina que só consome uma API já
  existente; rejeitado.
- **Tailwind + shadcn/ui:** era a escolha da versão anterior deste ADR; removida do MVP para
  reduzir setup — o frontend não é onde o projeto investe profundidade agora.
- **Vue/Svelte:** sem motivo de portfólio para desviar de React, que tem mais relevância de
  mercado.
- **JavaScript puro (sem TypeScript):** rejeitado — TypeScript é o que permite ao client gerado
  do OpenAPI pegar divergência de contrato em tempo de compilação, que é justamente o ponto de
  "consumir corretamente uma API tipada".

## Consequências

- Poucas telas no MVP (seleção/criação de loja, lista de produtos com destaque de estoque baixo,
  formulário de produto, formulário de movimentação, histórico de movimentações) — ver
  `docs/spec/03-architecture.md`.
- Se o projeto quiser aprofundar UI depois, isso é uma decisão nova (novo ADR), não algo a fazer
  "de brinde" durante o MVP.
