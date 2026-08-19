# ADR-0007: Deploy — Fly.io (backend + Postgres + Keycloak) e Vercel/Netlify (frontend)

Status: **Aceito, sujeito a revisão** · Data: 2026-08-18

## Contexto

O projeto precisa de uma URL pública real para portfólio (00-vision.md, critério de sucesso), com
custo próximo de zero e setup simples o bastante para não virar o foco do projeto (o foco é
domínio + backend + frontend, não DevOps de plataforma).

## Decisão

- **Backend (Quarkus)**: container Docker deployado no **Fly.io**.
- **Banco**: Postgres gerenciado (Fly Postgres, ou Neon como alternativa se o free tier do Fly
  mudar).
- **Keycloak**: container próprio também no Fly.io.
- **Frontend**: build estático no **Vercel** ou **Netlify** (CDN, preview deploy por PR).

## Alternativas consideradas

- **AWS (ECS/RDS/Cognito)**: mais "enterprise", porém setup e custo bem maiores; fica como
  possível migração futura documentada aqui, não como MVP.
- **Kubernetes**: rejeitado para este projeto — overhead operacional desproporcional ao tamanho do
  sistema; o usuário priorizou ter algo publicado rápido em vez de demonstrar k8s.
- **Cognito/Auth0 no lugar de self-host Keycloak**: reduziria carga operacional, mas o ADR-0004 já
  decidiu por Keycloak self-hosted; caso o consumo de recursos do Keycloak no Fly.io se mostrar
  pesado/caro, **este ADR será superado** por um novo registrando a troca (ex: Keycloak só em
  dev/CI, IdP gerenciado em produção).

## Consequências

- Três serviços deployados de forma independente (backend, Postgres, Keycloak) mais o frontend —
  pipeline de CI/CD precisa orquestrar isso (ver roadmap M6).
- Ponto explícito de revisão: se o custo/latência do Keycloak self-hosted no Fly.io for ruim,
  revisitar (não é uma decisão "de uma vez para sempre" — está marcada como tal aqui de propósito).
