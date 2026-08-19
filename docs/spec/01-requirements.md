# 01 — Requisitos

Status: **Aceito** · Última revisão: 2026-08-19

## Requisitos funcionais

### Módulo: Lojas (Tenant)

- **RF-01** — Criar uma loja informando nome. Sistema retorna um identificador único (UUID).
- **RF-02** — Consultar dados de uma loja pelo identificador.

### Módulo: Produtos

- **RF-03** — Cadastrar produto em uma loja: SKU (único dentro da loja), nome, unidade de
  medida, estoque mínimo, e opcionalmente custo unitário e preço de venda.
- **RF-04** — Editar metadados de um produto (nome, estoque mínimo, custo, preço, ativo/inativo).
  Edição de metadados **nunca** altera o saldo de estoque diretamente (só movimentação altera
  saldo, ver RF-06).
- **RF-05** — Listar produtos de uma loja, com filtro opcional por "abaixo do estoque mínimo" e
  por "ativo".

### Módulo: Movimentação de estoque

- **RF-06** — Registrar movimentação de estoque de um produto: tipo (`ENTRADA`, `SAIDA`,
  `AJUSTE`), quantidade e motivo opcional (obrigatório para `AJUSTE`). O saldo do produto é
  atualizado atomicamente junto com o registro da movimentação.
- **RF-07** — Uma `SAIDA` que deixaria o saldo negativo é rejeitada (erro de negócio, não altera
  nada).
- **RF-08** — Toda movimentação registrada é imutável — não existe endpoint de editar ou apagar
  movimentação.
- **RF-09** — Consultar histórico de movimentações de um produto, paginado, mais recente primeiro.

### Módulo: Alertas de estoque baixo

- **RF-10** — A listagem de produtos (RF-05) permite filtrar produtos cujo saldo atual está
  menor ou igual ao estoque mínimo configurado. Não é uma entidade separada nem envolve
  notificação ativa (email/push) no MVP — é uma consulta.

## Requisitos não-funcionais

- **RNF-01 — Consistência:** saldo de produto nunca é negativo; escrita concorrente sobre o mesmo
  produto não pode resultar em saldo incorreto (ver ADR-0006).
- **RNF-02 — Auditabilidade:** toda alteração de saldo tem uma movimentação correspondente
  imutável com timestamp; não existe `UPDATE` de saldo fora desse fluxo.
- **RNF-03 — Isolamento por loja:** toda consulta/escrita é escopada por `tenantId`; dados de uma
  loja nunca aparecem em resposta de outra.
- **RNF-04 — Segurança (limitação aceita no MVP):** não há autenticação/autorização no MVP — ver
  ADR-0004. Isso é uma limitação documentada, não ausência de requisito — o requisito real
  (autenticação real por usuário/papel) está registrado no roadmap pós-MVP.
- **RNF-05 — Observabilidade mínima:** logs estruturados básicos (CloudWatch via App Runner, ver
  ADR-0007); sem exigência de tracing distribuído no MVP.
- **RNF-06 — Erros previsíveis:** toda resposta de erro da API segue RFC 7807
  (`application/problem+json`), nunca stack trace cru.
- **RNF-07 — Portabilidade de dado monetário/quantidade:** nunca usar `float`/`double` para
  dinheiro ou quantidade (ver `CLAUDE.md` regra 4).
- **RNF-08 — Custo:** operação dentro do free tier da AWS (12 meses) e da Vercel; alerta de
  orçamento configurado (ver ADR-0007).
