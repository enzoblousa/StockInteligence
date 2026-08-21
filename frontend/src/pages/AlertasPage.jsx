import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Table from 'react-bootstrap/Table'
import * as alertaService from '../api/alertaService'
import Alerta from '../components/Alerta'
import Carregando from '../components/Carregando'

const INTERVALO_POLLING_MS = 5000

/**
 * Lista os alertas de estoque baixo consumidos pelo notification-service
 * a partir do tópico Kafka estoque.baixo-atingido (specs/002-alerta-estoque-baixo,
 * US-5). Faz polling simples — sem WebSocket/SSE, mais simples de operar
 * e suficiente pro volume esperado (Princípio VI, YAGNI). Diferente de
 * ProdutoListPage: o spinner só aparece na carga inicial, não a cada
 * poll em segundo plano (evitaria a tela "piscando" a cada 5s).
 */
export default function AlertasPage() {
  const [alertas, setAlertas] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)

  useEffect(() => {
    let cancelado = false

    async function carregar(primeiraCarga) {
      if (primeiraCarga) {
        setCarregando(true)
      }
      try {
        const dados = await alertaService.listarAlertas()
        if (!cancelado) {
          setAlertas(dados)
          setErro(null)
        }
      } catch (e) {
        if (!cancelado) {
          setErro(e.message)
        }
      } finally {
        if (!cancelado && primeiraCarga) {
          setCarregando(false)
        }
      }
    }

    carregar(true)
    const intervalo = setInterval(() => carregar(false), INTERVALO_POLLING_MS)

    return () => {
      cancelado = true
      clearInterval(intervalo)
    }
  }, [])

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h1 className="h3 m-0">Alertas de estoque baixo</h1>
        <Link to="/produtos" className="btn btn-outline-secondary">
          Voltar
        </Link>
      </div>

      <Alerta mensagem={erro} />

      {carregando ? (
        <Carregando />
      ) : alertas.length === 0 ? (
        <p className="text-muted">Nenhum alerta recebido ainda.</p>
      ) : (
        <Table striped bordered hover responsive>
          <thead>
            <tr>
              <th>SKU</th>
              <th>Quantidade atual</th>
              <th>Quantidade mínima</th>
              <th>Ocorrido em</th>
              <th>Recebido em</th>
            </tr>
          </thead>
          <tbody>
            {alertas.map((alerta) => (
              <tr key={alerta.id}>
                <td>{alerta.sku}</td>
                <td>{alerta.quantidadeAtual}</td>
                <td>{alerta.quantidadeMinima}</td>
                <td>{new Date(alerta.ocorridoEm).toLocaleString('pt-BR')}</td>
                <td>{new Date(alerta.recebidoEm).toLocaleString('pt-BR')}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </div>
  )
}
