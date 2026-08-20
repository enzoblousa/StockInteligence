import Alert from 'react-bootstrap/Alert'

/**
 * Mensagem de erro reutilizável (US-5) — mesmo componente em toda a
 * aplicação, nunca uma mensagem de erro "solta" numa página específica.
 */
export default function Alerta({ mensagem }) {
  if (!mensagem) {
    return null
  }

  return <Alert variant="danger">{mensagem}</Alert>
}
