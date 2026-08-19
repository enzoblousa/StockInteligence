# ADR-0006: Controle de concorrência otimista no saldo de estoque

Status: **Aceito** · Data: 2026-08-19

## Contexto

`Produto.saldoAtual` é atualizado toda vez que uma `MovimentoEstoque` é registrada. Duas
requisições concorrentes sobre o mesmo produto (ex.: duas saídas simultâneas) não podem resultar
em saldo inconsistente ("lost update") nem em saldo negativo — isso é o núcleo da proposta de
valor do produto ("saldo sempre consistente", ver `docs/spec/00-vision.md`).

## Decisão

- `Produto` carrega uma coluna `version` (`@Version` do JPA) — controle de concorrência
  **otimista**.
- O caso de uso de registrar movimentação (`application`) lê o `Produto`, calcula o novo saldo,
  e grava `Produto` + `MovimentoEstoque` na mesma transação. Se outra transação alterou o
  `Produto` entre a leitura e a escrita, o commit falha com `OptimisticLockException`.
- Esse conflito é mapeado para **HTTP 409 Conflict** (RFC 7807) na API — o cliente decide se
  tenta de novo (retry idempotente: reenviar a mesma intenção de movimentação).
- Validação de saldo insuficiente (`SAIDA` maior que `saldoAtual`) é checada **dentro** da mesma
  transação, depois da leitura com lock otimista — nunca "ler, validar em memória, gravar depois"
  sem essa proteção.

## Alternativas consideradas

- **Lock pessimista (`SELECT ... FOR UPDATE`):** rejeitada como padrão — o volume de escrita
  concorrente esperado é baixo (uma loja pequena, poucos operadores simultâneos); lock otimista
  evita segurar lock de linha por mais tempo que o necessário e é mais simples de testar. Fica
  documentado como alternativa se, na prática, a taxa de conflito/retry se mostrar alta.
- **Recalcular saldo sempre a partir do ledger completo (`SUM` de movimentos)** em vez de manter
  `saldoAtual` desnormalizado: eliminaria a necessidade de lock, mas piora performance de leitura
  (toda consulta de saldo vira agregação) e não elimina a corrida na escrita — duas inserções
  concorrentes ainda podem violar "nunca negativo" sem alguma forma de serialização. Pode ser
  revisitado pós-MVP se auditoria mais profunda exigir.
- **Sem controle de concorrência:** rejeitado — quebra diretamente a garantia central do produto.

## Consequências

- Testes de integração incluem cenário explícito de concorrência (duas movimentações simultâneas
  no mesmo produto), ver `docs/spec/03-architecture.md` § estratégia de testes.
- Frontend precisa tratar 409 de forma amigável (ex.: "outra pessoa alterou este produto,
  atualize e tente de novo"), não como erro genérico.
