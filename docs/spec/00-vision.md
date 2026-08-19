# 00 — Visão do Produto

Status: **Aceito** · Última revisão: 2026-08-19

## Nome de trabalho

**Stock Master** — controle de estoque inteligente. Nada no código depende do nome ainda; trocar
aqui é suficiente.

## Problema

Microempreendedores e lojistas de pequeno/médio porte que revendem produtos físicos costumam
controlar estoque em planilhas ou de cabeça, o que causa: saldo desatualizado, vendas de produto
que já acabou (overselling), falta de rastreabilidade de quem mexeu no estoque e quando, e nenhuma
visibilidade sobre quando comprar mais (ruptura de estoque). Diferente de uma grande rede, esse
público não tem equipe de TI nem orçamento para um ERP tradicional — precisa de algo simples de
operar no dia a dia, mas confiável no dado.

## Proposta de valor

Um sistema onde **pedidos de compra e venda são a única forma de mexer no estoque de forma
controlada** (além de ajustes manuais auditados), com saldo sempre consistente, rastreável e
alertas automáticos de estoque baixo — eliminando a divergência entre "o que a planilha diz" e "o
que tem na prateleira". Pensado desde o início para operação de baixo atrito: poucos cliques,
poucas telas, papéis claros por pessoa da equipe.

## Por que este projeto (contexto de portfólio)

Este é um projeto de estudo/portfólio para demonstrar competência de engenharia nível sênior,
com o esforço deliberadamente concentrado em duas frentes:

- **Backend com arquitetura sólida**: modelagem de domínio não-trivial (agregados, invariantes,
  concorrência), arquitetura hexagonal em Quarkus, segurança real (OIDC/RBAC), qualidade de
  engenharia (testes em camadas, observabilidade, CI/CD).
- **Infraestrutura cloud real**: deploy em AWS com infraestrutura como código (Terraform),
  pipeline de CI/CD reproduzível e publicado — ver ADR-0009.

O **frontend é intencionalmente simples**: poucas telas, o suficiente para demonstrar consumo
correto de um contrato de API tipado (TanStack Query, formulários validados) e boas práticas de
UI desacoplada — não é o foco de profundidade do projeto, e isso é uma escolha deliberada, não uma
limitação.

As decisões de escopo abaixo priorizam **profundidade em um recorte pequeno** em vez de amplitude
rasa (não é para virar um ERP completo).

## Visão de futuro (pós-MVP, direção declarada mas não implementada)

O nome "Stock Master" já aponta para onde o produto quer chegar depois do MVP — registrado aqui
para orientar decisões de arquitetura que **não travem** essa evolução, sem implementá-la agora
(evitar overengineering prematuro):

- **IA**: previsão de demanda e sugestão de ponto de reposição a partir do histórico de
  movimentações (já modelado como imutável/auditável desde o MVP — ver `02-domain-model.md`).
- **IoT**: leitores de código de barras/RFID e câmeras para contagem e baixa automática de
  estoque, publicando eventos que entram no sistema pela mesma porta (`InventoryPort`) usada por
  compras/vendas hoje — ver nota em `03-architecture.md`. AWS IoT Core é o candidato natural de
  integração (ver ADR-0009).

Essas features ficam fora do roadmap M0–M6 (MVP) e são tratadas como M8/stretch — ver
`05-roadmap.md`.

## Personas

- **Admin/Dono do negócio** — configura catálogo, parceiros, usuários e papéis; em uma
  microempresa, frequentemente é a mesma pessoa que opera o estoque no dia a dia.
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
7. Deploy acessível publicamente em AWS (link vivo no portfólio) com infraestrutura como código.

## Não-objetivos (explicitamente fora do MVP)

- IA (previsão de demanda) e IoT (leitores/câmeras) — ver "Visão de futuro" acima; fica para M8.
- Emissão fiscal (NF-e/NFC-e) e integrações tributárias.
- Múltiplos depósitos/lojas e rastreio por lote/validade (fica como fase futura — ver roadmap M7).
- Financeiro/contas a pagar-receber, conciliação bancária.
- Integração com marketplaces/e-commerce.
- Precificação dinâmica, promoções, cupons.
- Mobile app nativo.
- Multi-tenant (uma instância = uma empresa).
- Frontend com muitas telas/polimento visual de produto — cobre o suficiente para operar o
  fluxo, não uma experiência de produto completa (ver "por que este projeto" acima).

## Critérios de sucesso

- Todo o fluxo compra → estoque → venda → estoque funciona fim a fim, com dados consistentes sob
  concorrência (dois vendedores tentando vender o último item não geram saldo negativo).
- Sistema publicamente acessível via URL, com login funcional, rodando em infraestrutura AWS
  provisionada via Terraform (não configuração manual no console).
- Cobertura de teste sólida no domínio (compras/vendas/estoque) e pipeline de CI verde.
- Documentação (esta spec) reflete fielmente o que está implementado a cada marco fechado.
