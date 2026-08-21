import Table from 'react-bootstrap/Table'
import Button from 'react-bootstrap/Button'
import Badge from 'react-bootstrap/Badge'
import { Link } from 'react-router-dom'

/**
 * Tabela de produtos (US-1) com ações de inativar/reativar inline (US-4).
 * Não chama a API diretamente — só recebe dados e callbacks via props.
 */
export default function ProdutoTable({ produtos, onInativar, onReativar }) {
  if (produtos.length === 0) {
    return <p className="text-muted">Nenhum produto encontrado.</p>
  }

  return (
    <Table striped bordered hover responsive>
      <thead>
        <tr>
          <th>SKU</th>
          <th>Nome</th>
          <th>Categoria</th>
          <th>Status</th>
          <th>Ações</th>
        </tr>
      </thead>
      <tbody>
        {produtos.map((produto) => (
          <tr key={produto.id}>
            <td>{produto.sku}</td>
            <td>{produto.nome}</td>
            <td>{produto.categoria}</td>
            <td>
              <Badge bg={produto.status === 'ATIVO' ? 'success' : 'secondary'}>{produto.status}</Badge>
            </td>
            <td className="d-flex gap-2">
              <Link to={`/produtos/${produto.id}/editar`} className="btn btn-sm btn-outline-primary">
                Editar
              </Link>
              <Link to={`/produtos/${produto.id}/estoque`} className="btn btn-sm btn-outline-info">
                Estoque
              </Link>
              {produto.status === 'ATIVO' ? (
                <Button size="sm" variant="outline-warning" onClick={() => onInativar(produto)}>
                  Inativar
                </Button>
              ) : (
                <Button size="sm" variant="outline-success" onClick={() => onReativar(produto)}>
                  Reativar
                </Button>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  )
}
