# Tasks: Frontend — Cadastro de Produto

**Input:** `spec.md`, `plan.md`
**Convenção:** `[P]` = pode ser executada em paralelo com outras `[P]` da
mesma fase (arquivos distintos, sem dependência entre si). Fases são
sequenciais entre si.

---

## Fase 1 — Setup do projeto

- [x] **T001** Criar o projeto com Vite (`npm create vite@latest frontend -- --template react`)
  dentro da raiz do repositório, ao lado de `backend/`.
- [x] **T002** Instalar dependências: `axios`, `react-router-dom`, `bootstrap`,
  `react-bootstrap`.
- [x] **T003** Criar a estrutura de pastas de `plan.md`: `src/api`,
  `src/constants`, `src/pages`, `src/components`.
- [x] **T004** `[P]` Criar `.env.example` com `VITE_API_BASE_URL=http://localhost:8080`
  e o `.env` local correspondente (git-ignorado).
- [x] **T005** `[P]` Importar o CSS do Bootstrap em `main.jsx`.

**Checkpoint:** `npm run dev` sobe a tela padrão do Vite sem erro.

---

## Fase 2 — Camada de API (`plan.md` › Mapeamento tela → endpoint)

- [x] **T006** `[P]` Criar `src/api/client.js`: instância Axios com `baseURL`
  lida de `import.meta.env.VITE_API_BASE_URL`.
- [x] **T007** `[P]` Criar `src/constants/produtoOptions.js` com os arrays
  `CATEGORIAS`, `UNIDADES_MEDIDA`, `STATUS` (mesmos valores de
  `specs/001-cadastro-produto/plan.md` § Decisões técnicas).
- [x] **T008** Criar `src/api/produtoService.js` com as 7 funções (uma por
  endpoint): `listar(filtros)`, `buscarPorId(id)`, `criar(dados)`,
  `atualizar(id, dados)`, `inativar(id)`, `reativar(id)`. Cada função captura
  erro do Axios e relança um objeto normalizado `{ mensagem }` — usando a
  mensagem do backend quando presente (`error.response.data.mensagem`) ou
  uma mensagem genérica de falha de rede caso contrário (US-5).

**Checkpoint:** `produtoService.js` é o único arquivo do projeto que importa
`client.js`.

---

## Fase 3 — Componentes reutilizáveis

- [x] **T009** `[P]` Criar `src/components/Alerta.jsx`: recebe uma mensagem
  de erro e a exibe (`Alert` do react-bootstrap, variante `danger`);
  renderiza `null` quando não há mensagem.
- [x] **T010** `[P]` Criar `src/components/Carregando.jsx`: spinner simples
  (`Spinner` do react-bootstrap).
- [x] **T011** `[P]` Criar `src/components/FiltroProdutos.jsx`: dois
  `<select>` (categoria, status) usando `produtoOptions.js`, chamando um
  callback `onFiltrar({ categoria, status })` ao mudar.
- [x] **T012** Criar `src/components/ProdutoTable.jsx`: tabela com
  SKU/nome/categoria/status + botões "Editar", "Inativar"/"Reativar"
  (conforme o status da linha) — recebe a lista de produtos e os callbacks
  de ação via props, sem chamar a API diretamente.

**Checkpoint:** componentes renderizam isolados (dados via props mockadas),
sem nenhuma chamada de API dentro deles.

---

## Fase 4 — Páginas (`spec.md` › User Stories)

- [x] **T013** Criar `src/pages/ProdutoListPage.jsx` (US-1): estado
  `produtos`/`carregando`/`erro`; `useEffect` chama
  `produtoService.listar(filtros)` ao montar e quando o filtro muda; usa
  `FiltroProdutos`, `ProdutoTable`, `Alerta`, `Carregando`; exibe mensagem
  de "nenhum produto encontrado" quando a lista vier vazia.
- [x] **T014** Implementar as ações "Inativar"/"Reativar" em
  `ProdutoListPage` (US-4): `window.confirm` antes de chamar
  `produtoService.inativar`/`reativar`; atualiza o produto na lista local
  sem recarregar a página inteira; em caso de erro (`409`), exibe via
  `Alerta` e mantém o status anterior na tela.
- [x] **T015** Criar `src/pages/ProdutoFormPage.jsx` (US-2 e US-3): lê
  `:id` da rota via `useParams` para decidir o modo (`criar` quando ausente,
  `editar` quando presente); em modo editar, `useEffect` chama
  `produtoService.buscarPorId(id)` para pré-preencher o formulário e
  renderiza o campo SKU como somente-leitura; em modo criar, todos os
  campos ficam editáveis, incluindo SKU.
- [x] **T016** Implementar o submit de `ProdutoFormPage`: validação HTML5
  (`required`, `type="number"`, `min="0"`) nos campos; chama
  `produtoService.criar` ou `.atualizar` conforme o modo; em sucesso,
  navega de volta para `/produtos`; em erro, exibe via `Alerta` **sem**
  limpar os campos já preenchidos (US-2).

**Checkpoint:** as duas páginas funcionam contra o backend real (não mockado).

---

## Fase 5 — Roteamento

- [x] **T017** Configurar `src/App.jsx` com `BrowserRouter` e `Routes`:
  `/` → redirect para `/produtos`; `/produtos` → `ProdutoListPage`;
  `/produtos/novo` → `ProdutoFormPage`; `/produtos/:id/editar` →
  `ProdutoFormPage`.
- [x] **T018** `[P]` Adicionar navegação: link/botão "Novo produto" na
  listagem (→ `/produtos/novo`) e botão "Voltar" no formulário (→
  `/produtos`).

**Checkpoint:** as 3 rotas de `plan.md` navegam corretamente pelo browser.

---

## Fase 6 — Validação final

- [x] **T019** Rodar o roteiro do `plan.md` › Quickstart do início ao fim
  contra o backend real (`quarkus:dev`), percorrendo manualmente cada
  critério de aceite de US-1 a US-5 em `spec.md`.
- [x] **T020** Revisar `spec.md` → confirmar que todo critério de aceite foi
  exercitado manualmente em T019 (rastreabilidade, mesmo espírito do
  T041 do backend).

---

## Dependências entre fases

```
Fase 1 (Setup)
   └─▶ Fase 2 (API)
          └─▶ Fase 3 (Componentes)
                 └─▶ Fase 4 (Páginas) ─▶ Fase 5 (Roteamento) ─▶ Fase 6 (Validação)
```

Tasks `[P]` dentro da mesma fase podem ser feitas em qualquer ordem entre si.
