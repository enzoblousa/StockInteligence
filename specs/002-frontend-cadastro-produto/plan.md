# Implementation Plan: Frontend — Cadastro de Produto

**Feature ID:** 002-frontend-cadastro-produto
**Spec:** `specs/002-frontend-cadastro-produto/spec.md`
**Constitution:** `memory/constitution.md` v2.2.0 (seção Frontend)
**Backend consumido:** `specs/001-cadastro-produto/plan.md` (7 endpoints REST)

## Resumo técnico

SPA React 18 + Vite, 3 rotas via React Router: listagem (com filtro),
criar e editar (mesmo componente de formulário, reaproveitado por modo).
Toda chamada HTTP passa por uma camada de serviço única
(`produtoService.js`) sobre um client Axios centralizado. Cada página
gerencia seu próprio estado de `loading`/`erro`/`dados` via `useState` +
`useEffect` — sem lib de estado global, conforme constitution.

## Constitution Check (seção Frontend)

| Convenção | Conformidade |
|---|---|
| JavaScript (sem TS) | ✅ Todo o projeto em `.jsx`/`.js`. |
| Client Axios único + `baseURL` via env | ✅ `src/api/client.js`, lê `VITE_API_BASE_URL`. |
| Camada de serviço, sem Axios direto em componente | ✅ `src/api/produtoService.js` é o único lugar que importa `client.js`. |
| Tratamento uniforme de erro | ✅ `produtoService.js` normaliza erro do Axios num formato único; componente `Alerta` exibe. |
| Sem estado global desnecessário | ✅ `useState`/`useEffect` por página, nenhuma lib de state management. |

Nenhum desvio a registrar em Complexity Tracking.

## Estrutura de pastas

```
frontend/
  index.html
  vite.config.js
  package.json
  .env.example              # VITE_API_BASE_URL=http://localhost:8080
  src/
    main.jsx
    App.jsx                  # <BrowserRouter> + rotas
    api/
      client.js               # instância Axios (baseURL, headers)
      produtoService.js        # 7 funções, uma por endpoint; normaliza erro
    constants/
      produtoOptions.js        # Categoria/UnidadeMedida/Status (espelha o backend)
    pages/
      ProdutoListPage.jsx       # US-1, US-4 (ações inline na tabela)
      ProdutoFormPage.jsx       # US-2 (criar) e US-3 (editar), mesmo componente
    components/
      ProdutoTable.jsx          # tabela + botões de ação por linha
      FiltroProdutos.jsx        # selects de categoria/status
      Alerta.jsx                # mensagem de erro reutilizável (US-5)
      Carregando.jsx            # spinner/estado de loading
```

## Rotas (React Router)

| Rota | Página | User Story |
|---|---|---|
| `/` | redirect → `/produtos` | — |
| `/produtos` | `ProdutoListPage` | US-1, US-4 |
| `/produtos/novo` | `ProdutoFormPage` (modo `criar`) | US-2 |
| `/produtos/:id/editar` | `ProdutoFormPage` (modo `editar`) | US-3 |

## Mapeamento tela → endpoint backend

| Ação na tela | Endpoint |
|---|---|
| Carregar listagem (com filtro) | `GET /api/produtos?categoria=&status=` |
| Submeter formulário de criação | `POST /api/produtos` |
| Carregar dados para edição | `GET /api/produtos/{id}` |
| Submeter formulário de edição | `PUT /api/produtos/{id}` |
| Botão "Inativar" na tabela | `PATCH /api/produtos/{id}/inativar` |
| Botão "Reativar" na tabela | `PATCH /api/produtos/{id}/reativar` |

## Decisões técnicas

- **Categoria/UnidadeMedida/Status replicados em `produtoOptions.js`**: o
  backend não expõe um endpoint de metadados de enum, e criar um só pra
  isso seria over-engineering nesta escala. Duplicação pequena e estável
  (mesmos valores de `specs/001-cadastro-produto/plan.md` § Decisões
  técnicas) — se o backend mudar os valores, este arquivo precisa
  acompanhar manualmente. Aceito conscientemente.
- **Confirmação de inativar/reativar via `window.confirm` nativo** — sem
  modal customizado nesta primeira versão (YAGNI/nível júnior).
- **Validação de formulário via atributos HTML5** (`required`,
  `type="number"`, `min="0"`) — sem lib de validação (Formik/Yup) por
  enquanto; suficiente para os critérios de aceite da spec.
- **`ProdutoFormPage` único para criar e editar**: evita duplicar
  marcação/lógica de formulário; o modo é decidido pela presença de `:id`
  na rota. Campo SKU é renderizado como somente-leitura quando em modo
  editar.
- **Erro de rede vs. erro de negócio**: `produtoService.js` distingue os
  dois casos — se a resposta tem corpo `{ mensagem }` (erro de negócio do
  backend), usa esse texto; caso contrário (timeout, servidor fora do ar),
  usa uma mensagem genérica fixa. Isso implementa US-5 num único lugar, não
  espalhado por página.

## Quickstart

```bash
cd frontend
npm install
cp .env.example .env   # ajustar VITE_API_BASE_URL se o backend não estiver em localhost:8080
npm run dev
```

Requer o backend (`specs/001-cadastro-produto`) rodando — ver
`specs/001-cadastro-produto/plan.md` § Quickstart.

## Fase seguinte

`tasks.md` — a ser gerado depois da revisão deste `plan.md` e do `spec.md`.

## Descobertas durante a implementação

- **CORS não estava configurado no backend.** Os testes do backend
  (RestAssured) e a validação manual via `curl` do T040 de
  `specs/001-cadastro-produto` não capturam isso — nenhum dos dois passa
  por um navegador, que é o único cliente que de fato aplica a política de
  mesma origem. Só apareceu ao validar esta feature num browser real
  (`mcp__claude-in-chrome`), via T019. Corrigido em
  `backend/src/main/resources/application.properties`
  (`quarkus.http.cors.enabled=true` + `origins`/`methods`/`headers`) —
  ver comentário no próprio arquivo sobre a renomeação de
  `quarkus.http.cors` para `quarkus.http.cors.enabled` em versões recentes
  do Quarkus, que causava um aviso silencioso de "unrecognized
  configuration key" em vez de erro.
- **Lição para o processo**: validação de integração front↔back precisa
  incluir pelo menos um teste (manual ou automatizado) via navegador real
  — testes de API isolados (curl/RestAssured) não pegam CORS.

## Complexity Tracking

Nenhum desvio da constitution nesta feature.
