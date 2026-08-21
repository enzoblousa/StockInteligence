import notificacaoClient from './notificacaoClient'

/**
 * Único ponto de tratamento de erro deste serviço (mesmo padrão de
 * produtoService.js/estoqueService.js — cada serviço tem sua própria
 * cópia, isolados por domínio).
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

/** GET /alertas — alertas de estoque baixo recebidos, mais recentes primeiro. */
export async function listarAlertas() {
  try {
    const { data } = await notificacaoClient.get('/alertas')
    return data
  } catch (erro) {
    tratarErro(erro)
  }
}
