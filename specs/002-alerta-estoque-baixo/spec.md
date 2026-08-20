# Feature Spec: Controle de Saldo de Estoque e Alerta de Estoque Baixo

**Feature ID:** 002-alerta-estoque-baixo
**Status:** Draft
**Criada em:** 2026-08-20
**Constitution base:** `memory/constitution.md` v2.1.1

## Contexto de negócio

A partir do cadastro de produto (001-cadastro-produto), o negócio precisa
saber quanto tem de cada produto em estoque e ser avisado automaticamente
quando esse saldo cai a um nível crítico, para evitar ruptura (falta do
produto para venda). Esta feature entrega o controle de saldo por produto e
o alerta automático de estoque baixo — sem nenhum detalhe de implementação
técnica, que será definido no `plan.md` correspondente.

## User Stories

### US-1 — Definir saldo inicial e quantidade mínima de um produto (prioridade: alta)

Como gestor de estoque, quero registrar quanto tenho de um produto hoje e a
partir de que quantidade considero "estoque baixo", para começar a
controlá-lo.

**Critérios de aceite:**
- **Given** um produto ativo sem saldo de estoque definido, **When** informo
  a quantidade inicial e a quantidade mínima, **Then** o saldo passa a
  existir para esse produto.
- **Given** um produto que já tem saldo de estoque definido, **When** tento
  defini-lo novamente, **Then** o sistema rejeita a operação.
- **Given** um produto inexistente, **When** tento definir o saldo, **Then**
  o sistema informa que o produto não foi encontrado.
- **Given** quantidade inicial ou quantidade mínima negativa, **When**
  submetido, **Then** o sistema rejeita a operação.

### US-2 — Registrar entrada de estoque (prioridade: alta)

Como gestor de estoque, quero registrar o recebimento/reposição de uma
quantidade de um produto, para manter o saldo atualizado.

**Critérios de aceite:**
- **Given** um saldo existente, **When** uma entrada de quantidade positiva
  é registrada, **Then** o saldo aumenta na mesma quantidade.
- **Given** um produto sem saldo definido, **When** uma entrada é tentada,
  **Then** o sistema rejeita informando que não há saldo definido.
- **Given** quantidade de entrada zero ou negativa, **When** tentado,
  **Then** o sistema rejeita a operação.

### US-3 — Registrar saída de estoque (prioridade: alta)

Como gestor de estoque, quero registrar venda/consumo/perda de uma
quantidade de um produto, mantendo o saldo atualizado e nunca negativo.

**Critérios de aceite:**
- **Given** saldo suficiente, **When** uma saída é registrada, **Then** o
  saldo diminui na mesma quantidade.
- **Given** uma quantidade de saída maior que o saldo atual, **When**
  tentado, **Then** o sistema rejeita a operação informando saldo
  insuficiente, e o saldo permanece inalterado.
- **Given** quantidade de saída zero ou negativa, **When** tentado, **Then**
  o sistema rejeita a operação.
- **Given** uma saída que faz o saldo, que estava acima da quantidade
  mínima, cair para um nível igual ou abaixo dela, **When** a saída é
  registrada com sucesso, **Then** o sistema sinaliza automaticamente um
  alerta de estoque baixo para esse produto (US-5).
- **Given** o saldo já está igual ou abaixo da quantidade mínima, **When**
  uma nova saída é registrada e o saldo continua igual ou abaixo dela,
  **Then** nenhum novo alerta é sinalizado (o alerta já foi emitido na
  transição anterior).

### US-4 — Consultar saldo de estoque (prioridade: alta)

Como usuário do sistema, quero consultar o saldo atual de um produto por
identificador ou SKU, para saber quanto há disponível e se está abaixo do
mínimo.

**Critérios de aceite:**
- **Given** um produto com saldo definido, **When** consultado por produto
  ou por SKU, **Then** o sistema retorna a quantidade atual, a quantidade
  mínima e um indicador de se o saldo está abaixo do mínimo.
- **Given** um produto sem saldo definido, **When** consultado, **Then** o
  sistema informa que não há saldo registrado.

### US-5 — Ser alertado automaticamente sobre estoque baixo (prioridade: alta)

Como gestor de estoque (ou outro sistema interessado), quero ser avisado
assim que um produto atinge nível de estoque baixo, para agir antes que
falte.

**Critérios de aceite:**
- **Given** um alerta sinalizado internamente pela saída que o originou,
  **When** essa operação é concluída com sucesso, **Then** o alerta é
  disponibilizado para quem quiser consumi-lo (quem consome o alerta e o
  que faz com ele é fora de escopo desta feature — ver feature futura).
- **Given** uma operação que sinalizaria um alerta de estoque baixo mas
  falha ou é revertida, **When** isso ocorre, **Then** nenhum alerta é
  publicado.

## Requisitos Funcionais

- **FR-001**: O sistema deve permitir definir, uma única vez por produto, um
  saldo de estoque com quantidade inicial e quantidade mínima.
- **FR-002**: O sistema deve rejeitar a definição de saldo para um produto
  que não existe.
- **FR-003**: O sistema deve rejeitar a redefinição de saldo para um produto
  que já tem saldo de estoque.
- **FR-004**: O sistema deve permitir registrar entrada de estoque, somando
  a quantidade informada ao saldo atual do produto.
- **FR-005**: O sistema deve permitir registrar saída de estoque, subtraindo
  a quantidade informada do saldo atual do produto.
- **FR-006**: O sistema deve rejeitar uma saída cuja quantidade seja maior
  que o saldo atual do produto, mantendo o saldo inalterado.
- **FR-007**: O sistema deve rejeitar quantidades de entrada/saída e
  quantidade inicial/mínima negativas; quantidades de entrada/saída também
  devem ser maiores que zero.
- **FR-008**: O sistema deve sinalizar automaticamente um alerta de estoque
  baixo quando uma saída faz o saldo de um produto cruzar a transição de
  "acima da quantidade mínima" para "igual ou abaixo dela" — não a cada
  saída subsequente enquanto o saldo permanecer baixo.
- **FR-009**: O sistema não deve publicar um alerta de estoque baixo para
  uma operação que não foi concluída com sucesso.
- **FR-010**: O sistema deve permitir consultar o saldo de estoque de um
  produto por identificador ou por SKU, incluindo um indicador de se está
  abaixo da quantidade mínima.
- **FR-011**: O saldo de estoque é único e global por produto — sem suporte
  a múltiplos depósitos/localizações nesta feature.

## Entidades-chave (linguagem de negócio)

- **Saldo de Estoque** — produto associado, quantidade atual, quantidade
  mínima (limiar que dispara o alerta).
- **Alerta de Estoque Baixo** — fato de negócio: produto, quantidade atual
  no momento, quantidade mínima, quando ocorreu.

## Fora de escopo (nesta feature)

- Múltiplos depósitos/localizações (saldo é global por produto).
- Edição posterior da quantidade mínima definida em US-1.
- Histórico/auditoria de cada movimentação individual — apenas o saldo
  consolidado é mantido.
- Quem consome o alerta de estoque baixo e o que faz com ele (ex.: e-mail,
  notificação push, painel de acompanhamento) — feature futura.
- Reposição automática de estoque.
- Controle de lote/validade.
- Listagem agregada de "todos os produtos abaixo do mínimo" — candidata a
  uma User Story futura, não incluída aqui (YAGNI).

## Checklist de revisão

- [x] Sem termos de implementação (framework, banco, mensageria) no corpo
      da spec.
- [x] Todos os critérios de aceite são verificáveis.
- [x] Toda ambiguidade identificada nesta rodada foi resolvida com o
      usuário: semântica do alerta é "só na transição" (US-3, FR-008),
      confirmada explicitamente antes da implementação; nenhum
      `[NEEDS CLARIFICATION]` pendente.
