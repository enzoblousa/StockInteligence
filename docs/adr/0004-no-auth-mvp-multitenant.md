# ADR-0004: Sem autenticação no MVP; multi-tenant por ID explícito na URL

Status: **Aceito** · Data: 2026-08-19

## Contexto

Duas decisões de escopo foram tomadas juntas para o MVP e precisam ficar documentadas porque,
combinadas, criam um trade-off que não pode ficar implícito:

1. O sistema é **multi-tenant desde o MVP** — cada loja (`Tenant`) tem seu próprio catálogo de
   produtos e movimentações, isolados por `tenantId`.
2. O MVP **não implementa autenticação/autorização** — sem login, sem sessão, sem JWT.

A combinação das duas significa que, no MVP, **qualquer pessoa que souber (ou adivinhar) o
`tenantId` de uma loja consegue ler e escrever os dados daquela loja.** Isso é aceitável para um
projeto de portfólio/demo, mas seria uma falha grave em produção real — precisa estar visível,
não escondido atrás de "ainda não implementamos auth".

## Decisão

- `Tenant` (loja) é uma entidade de primeira classe desde o M1. Todo recurso de negócio é
  aninhado sob `/api/tenants/{tenantId}/...` (ver `docs/spec/04-api-contract.md`).
- `tenantId` é um **UUID v4** (não sequencial) — reduz enumeração casual, mas **não é controle
  de acesso**, é só ofuscação. Isso fica dito explicitamente aqui para não ser confundido com
  segurança real.
- Não há tabela de usuário, papel, sessão ou middleware de autorização no MVP.
- O risco é declarado no README do projeto e em `docs/spec/00-vision.md` como limitação
  conhecida e deliberada do MVP, não um esquecimento.
- **Autenticação real é a primeira prioridade do pós-MVP** (ver `docs/spec/05-roadmap.md`,
  marco pós-MVP M+1): usuários passam a pertencer a um `Tenant` com papel, e todo endpoint passa
  a exigir usuário autenticado cujo `tenantId` bata com o da URL.

## Alternativas consideradas

- **Keycloak/OIDC desde o MVP:** era a decisão original do projeto (versão anterior deste
  documento). Rejeitada para o MVP porque exige hospedar/operar um serviço adicional (Keycloak),
  configurar realm/clients e integrar `keycloak-js` no frontend — trabalho real que não avança a
  demonstração central do MVP (estoque consistente, auditável, multi-loja). Fica como próximo
  passo documentado, não descartado.
- **API key simples por tenant** (meio-termo entre "nada" e OIDC completo): considerada como
  primeiro passo pós-MVP antes de ir direto para OIDC completo — mais simples que Keycloak,
  ainda dá alguma proteção real. Não escolhida para o MVP em si porque o pedido explícito foi
  "sem autenticação no MVP"; fica registrada aqui como candidata natural ao M+1.
- **Não modelar multi-tenant agora, adicionar depois:** rejeitada — o usuário quer o modelo de
  dados multi-tenant já correto desde o início, para não pagar uma migração de dados depois.

## Consequências

- Nenhuma feature de negócio nova deve ser priorizada no roadmap antes de autenticação real,
  uma vez que o MVP esteja demonstrado publicamente (ver `CLAUDE.md` regra 7).
- Qualquer deploy público (ADR-0007) deve deixar claro (README, e opcionalmente um banner no
  frontend) que o ambiente é uma demo sem controle de acesso — não inserir dados reais sensíveis.
- Quando a autenticação for implementada, este ADR é **superseded** por um novo ADR — este texto
  nunca é editado retroativamente (ver `CLAUDE.md` regra sobre ADRs).
