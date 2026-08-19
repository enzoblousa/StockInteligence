# 00 — Visão do Produto

Status: **Aceito** · Última revisão: 2026-08-19

## Nome de trabalho

**Stock Master** — controlador de estoque para pequenos lojistas.

## Problema

Microempreendedores e lojistas de pequeno porte que revendem produtos físicos costumam
controlar estoque em planilhas ou de cabeça, o que causa: saldo desatualizado, vendas de produto
que já acabou (overselling), falta de rastreabilidade de quem mexeu no estoque e quando, e nenhuma
visibilidade sobre quando comprar mais (ruptura de estoque). Esse público não tem equipe de TI
nem orçamento para um ERP tradicional — precisa de algo simples de operar, mas confiável no dado.

## Proposta de valor

Um sistema onde **toda movimentação de estoque (entrada, saída, ajuste) é registrada de forma
imutável e auditável**, com saldo sempre consistente (nunca fica negativo, mesmo sob escrita
concorrente — ver ADR-0006) e alerta de estoque baixo por limite configurável por produto —
eliminando a divergência entre "o que a planilha diz" e "o que tem na prateleira". Cada loja
(`Tenant`) tem seu próprio catálogo e estoque, isolados desde o início.

## Por que este projeto (contexto de portfólio)

Projeto de estudo/portfólio para demonstrar competência de engenharia, com esforço deliberadamente
concentrado em duas frentes:

- **Backend com arquitetura sólida**: modelagem de domínio (invariantes, concorrência),
  organização hexagonal-lite em Quarkus (ADR-0002), qualidade de engenharia (testes em camadas,
  migrations versionadas).
- **Infraestrutura cloud real**: deploy em AWS free tier com infraestrutura como código
  (Terraform) — ver ADR-0007.

O **frontend é intencionalmente simples**: poucas telas, o suficiente para demonstrar consumo
correto de um contrato de API tipado — não é o foco de profundidade do projeto (ADR-0005).

## Escopo do MVP

**Dentro do MVP:**
- Cadastro de loja (`Tenant`) e de produtos por loja.
- Registro de movimentações de estoque (entrada / saída / ajuste), unitárias por produto.
- Saldo de estoque sempre consistente (nunca negativo), protegido contra escrita concorrente.
- Listagem de produtos com destaque/filtro de estoque abaixo do mínimo configurado.
- Histórico de movimentações por produto.
- Deploy público funcional em AWS free tier + frontend na Vercel.

**Fora do MVP (não-objetivos declarados):**
- **Autenticação/autorização** — decisão explícita e documentada como risco aceito (ADR-0004),
  primeira prioridade do pós-MVP.
- **Pedidos multi-item** (documento de compra/venda com várias linhas) — o MVP registra
  movimentações unitárias por produto, não pedidos compostos.
- Relatórios avançados, previsão de demanda, IA.
- Multi-moeda, app mobile, integração com hardware (leitor de código de barras, etc.).

## Persona (MVP)

Lojista/operador de uma loja pequena — persona única no MVP, sem diferenciação de papel/permissão
(consequência direta de não haver autenticação ainda, ver ADR-0004).

## Critério de sucesso do MVP

Sistema publicamente acessível (URL real, AWS + Vercel), permitindo: criar loja, cadastrar
produto, registrar movimentações de estoque, observar saldo correto mesmo com escritas
concorrentes (validado por teste de integração dedicado), e visualizar produtos abaixo do
estoque mínimo.
