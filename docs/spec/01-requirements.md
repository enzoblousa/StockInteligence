# 01 — Requisitos

Status: **Aceito** · Última revisão: 2026-08-19

Convenção de ID: `RF-<módulo>-<n>` para funcional, `RNF-<n>` para não-funcional. Referencie estes
IDs em commits/PRs/tarefas do roadmap para rastreabilidade.

Requisitos de IA/IoT (visão de futuro descrita em `00-vision.md`) não têm IDs ainda — só ganham
`RF-IA-*`/`RF-IOT-*` quando forem de fato planejados em um marco do roadmap (M8), para não
inflar o MVP com requisitos especulativos.

## RF — Catálogo (CAT)

- **RF-CAT-1**: CRUD de produto (SKU único, nome, categoria, unidade de medida, preço de custo,
  preço de venda, estoque mínimo).
- **RF-CAT-2**: CRUD de categoria de produto.
- **RF-CAT-3**: Produto não pode ser excluído se tiver movimentação de estoque associada
  (apenas inativado).
- **RF-CAT-4**: Busca/listagem paginada e filtrável de produtos (por nome, SKU, categoria).

## RF — Parceiros (PAR)

- **RF-PAR-1**: CRUD de parceiro (nome, documento — CPF/CNPJ —, contato), com papel(éis)
  `FORNECEDOR` e/ou `CLIENTE` (um parceiro pode ser os dois).
- **RF-PAR-2**: Documento (CPF/CNPJ) validado e único por parceiro.

## RF — Estoque (EST)

- **RF-EST-1**: Cada produto tem um saldo de estoque (quantidade disponível) no depósito único
  do MVP.
- **RF-EST-2**: Toda alteração de saldo gera um registro de movimentação imutável: tipo
  (`ENTRADA_COMPRA`, `SAIDA_VENDA`, `AJUSTE_POSITIVO`, `AJUSTE_NEGATIVO`), quantidade, saldo
  resultante, referência ao documento de origem (pedido de compra/venda ou nulo para ajuste
  manual), usuário responsável, timestamp.
- **RF-EST-3**: Ajuste manual de estoque exige motivo (texto) e é restrito a papéis
  `ADMIN`/`ESTOQUISTA`.
- **RF-EST-4**: Saldo nunca fica negativo — qualquer operação que resultaria em negativo é
  rejeitada com erro de negócio explícito.
- **RF-EST-5**: Consulta de histórico de movimentações por produto, paginada, com filtro por
  período e tipo.

## RF — Compras (CMP)

- **RF-CMP-1**: Criar pedido de compra (fornecedor + linhas de produto/quantidade/custo
  unitário), status inicial `RASCUNHO`.
- **RF-CMP-2**: Confirmar pedido de compra (`RASCUNHO` → `CONFIRMADO`) — trava edição das linhas.
- **RF-CMP-3**: Receber pedido de compra (`CONFIRMADO` → `RECEBIDO`) — gera movimentação de
  entrada de estoque para cada linha, atualiza custo do produto (opcional: custo médio ponderado).
- **RF-CMP-4**: Cancelar pedido de compra antes do recebimento (`RASCUNHO`/`CONFIRMADO` →
  `CANCELADO`) — não afeta estoque.
- **RF-CMP-5**: Recebimento parcial é fora do MVP (recebimento é sempre integral); registrar como
  débito técnico explícito no roadmap (M7).

## RF — Vendas (VND)

- **RF-VND-1**: Criar pedido de venda (cliente + linhas de produto/quantidade/preço unitário),
  status inicial `RASCUNHO`.
- **RF-VND-2**: Confirmar pedido de venda (`RASCUNHO` → `CONFIRMADO`) — **reserva** a quantidade
  (reduz disponível sem gerar movimentação de saída ainda); rejeita se estoque disponível
  (saldo − já reservado) for insuficiente para qualquer linha.
- **RF-VND-3**: Faturar pedido de venda (`CONFIRMADO` → `FATURADO`) — gera movimentação de saída
  de estoque, baixa a reserva.
- **RF-VND-4**: Cancelar pedido de venda (`RASCUNHO`/`CONFIRMADO` → `CANCELADO`) — se havia
  reserva, libera-a; não gera movimentação de estoque.
- **RF-VND-5**: Duas confirmações concorrentes disputando o mesmo saldo nunca podem ambas ter
  sucesso além do que o estoque permite (ver RNF de concorrência).

## RF — Alertas (ALR)

- **RF-ALR-1**: Quando o saldo de um produto fica ≤ estoque mínimo cadastrado, o sistema registra
  um alerta de estoque baixo.
- **RF-ALR-2**: Alertas ativos são listáveis via API/UI; opcionalmente notificados por e-mail
  (M5 — ver roadmap).

## RF — Identidade & Acesso (IAM)

- **RF-IAM-1**: Login via Keycloak (OIDC); sessão do frontend baseada em token.
- **RF-IAM-2**: Papéis: `ADMIN`, `ESTOQUISTA`, `VENDEDOR`, `GESTOR` (somente leitura de
  relatórios).
- **RF-IAM-3**: Toda rota de escrita da API exige papel compatível com a ação (ver
  `03-architecture.md` para mapeamento endpoint → papel).

## RF — Relatórios (REL)

- **RF-REL-1**: Relatório de produtos abaixo do estoque mínimo.
- **RF-REL-2**: Relatório de movimentações de estoque por período.
- **RF-REL-3**: Relatório simples de giro de estoque (quantidade vendida / saldo médio no
  período) — M5, pode ser simplificado no MVP.

## Requisitos não-funcionais (RNF)

- **RNF-1 (Consistência/Concorrência)**: Operações que alteram saldo de estoque usam controle de
  concorrência otimista (`@Version`); em conflito, a operação é reexecutada ou falha de forma
  explícita e idempotente — nunca perde escrita silenciosamente.
- **RNF-2 (Auditabilidade)**: Toda movimentação de estoque é imutável (sem UPDATE/DELETE) e
  rastreável até usuário + documento de origem.
- **RNF-3 (Segurança)**: Autenticação/autorização via OIDC real; segredos fora do código;
  comunicação em produção via HTTPS.
- **RNF-4 (Observabilidade)**: Logs estruturados (JSON) com correlation/trace id; métricas
  expostas via Micrometer/Prometheus; tracing distribuído via OpenTelemetry.
- **RNF-5 (Performance)**: Listagens paginadas (nunca retornar coleção completa sem limite);
  índices no banco para colunas de busca/filtro frequentes (SKU, documento do parceiro, status
  de pedido).
- **RNF-6 (Testabilidade)**: Regras de domínio cobertas por teste unitário puro; fluxos críticos
  (compra→estoque, venda→estoque, concorrência) cobertos por teste de integração com
  Testcontainers.
- **RNF-7 (Confiabilidade de contrato)**: API documentada via OpenAPI; mudanças de contrato
  quebram o build de CI (checagem de compatibilidade) antes de quebrar o frontend em produção.
- **RNF-8 (Deploy)**: Aplicação containerizada, pipeline de CI/CD, deploy reproduzível a partir de
  um commit (sem passos manuais não documentados).
- **RNF-9 (Erro)**: Respostas de erro em `application/problem+json` (RFC 7807), nunca stack trace
  cru para o cliente.
- **RNF-10 (Internacionalização)**: Fora de escopo no MVP; idioma único pt-BR, mas nomes de
  domínio no código em inglês (ver `02-domain-model.md`) para não travar extensão futura.
