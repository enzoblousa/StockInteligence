# 05 — Roadmap de Implementação

Status: **Aceito** · Última revisão: 2026-08-19

Ordem pensada para sempre ter algo executável/demonstrável ao final de cada marco. Cada marco,
ao ser fechado, deve deixar a spec (`docs/spec/`) refletindo fielmente o que foi implementado
(regra de reconciliação do `CLAUDE.md`).

## M0 — Fundação

- Scaffolding `backend/` (Quarkus, Maven, extensões: REST, Hibernate ORM Panache, JDBC Postgres,
  Flyway, OIDC, SmallRye OpenAPI, Micrometer, logging JSON).
- Scaffolding `frontend/` (Vite + React + TS + Tailwind + shadcn/ui, roteamento básico).
- `docker-compose.yml` (Postgres + Keycloak + backend + frontend) para dev local.
- Health checks (`/q/health`) e `GET /api/v1/ping` de sanidade.
- CI (GitHub Actions): build + test de backend e frontend em cada PR.
- Realm/clients/papéis do Keycloak exportados em `infra/keycloak/realm-export.json`.

## M1 — Catálogo, Parceiros & Auth

- RF-CAT-1..4, RF-PAR-1..2 implementados fim a fim (API + tela de listagem/formulário no
  frontend).
- Login funcional via Keycloak no frontend; `@RolesAllowed` aplicado nos endpoints de escrita.
- Migrations Flyway iniciais (`product`, `category`, `partner`).

## M2 — Núcleo de Estoque

- `StockBalance`/`StockMovement` (RF-EST-1..5), incluindo ajuste manual.
- `@Version` e teste de integração de concorrência (duas escritas simultâneas).
- Tela de consulta de saldo + histórico de movimentações.

## M3 — Compras

- RF-CMP-1..4 fim a fim, incluindo geração de `StockMovement` ao receber.
- Teste de integração compra→estoque, incluindo idempotência do recebimento.

## M4 — Vendas

- RF-VND-1..5 fim a fim: confirmar (reserva), faturar (baixa), cancelar (libera reserva).
- Teste de integração venda→estoque, incluindo o caso de estoque insuficiente e o caso de
  concorrência (RF-VND-5).

## M5 — Alertas & Relatórios

- Evento `ProductLowStockReached` (RF-ALR-1) + endpoint de listagem (RF-ALR-2).
- Notificação por e-mail (opcional, se houver tempo — senão fica documentado como pendente).
- Relatórios RF-REL-1..3 (API + tela simples de dashboard no frontend).

## M6 — Observabilidade & Deploy

- Métricas de negócio + tracing (RNF-4).
- Infra AWS provisionada via Terraform (VPC/networking mínimo, RDS, App Runner, ECR, Secrets
  Manager) — ver ADR-0009.
- Pipeline de CD: build de imagem, push para ECR, deploy backend+Keycloak no AWS App Runner
  (RDS compartilhado), deploy frontend no Vercel/Netlify.
- Smoke test pós-deploy (script simples batendo nos health checks e num fluxo de leitura).
- AWS Budgets com alerta de custo configurado; script/instrução de `terraform destroy` documentada
  para desligar tudo entre demonstrações, se necessário.
- README do projeto atualizado com link público, instruções de rodar localmente, e um diagrama
  de arquitetura (reaproveitar `03-architecture.md`).

## M7 — Stretch goals (fora do MVP, priorizar só se M0–M6 estiverem sólidos)

- Multi-depósito e rastreio por lote/validade (exigiria revisar `02-domain-model.md`).
- Recebimento parcial de pedido de compra (RF-CMP-5).
- Mensageria assíncrona (Kafka/SmallRye Reactive Messaging) substituindo eventos in-process
  (ADR-0006 já deixa a porta pronta para isso).
- Fluxo de devolução de venda faturada (reverter estoque de forma auditável).
- Build nativo (GraalVM) do backend.
- Internacionalização.
- Migração de App Runner para ECS Fargate + ALB + VPC própria, se quiser aprofundar a
  demonstração de rede/IAM na AWS (não necessário para o objetivo do MVP).

## M8 — IA & IoT (visão de futuro, não planejado em detalhe — ver `00-vision.md`)

Só entra em detalhamento de requisitos (`RF-IA-*`/`RF-IOT-*`) se M0–M7 estiverem fechados e o
projeto seguir ativo. Direção pretendida, sem compromisso de escopo/data:

- Ingestão de eventos de leitores de código de barras/câmeras via AWS IoT Core, entrando no
  domínio pela `InventoryPort` já existente (sem redesenhar `Inventory`).
- Previsão de demanda / sugestão de ponto de reposição a partir do histórico de
  `StockMovement` (candidato: job batch simples antes de qualquer modelo de ML custom).

## Como usar este roadmap

Ao começar um marco, abra uma issue/branch por item, referenciando o ID de requisito
(`RF-CAT-1`, etc.). Ao terminar um marco, revise se algum RF/RNF precisou ser ajustado durante a
implementação e atualize o spec correspondente antes de fechar.
