# ADR-0009: Infraestrutura cloud em AWS (App Runner + RDS), IaC via Terraform — supersede ADR-0007

Status: **Aceito** · Data: 2026-08-19 · **Supersede ADR-0007**

## Contexto

O ADR-0007 original decidiu Fly.io (backend + Postgres + Keycloak) e Vercel/Netlify (frontend),
priorizando custo baixo e setup rápido. O projeto foi reformulado (`00-vision.md`) para um
produto ("Stock Master") com visão declarada de médio prazo envolvendo **IoT** (leitores de
código de barras, câmeras) e **IA** (previsão de demanda). Isso muda o cálculo:

- AWS é o provedor mais demandado em vagas sênior — decidir por ele tem valor de portfólio maior
  que otimizar só por custo/simplicidade.
- A visão de IoT futura (M8) tem um caminho nativo em AWS (IoT Core); Google descontinuou o
  Cloud IoT Core em 2023, o que enfraquece GCP como opção caso essa direção vingue.
- O projeto ainda precisa de uma URL pública barata e de setup que não vire o foco do trabalho
  (mesma preocupação do ADR-0007 original) — isso descarta soluções operacionalmente pesadas
  (Kubernetes/EKS) e favorece serviços gerenciados dentro da AWS, não a AWS "completa" desde o
  dia 1.

## Decisão

- **Backend (Quarkus)**: container Docker publicado no **Amazon ECR**, deployado em
  **AWS App Runner** (HTTPS, scaling e deploy a partir de imagem gerenciados pela plataforma —
  evita configurar VPC/ALB manualmente no MVP).
- **Banco**: **AWS RDS PostgreSQL** (free tier, `db.t3.micro`), com upgrade documentado para
  Aurora Serverless v2 se o custo/carga justificar depois.
- **Keycloak**: container próprio, também em App Runner, usando a mesma instância RDS (schema/
  database separado) para não pagar por um segundo banco gerenciado.
- **Frontend**: mantido em **Vercel ou Netlify** (fora da AWS, de propósito — ver alternativas).
- **IaC**: toda a infra AWS descrita em **Terraform**, versionado em `infra/`, aplicado via CI.
- **Segredos**: AWS Secrets Manager / SSM Parameter Store, injetados como variável de ambiente no
  App Runner — nunca hardcoded (constituição, regra 8).
- **Observabilidade**: CloudWatch Logs/Metrics nativo do App Runner no MVP; ADOT
  (AWS Distro for OpenTelemetry) como evolução para X-Ray, não bloqueante.
- **Custo**: AWS Budgets com alerta configurado desde o M6; `terraform destroy` documentado no
  README para desligar tudo entre demonstrações.

## Alternativas consideradas

- **Manter Fly.io + Vercel (ADR-0007 original)**: mais simples e mais barato de operar, mas (a)
  sinaliza menos para o mercado de trabalho do que AWS, e (b) não tem caminho nativo para a visão
  de IoT declarada em `00-vision.md`. Era a decisão certa para o escopo anterior (StockPilot
  genérico); deixou de ser a melhor decisão quando o produto ganhou essa direção futura.
- **Render.com**: PaaS tipo Heroku, fricção ainda menor que App Runner, mas mesmo trade-off de
  não ser AWS nem ter IoT nativo — rejeitado pelo mesmo motivo.
- **Google Cloud Run + Cloud SQL**: bom modelo serverless/pay-per-use, mas o Cloud IoT Core foi
  descontinuado em 2023 — enfraquece a opção justamente na dimensão que motivou reabrir esta
  decisão.
- **Kubernetes (EKS)**: rejeitado pelo mesmo motivo do ADR-0007 original — overhead operacional
  desproporcional ao tamanho do sistema; o objetivo é ter algo publicado e sólido, não demonstrar
  operação de k8s.
- **AWS CDK em vez de Terraform**: também seria uma escolha razoável (TypeScript, reaproveita
  stack do frontend), mas Terraform é a ferramenta de IaC mais reconhecida no mercado
  independente de cloud — melhor sinal de portfólio e mais transferível.
- **ECS Fargate + ALB + VPC própria desde já**: mais "AWS de verdade" (rede, IAM mais explícito),
  mas adiciona complexidade operacional não essencial para o MVP; App Runner cobre o mesmo
  container sem esse overhead. Documentado como evolução possível no roadmap (M7) se o projeto
  quiser aprofundar essa demonstração depois.

## Consequências

- App Runner + RDS + Keycloak em App Runner ainda são **três serviços gerenciados** de infra
  (mais o frontend fora da AWS) — pipeline de CI/CD precisa orquestrar build/push/deploy dos
  dois containers (backend, Keycloak) e `terraform apply` (ver roadmap M6).
- Fica documentado como ponto de revisão explícito: se o custo do free tier da RDS/App Runner
  virar problema (ex: após 12 meses), revisitar (mesmo espírito de "decisão viva" do ADR-0007
  original) — não travar em Aurora Serverless v2 ou outro provedor sem necessidade real.
- Manter o frontend fora da AWS (Vercel/Netlify) é uma escolha consciente de multi-cloud: o
  ADR-0005 já trata o frontend como deploy desacoplado, e Vercel/Netlify continuam sendo a opção
  mais simples/gratuita para SPA estática — não há ganho de portfólio em mover isso para
  S3+CloudFront agora.
- ADR-0007 permanece no histórico do repositório (nunca editado — constituição do `CLAUDE.md`);
  este ADR é a referência vigente para infraestrutura a partir de 2026-08-19.
