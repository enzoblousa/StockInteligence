# ADR-0007: Infraestrutura cloud em AWS free tier (App Runner + RDS), Terraform, frontend fora da AWS

Status: **Aceito** · Data: 2026-08-19

## Contexto

O MVP precisa de uma URL pública real para demonstração de portfólio, com custo zero (ou
próximo) durante os primeiros 12 meses, sem que a configuração de infraestrutura vire o foco do
trabalho (o foco de profundidade é backend + domínio, ver `docs/spec/00-vision.md`).

## Decisão

- **Backend (Quarkus):** empacotado como container, publicado no **Amazon ECR**, deployado em
  **AWS App Runner** — HTTPS, deploy a partir de imagem e scaling gerenciados pela plataforma,
  sem precisar configurar VPC/ALB manualmente.
- **Banco:** **AWS RDS PostgreSQL**, instância free tier (`db.t3.micro`/`db.t4g.micro`, 20GB),
  válida pelos primeiros 12 meses da conta AWS.
- **Frontend:** hospedado na **Vercel** (fora da AWS, de propósito) — free tier generoso para
  SPA estática, deploy por push, zero configuração de CDN manual.
- **IaC:** toda a infra AWS (App Runner, RDS, ECR, IAM, SSM) descrita em **Terraform**,
  versionado em `infra/`.
- **Segredos:** AWS SSM Parameter Store (ou Secrets Manager), injetados como variável de
  ambiente no App Runner — nunca hardcoded (ver `CLAUDE.md` regra 8).
- **Observabilidade:** CloudWatch Logs/Metrics nativo do App Runner no MVP; sem OpenTelemetry/
  X-Ray no MVP (evolução possível, não bloqueante).
- **Custo sob controle:** AWS Budgets com alerta configurado desde o M4 (deploy); `terraform
  destroy` documentado no README para desligar tudo entre demonstrações.

## Alternativas consideradas

- **Fly.io/Render + Neon (Postgres serverless):** setup ainda mais simples, sem cliff de 12
  meses (Neon tem free tier permanente), e sinaliza bem "MVP enxuto". Foi a opção descartada
  aqui porque o usuário priorizou explicitamente **AWS** — fica registrado como a alternativa
  natural a revisitar se o custo pós free-tier da RDS/App Runner virar problema (ver
  Consequências).
- **ECS Fargate + ALB + VPC própria desde já:** mais "AWS de verdade" (rede, IAM mais explícito),
  mas adiciona complexidade operacional não essencial para o MVP; App Runner cobre o mesmo
  container sem esse overhead. Documentado como evolução possível se o projeto quiser aprofundar
  essa demonstração depois.
- **Kubernetes (EKS):** rejeitado — overhead operacional desproporcional ao tamanho do sistema.
- **S3 + CloudFront para o frontend:** funcionaria, mas não há ganho de portfólio em mover uma
  SPA estática simples para dentro da AWS quando Vercel já resolve isso com deploy por push.

## Consequências

- App Runner **não escala a zero** — há custo residual mesmo com baixo tráfego, e o free tier de
  12 meses da conta AWS eventualmente expira. Isso é um ponto de revisão explícito no roadmap
  pós-MVP: se o custo virar problema, revisitar para Fly.io/Render + Neon nesse momento, sem
  travar essa migração por apego à decisão atual.
- Pipeline de CI/CD precisa orquestrar build/push da imagem do backend + `terraform apply` (ver
  `docs/spec/05-roadmap.md`, M4) — deploy é acionado manualmente (`workflow_dispatch`), não a
  cada merge, para evitar custo/reaplicação acidental durante o desenvolvimento do MVP.
- Manter o frontend fora da AWS (Vercel) é uma escolha consciente de multi-cloud, coerente com
  o ADR-0005 tratar o frontend como deploy desacoplado do backend.
