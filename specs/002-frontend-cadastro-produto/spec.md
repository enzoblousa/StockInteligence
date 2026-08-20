# Feature Spec: Frontend — Cadastro de Produto

**Feature ID:** 002-frontend-cadastro-produto
**Status:** Draft
**Criada em:** 2026-08-20
**Constitution base:** `memory/constitution.md` v2.2.0 (seção Frontend)
**Depende de:** `specs/001-cadastro-produto` (API já implementada e no ar)

## Contexto de negócio

Painel web para o usuário operar o catálogo de produtos sem precisar chamar
a API diretamente. Cobre a mesma superfície funcional de
`specs/001-cadastro-produto/spec.md`, agora sob a ótica de interface — a
regra de negócio em si (unicidade de SKU, transições de status, validação
de preço) já existe e é garantida pelo backend; o frontend só precisa
refletir isso de forma clara para quem está usando.

## User Stories

### US-1 — Visualizar lista de produtos (prioridade: alta)

Como usuário, quero ver uma lista dos produtos cadastrados, com filtro por
categoria e status, para ter visão geral do catálogo.

**Critérios de aceite:**
- **Given** produtos cadastrados no backend, **When** a tela de listagem é
  aberta, **Then** a tabela exibe SKU, nome, categoria e status de cada
  produto.
- **Given** um filtro de categoria e/ou status selecionado, **When**
  aplicado, **Then** a tabela mostra apenas os produtos correspondentes
  (mesmo filtro que `GET /api/produtos` já suporta).
- **Given** nenhum produto cadastrado (ou nenhum resultado para o filtro),
  **When** a tela é exibida, **Then** aparece uma mensagem de "nenhum
  produto encontrado" — nunca uma tabela vazia sem contexto.
- **Given** a API está indisponível ou retorna erro, **When** a tela tenta
  carregar, **Then** uma mensagem de erro amigável é exibida (nunca tela em
  branco ou erro cru no console).

### US-2 — Cadastrar novo produto (prioridade: alta)

Como usuário, quero preencher um formulário para cadastrar um novo produto.

**Critérios de aceite:**
- **Given** dados válidos preenchidos, **When** o formulário é submetido,
  **Then** o produto é criado e o usuário volta para a listagem, vendo o
  novo produto nela.
- **Given** um campo obrigatório vazio, **When** o formulário é submetido,
  **Then** o navegador impede o envio e destaca o campo (validação nativa).
- **Given** um SKU já em uso por outro produto ativo (o backend responde
  `409`), **When** o formulário é submetido, **Then** a mensagem de erro
  do backend é exibida e os dados já preenchidos **não** se perdem.

### US-3 — Editar produto existente (prioridade: média)

Como usuário, quero editar os dados de um produto (exceto SKU) para manter
o catálogo atualizado.

**Critérios de aceite:**
- **Given** um produto existente, **When** a edição é aberta a partir da
  listagem, **Then** o formulário aparece pré-preenchido com os dados
  atuais e o campo SKU é somente leitura (reflete FR-004 do backend).
- **Given** dados alterados válidos, **When** submetido, **Then** o
  produto é atualizado e o usuário volta para a listagem vendo os dados
  novos.

### US-4 — Inativar / reativar produto (prioridade: média)

Como usuário, quero inativar ou reativar um produto direto na listagem.

**Critérios de aceite:**
- **Given** um produto ativo, **When** o usuário confirma a ação
  "Inativar", **Then** o status muda para Inativo na tabela sem recarregar
  a página inteira.
- **Given** um produto inativo, **When** o usuário confirma "Reativar",
  **Then** o status muda para Ativo — ou, se o backend rejeitar (`409`,
  SKU em uso por outro produto ativo), a mensagem de erro é exibida e o
  status permanece Inativo na tela.

### US-5 — Feedback de erro consistente (prioridade: alta)

Como usuário, quero ver mensagens de erro claras e sempre no mesmo lugar
quando uma operação falhar, para entender o que corrigir.

**Critérios de aceite:**
- **Given** qualquer chamada à API que falhe (`400`/`404`/`409`/rede fora
  do ar), **When** o erro ocorre, **Then** uma mensagem de alerta visível é
  exibida, usando o texto vindo do backend (`{ mensagem }`) quando
  disponível, ou uma mensagem genérica caso contrário (ex.: falha de rede).

## Fora de escopo (nesta feature)

- Autenticação/login — a API ainda não expõe isso.
- TypeScript — decisão deliberada da constitution (nível júnior, foco em
  fundamentos JS).
- Gerenciamento de estado global (Redux/Zustand/Context complexo).
- Testes automatizados de frontend — candidato a uma iteração futura.
- Responsividade mobile avançada — usa o grid padrão do Bootstrap, sem
  otimização extra.
- Qualquer tela de Categoria/Unidade de Medida como cadastro próprio — são
  listas fixas, replicando a decisão já tomada no backend.

## Checklist de revisão

- [x] Sem termos de implementação (React, Axios, componente) no corpo dos
      critérios de aceite — só comportamento observável pelo usuário.
- [x] Todo critério de aceite é verificável manualmente na tela.
- [x] Nenhum `[NEEDS CLARIFICATION]` pendente — mapeamento 1:1 com as User
      Stories já validadas em `specs/001-cadastro-produto/spec.md`.
