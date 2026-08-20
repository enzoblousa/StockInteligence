import Form from 'react-bootstrap/Form'
import Row from 'react-bootstrap/Row'
import Col from 'react-bootstrap/Col'
import { CATEGORIAS, STATUS } from '../constants/produtoOptions'

/** Filtro de categoria/status da listagem (US-1). */
export default function FiltroProdutos({ categoria, status, onFiltrar }) {
  function aoMudarCategoria(evento) {
    onFiltrar({ categoria: evento.target.value || undefined, status })
  }

  function aoMudarStatus(evento) {
    onFiltrar({ categoria, status: evento.target.value || undefined })
  }

  return (
    <Row className="mb-3 g-2">
      <Col xs="auto">
        <Form.Select value={categoria ?? ''} onChange={aoMudarCategoria} aria-label="Filtrar por categoria">
          <option value="">Todas as categorias</option>
          {CATEGORIAS.map((valor) => (
            <option key={valor} value={valor}>
              {valor}
            </option>
          ))}
        </Form.Select>
      </Col>
      <Col xs="auto">
        <Form.Select value={status ?? ''} onChange={aoMudarStatus} aria-label="Filtrar por status">
          <option value="">Todos os status</option>
          {STATUS.map((valor) => (
            <option key={valor} value={valor}>
              {valor}
            </option>
          ))}
        </Form.Select>
      </Col>
    </Row>
  )
}
