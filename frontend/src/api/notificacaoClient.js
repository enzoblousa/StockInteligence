import axios from 'axios'

/**
 * Segunda instância Axios — deliberada, não uma exceção descuidada ao
 * padrão de "client único" (memory/constitution.md § Frontend). O
 * notification-service é um serviço implantado à parte do backend, com
 * sua própria URL pública; um app com dois backends de verdade precisa de
 * duas baseURLs. Nenhum arquivo além de alertaService.js deve importar
 * este client diretamente.
 */
const notificacaoClient = axios.create({
  baseURL: import.meta.env.VITE_ALERTAS_API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

export default notificacaoClient
