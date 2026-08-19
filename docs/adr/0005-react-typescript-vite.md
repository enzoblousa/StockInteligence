# ADR-0005: Frontend React + TypeScript + Vite, SPA desacoplada

Status: **Aceito** · Data: 2026-08-18

## Contexto

O frontend deve ser um framework "simples" no sentido de não exigir infraestrutura própria de
servidor (nada de SSR complexo), mas ainda assim demonstrar boas práticas modernas de frontend
para vaga sênior, consumindo a API Quarkus via contrato tipado.

## Decisão

**React 18 + TypeScript**, build com **Vite**, SPA totalmente desacoplada do backend (deploy
separado). Client de API gerado a partir do OpenAPI exposto pelo Quarkus. UI com
**Tailwind CSS + shadcn/ui**. Estado de servidor via **TanStack Query**; formulários via
**React Hook Form + Zod**.

## Alternativas consideradas

- **Vue 3 + TypeScript**: curva de aprendizado mais suave, também viável; React escolhido por ser
  o mais demandado em vagas sênior (critério explícito do usuário).
- **Htmx + Alpine.js servido pelo Quarkus (Qute)**: stack mais enxuta e unificada (um único
  deploy), mas reduz a superfície para demonstrar competência de frontend moderno desacoplado —
  contra o objetivo de portfólio.
- **Next.js**: SSR/SSG não agregam aqui (app autenticado, não é conteúdo indexável) e adiciona
  complexidade de infraestrutura sem benefício claro.

## Consequências

- Dois deploys independentes (frontend estático + backend), exigindo CORS configurado
  corretamente em produção.
- Contrato de API vira uma dependência de build do frontend (client gerado) — mudança de contrato
  quebra o build do frontend antes de quebrar em produção (RNF-7), o que é desejado.
