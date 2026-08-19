# 00 — Visão do Produto

Status: **Aceito** · Última revisão: 2026-08-18

## Nome de trabalho

**StockPilot** — placeholder. Trocar aqui é suficiente; nada no código depende do nome ainda.

## Problema

Pequenas e médias empresas que revendem produtos físicos frequentemente controlam estoque em
planilhas, o que causa: saldo desatualizado, vendas de produto que já acabou (overselling),
falta de rastreabilidade de quem mexeu no estoque e quando, e nenhuma visibilidade sobre quando
comprar mais (ruptura de estoque).

## Proposta de valor

Um sistema único onde **pedidos de compra e venda são a única forma de mexer no estoque de forma
controlada** (além de ajustes manuais auditados), com saldo sempre consistente, rastreável e
alertas automáticos de estoque baixo — eliminando a divergência entre "o que a planilha diz" e
"o que tem na prateleira".

## Por que este projeto (contexto de portfólio)

Este é um projeto de estudo/portfólio para demonstrar competência de engenharia nível sênior:

- Modelagem de domínio não-trivial (agregados, invariantes, concorrência).
- Arquitetura backend limpa (hexagonal) em Quarkus, não só CRUD.
- Segurança real (OIDC/RBAC), não autenticação de brinquedo.
- Qualidade de engenharia: testes em camadas, observabilidade, CI/CD, deploy real.
- Frontend desacoplado consumindo um contrato de API tipado.

As decisões de escopo abaixo priorizam **profundidade em um recorte pequeno** em vez de
amplitude rasa (não é para virar um ERP completo).

## Personas

- **Admin** — configura catálogo, parceiros, usuários e papéis.
- **Estoquista/Operador** — recebe mercadoria (compras), faz ajustes de estoque, consulta saldo.
- **Vendedor** — cria e confirma pedidos de venda.
- **Gestor** — acompanha relatórios (estoque baixo, giro, histórico) mas não opera o dia a dia.

## Objetivos (goals) do MVP

1. Cadastro de produtos, categorias e parceiros (fornecedores/clientes).
2. Estoque com saldo por produto, movimentações imutáveis e auditáveis, ajustes manuais.
3. Fluxo de compra: pedido de compra → recebimento → entrada de estoque.
4. Fluxo de venda: pedido de venda → confirmação (reserva) → faturamento (baixa) → cancelamento
   (libera reserva), com bloqueio de venda sem saldo disponível.
5. Alerta de estoque abaixo do mínimo.
6. Autenticação/autorização real via Keycloak com papéis distintos.
7. Deploy acessível publicamente (link vivo no portfólio).

## Não-objetivos (explicitamente fora do MVP)

- Emissão fiscal (NF-e/NFC-e) e integrações tributárias.
- Múltiplos depósitos/lojas e rastreio por lote/validade (fica como fase futura — ver roadmap M7).
- Financeiro/contas a pagar-receber, conciliação bancária.
- Integração com marketplaces/e-commerce.
- Precificação dinâmica, promoções, cupons.
- Mobile app nativo.
- Multi-tenant (uma instância = uma empresa).

## Critérios de sucesso

- Todo o fluxo compra → estoque → venda → estoque funciona fim a fim, com dados consistentes sob
  concorrência (dois vendedores tentando vender o último item não geram saldo negativo).
- Sistema publicamente acessível via URL, com login funcional.
- Cobertura de teste sólida no domínio (compras/vendas/estoque) e pipeline de CI verde.
- Documentação (esta spec) reflete fielmente o que está implementado a cada marco fechado.
