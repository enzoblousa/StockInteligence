import client from './client'

/** Mesmo padrão de produtoService.js — cópia isolada por domínio. */
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

/**
 * GET /api/produtos/{produtoId}/saldo-estoque — retorna null se o saldo
 * ainda não foi iniciado (404 é um estado esperado deste fluxo, não um
 * erro: todo produto passa por "sem saldo" antes de "com saldo").
 */
export async function buscarSaldo(produtoId) {
  try {
    const { data } = await client.get(`/api/produtos/${produtoId}/saldo-estoque`)
    return data
  } catch (erro) {
    if (erro.response?.status === 404) {
      return null
    }
    tratarErro(erro)
  }
}

/** POST /api/produtos/{produtoId}/saldo-estoque — US-1 (specs/002-alerta-estoque-baixo) */
export async function iniciarSaldo(produtoId, dados) {
  try {
    const { data } = await client.post(`/api/produtos/${produtoId}/saldo-estoque`, dados)
    return data
  } catch (erro) {
    tratarErro(erro)
  }
}

/** POST /api/produtos/{produtoId}/saldo-estoque/entradas — US-2 */
export async function registrarEntrada(produtoId, dados) {
  try {
    const { data } = await client.post(`/api/produtos/${produtoId}/saldo-estoque/entradas`, dados)
    return data
  } catch (erro) {
    tratarErro(erro)
  }
}

/** POST /api/produtos/{produtoId}/saldo-estoque/saidas — US-3 */
export async function registrarSaida(produtoId, dados) {
  try {
    const { data } = await client.post(`/api/produtos/${produtoId}/saldo-estoque/saidas`, dados)
    return data
  } catch (erro) {
    tratarErro(erro)
  }
}
