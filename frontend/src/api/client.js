import axios from 'axios'

/**
 * Instância Axios centralizada — nenhum outro arquivo do projeto deve
 * chamar Axios diretamente (memory/constitution.md § Frontend).
 */
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

export default client
