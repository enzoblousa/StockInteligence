import client from './client'

/**
 * Único ponto de tratamento de erro da API (US-5): usa a mensagem de
 * negócio do backend (`{ mensagem }`, corpo dos 400/404/409) quando
 * disponível, senão cai numa mensagem genérica — nunca deixa o erro cru
 * do Axios vazar para a UI.
 */
function tratarErro(erro) {
  const mensagemBackend = erro.response?.data?.mensagem
  if (mensagemBackend) {
    throw new Error(mensagemBackend)
  }
  if (erro.response) {
    throw new Error(`Erro ${erro.response.status} ao comunicar com o servidor.`)
  }
  throw new Error('Não foi possível conectar ao servidor. Verifique sua conexão.')
}

/** GET /api/produtos — US-1 */
export async function listar({ categoria, status, page = 0, size = 20 } = {}) {
  try {
    const params = { page, size }
    if (categoria) params.categoria = categoria
    if (status) params.status = status

    const { data } = await client.get('/api/produtos', { params })
    return data
  } catch (erro) {
    tratarErro(erro)
  }
}

/** GET /api/produtos/{id} — carrega dados para edição (US-3) */
export async function buscarPorId(id) {
  try {
    const { data } = await client.get(`/api/produtos/${id}`)
    return data
  } catch (erro) {
    tratarErro(erro)
  }
}

/** POST /api/produtos — US-2 */
export async function criar(dados) {
  try {
    const { data } = await client.post('/api/produtos', dados)
    return data
  } catch (erro) {
    tratarErro(erro)
  }
}

/** PUT /api/produtos/{id} — US-3 */
export async function atualizar(id, dados) {
  try {
    const { data } = await client.put(`/api/produtos/${id}`, dados)
    return data
  } catch (erro) {
    tratarErro(erro)
  }
}

/** PATCH /api/produtos/{id}/inativar — US-4 */
export async function inativar(id) {
  try {
    const { data } = await client.patch(`/api/produtos/${id}/inativar`)
    return data
  } catch (erro) {
    tratarErro(erro)
  }
}

/** PATCH /api/produtos/{id}/reativar — US-4 */
export async function reativar(id) {
  try {
    const { data } = await client.patch(`/api/produtos/${id}/reativar`)
    return data
  } catch (erro) {
    tratarErro(erro)
  }
}
