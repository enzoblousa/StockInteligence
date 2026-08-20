# Feature Spec: Cadastro de Produto

**Feature ID:** 001-cadastro-produto
**Status:** Draft
**Criada em:** 2026-08-20
**Constitution base:** `memory/constitution.md` v2.0.1

## Contexto de negócio

O cadastro de produto é a base sobre a qual todas as demais operações de
estoque (movimentação, consulta de saldo, reposição) vão atuar. Sem um
produto cadastrado, não há o que controlar. Esta feature entrega o CRUD de
negócio de produto — sem nenhum detalhe de implementação técnica, que será
definido no `plan.md` correspondente.

## User Stories

### US-1 — Cadastrar novo produto (prioridade: alta)

Como gestor de estoque, quero cadastrar um novo produto informando seus dados
básicos, para que ele passe a existir no sistema e possa ser movimentado.

**Critérios de aceite:**
- **Given** um SKU, nome, categoria, unidade de medida e preço de custo/venda
  válidos, **When** o produto é cadastrado, **Then** o produto passa a existir
  com status **Ativo** e um identificador único gerado pelo sistema.
- **Given** um SKU já usado por outro produto **Ativo**, **When** o cadastro é
  tentado, **Then** o sistema rejeita a operação informando que o SKU já está
  em uso.
- **Given** ausência de Nome ou SKU (campos obrigatórios), **When** o cadastro
  é tentado, **Then** o sistema rejeita a operação apontando o(s) campo(s)
  faltante(s).
- **Given** preço de custo ou preço de venda negativo, **When** o cadastro é
  tentado, **Then** o sistema rejeita a operação.

### US-2 — Consultar produto (prioridade: alta)

Como usuário do sistema, quero consultar um produto específico pelo seu
identificador ou SKU, para verificar seus dados cadastrais atuais.

**Critérios de aceite:**
- **Given** um produto existente, **When** consultado por ID ou SKU, **Then**
  o sistema retorna todos os seus dados cadastrais e o status atual.
- **Given** um identificador/SKU inexistente, **When** consultado, **Then** o
  sistema informa que o produto não foi encontrado.

### US-3 — Listar produtos (prioridade: alta)

Como gestor de estoque, quero listar os produtos cadastrados, podendo
filtrar por categoria e por status, para ter visão geral do catálogo.

**Critérios de aceite:**
- **Given** produtos cadastrados, **When** a listagem é solicitada sem
  filtro, **Then** o sistema retorna todos os produtos (paginados).
- **Given** um filtro de categoria e/ou status, **When** a listagem é
  solicitada, **Then** o sistema retorna apenas os produtos que atendem ao
  filtro.

### US-4 — Editar dados cadastrais de um produto (prioridade: média)

Como gestor de estoque, quero atualizar nome, categoria, unidade de medida
e/ou preços de um produto existente, para manter o cadastro correto.

**Critérios de aceite:**
- **Given** um produto existente, **When** seus dados editáveis são
  atualizados com valores válidos, **Then** o sistema salva as alterações e
  preserva o mesmo identificador e SKU.
- **Given** uma tentativa de alterar o SKU, **When** a edição é submetida,
  **Then** o sistema rejeita a alteração — **SKU é imutável** após a criação
  (decisão de negócio confirmada).
- **Given** preço de custo ou venda negativo na edição, **When** submetido,
  **Then** o sistema rejeita a operação.

### US-5 — Inativar / reativar produto (prioridade: média)

Como gestor de estoque, quero inativar um produto que não é mais
comercializado, sem perder o histórico de movimentações associado a ele, e
poder reativá-lo depois se necessário.

**Critérios de aceite:**
- **Given** um produto **Ativo**, **When** inativado, **Then** seu status
  passa a **Inativo** e ele deixa de aparecer nas listagens padrão (sem
  filtro explícito de status), mas seu histórico permanece intacto.
- **Given** um produto **Inativo**, **When** reativado, **Then** seu status
  volta a **Ativo**, desde que nenhum outro produto **Ativo** esteja usando o
  mesmo SKU no momento da reativação.
- Exclusão física de produto **não é suportada** nesta feature — apenas
  inativação lógica (decisão de negócio confirmada).

## Requisitos Funcionais

- **FR-001**: O sistema deve permitir o cadastro de um produto com: Nome
  (obrigatório), SKU (obrigatório, único entre produtos ativos), Categoria,
  Unidade de Medida, Preço de Custo e Preço de Venda.
- **FR-002**: O sistema deve gerar um identificador único para cada produto
  no momento do cadastro.
- **FR-003**: O sistema deve impedir dois produtos **Ativos** com o mesmo
  SKU.
- **FR-004**: O SKU de um produto não pode ser alterado após o cadastro.
- **FR-005**: O sistema deve permitir consulta de um produto por
  identificador ou por SKU.
- **FR-006**: O sistema deve permitir listagem de produtos com filtro
  opcional por categoria e por status, com paginação.
- **FR-007**: O sistema deve permitir a edição de Nome, Categoria, Unidade de
  Medida, Preço de Custo e Preço de Venda de um produto existente.
- **FR-008**: O sistema deve permitir inativar um produto **Ativo** e
  reativar um produto **Inativo**, respeitando a unicidade de SKU entre
  produtos ativos (FR-003).
- **FR-009**: O sistema deve rejeitar preços de custo/venda negativos, tanto
  no cadastro quanto na edição.
- **FR-010**: O sistema não deve permitir exclusão física de produtos.

## Entidades-chave (linguagem de negócio)

- **Produto** — identificador único, SKU (imutável), nome, categoria,
  unidade de medida, preço de custo, preço de venda, status (Ativo/Inativo).
- **Categoria** — valor de uma lista pré-definida do sistema (não é um
  cadastro à parte nesta fase). Lista de valores concretos a ser definida em
  `data-model.md` durante o `plan.md` desta feature.
- **Unidade de Medida** — valor de uma lista pré-definida do sistema (ex.:
  UN, KG, L, CX). Lista de valores concretos a ser definida em
  `data-model.md` durante o `plan.md` desta feature.

## Fora de escopo (nesta feature)

- Movimentação de estoque (entrada/saída) e saldo por SKU/depósito —
  feature futura, que consome o cadastro de produto criado aqui.
- Cadastro dinâmico de categorias e unidades de medida como entidades
  próprias — considerado fora do MVP; pode ser revisitado se o negócio
  exigir listas configuráveis pelo usuário.
- Controle de múltiplos depósitos/localizações — tratado na feature de
  movimentação de estoque.
- Precificação avançada (promoções, tabelas de preço por cliente).

## Checklist de revisão

- [x] Sem termos de implementação (framework, banco, API) no corpo da spec.
- [x] Todos os critérios de aceite são verificáveis.
- [x] Toda ambiguidade identificada nesta rodada foi resolvida com o
      usuário (registrado acima); nenhum `[NEEDS CLARIFICATION]` pendente
      no nível de regra de negócio — apenas os valores concretos de
      Categoria/Unidade de Medida ficam para o `plan.md`.
