# ADR-0004: Keycloak (OIDC) para autenticação e autorização

Status: **Aceito** · Data: 2026-08-18

## Contexto

O sistema precisa de RBAC real (RF-IAM-2/3): papéis distintos (`ADMIN`, `ESTOQUISTA`, `VENDEDOR`,
`GESTOR`) controlando acesso a endpoints. O projeto também busca demonstrar competência em
identity/access management, comum em vagas sênior.

## Decisão

Usar **Keycloak** como Identity Provider, integrado ao backend via extensão `quarkus-oidc`
(validação de JWT) e ao frontend via `keycloak-js` com fluxo Authorization Code + PKCE.

## Alternativas consideradas

- **JWT emitido pela própria aplicação**: mais simples de rodar, mas não demonstra integração com
  um IdP real nem delega corretamente gestão de usuários/senhas/MFA.
- **Auth0/Okta (SaaS gerenciado)**: reduziria carga operacional, mas tem tiers gratuitos limitados
  e não demonstra self-hosting de um IdP — trade-off revisitado no ADR-0007 (deploy).
- **Sem auth no MVP**: rejeitado — vai contra a constituição do projeto (`CLAUDE.md`, regra 7) e o
  objetivo de portfólio.

## Consequências

- Precisa rodar/gerenciar uma instância de Keycloak (local via Docker Compose; produção via
  ADR-0007).
- Realm, clients e papéis do Keycloak precisam ser versionados (export JSON em `infra/keycloak/`)
  para reprodutibilidade — tratado como parte do M1 no roadmap.
- Fluxo de login do frontend depende de redirect para o Keycloak (não é um simples POST de
  usuário/senha para a própria API).
