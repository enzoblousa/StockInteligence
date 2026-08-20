# Estratégia de Testes

**Complementa:** `memory/constitution.md` (Princípio V — Testabilidade)

Regra central: **cada regra de negócio tem exatamente uma camada dona da sua
cobertura exaustiva.** As demais camadas testam apenas o que é exclusivo
delas — nunca reafirmam o mesmo cenário de negócio já provado em outro nível.
Duplicar cobertura entre camadas não aumenta confiança de forma proporcional
ao custo de manutenção que adiciona; aumenta o custo de mudar qualquer regra
(N lugares pra atualizar) sem reduzir risco real.

## Quem é dono de quê

| Camada | Dona de | NÃO deve testar |
|---|---|---|
| **Domínio** (unit, puro — VOs e agregado) | Toda regra de negócio em si: formato/limites de VO, invariantes do agregado, toda transição de status válida e inválida. É a fonte única de verdade — cobertura exaustiva mora aqui. | Nada de infraestrutura (óbvio, domínio não a conhece). |
| **CommandHandler** (unit, repositório mockado) | Apenas orquestração que depende do repositório: "já existe SKU ativo" → rejeita; "produto não encontrado" → rejeita. Um caso feliz por handler. | Não re-testa formato de SKU/preço nem toda transição de status — isso já está provado no domínio; um único caso de "dado inválido propaga exceção de domínio" basta como smoke test. |
| **Repositório** (`@QuarkusTest`, banco real) | O que só o banco prova: round-trip save/load, constraint de unicidade parcial (`uq_produto_sku_ativo`), corretude de query/paginação/filtro do read side. | Não re-testa regra de negócio (isso é papel do domínio) nem orquestração (isso é papel do handler). |
| **REST Resource** (`@QuarkusTest` + RestAssured) | Só a fiação HTTP: request→Command/Query correto, status code certo por família de erro (`400`/`404`/`409`), formato do response. Um caso feliz por endpoint + **um** exemplo de cada família de erro no total (o `ExceptionMapper` é único e compartilhado — não precisa ser provado endpoint por endpoint). | Não repete a matriz de Given/When/Then de `spec.md` — essa matriz já foi validada em domínio + handler. |

## Como aplicar isso a uma nova feature

Ao escrever `tasks.md`, para cada regra de negócio pergunte: "essa regra já
tem uma camada dona?" Se sim, a task na camada de cima é um *smoke test* (1
caso), não uma reexecução do mesmo conjunto de casos.

## Efeito prático em `001-cadastro-produto`

Aplicado nesta feature, o `ProdutoResource` (`@QuarkusTest`) deixou de
reimplementar os 5 User Stories completos e passou a cobrir só: 1 caso feliz
por endpoint (7) + 1 caso `400` + 1 caso `404` + 1 caso `409` — a matriz de
regra de negócio em si continua exaustiva, mas mora só em domínio e
handlers.
