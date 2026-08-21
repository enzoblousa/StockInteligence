import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import Form from 'react-bootstrap/Form'
import Button from 'react-bootstrap/Button'
import Badge from 'react-bootstrap/Badge'
import * as produtoService from '../api/produtoService'
import * as estoqueService from '../api/estoqueService'
import Alerta from '../components/Alerta'
import Carregando from '../components/Carregando'

const FORM_INICIAR_VAZIO = { quantidadeInicial: '', quantidadeMinima: '' }
const FORM_MOVIMENTO_VAZIO = { quantidade: '' }

/**
 * Gerencia o saldo de estoque de um produto (specs/002-alerta-estoque-baixo,
 * US-1 a US-4). Se o produto ainda não tem saldo definido (404 do backend
 * — ver estoqueService.buscarSaldo), mostra o formulário de "iniciar
 * saldo"; senão, mostra o saldo atual e os formulários de entrada/saída.
 */
export default function SaldoEstoquePage() {
  const { id } = useParams()

  const [produto, setProduto] = useState(null)
  const [saldo, setSaldo] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)
  const [enviando, setEnviando] = useState(false)

  const [formIniciar, setFormIniciar] = useState(FORM_INICIAR_VAZIO)
  const [formEntrada, setFormEntrada] = useState(FORM_MOVIMENTO_VAZIO)
  const [formSaida, setFormSaida] = useState(FORM_MOVIMENTO_VAZIO)

  useEffect(() => {
    let cancelado = false

    async function carregar() {
      setCarregando(true)
      setErro(null)
      try {
        const [produtoCarregado, saldoCarregado] = await Promise.all([
          produtoService.buscarPorId(id),
          estoqueService.buscarSaldo(id),
        ])
        if (!cancelado) {
          setProduto(produtoCarregado)
          setSaldo(saldoCarregado)
        }
      } catch (e) {
        if (!cancelado) {
          setErro(e.message)
        }
      } finally {
        if (!cancelado) {
          setCarregando(false)
        }
      }
    }

    carregar()
    return () => {
      cancelado = true
    }
  }, [id])

  async function recarregarSaldo() {
    const saldoAtualizado = await estoqueService.buscarSaldo(id)
    setSaldo(saldoAtualizado)
  }

  async function aoIniciarSaldo(evento) {
    evento.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      await estoqueService.iniciarSaldo(id, {
        quantidadeInicial: Number(formIniciar.quantidadeInicial),
        quantidadeMinima: Number(formIniciar.quantidadeMinima),
      })
      await recarregarSaldo()
      setFormIniciar(FORM_INICIAR_VAZIO)
    } catch (e) {
      setErro(e.message)
    } finally {
      setEnviando(false)
    }
  }

  async function aoRegistrarEntrada(evento) {
    evento.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      await estoqueService.registrarEntrada(id, { quantidade: Number(formEntrada.quantidade) })
      await recarregarSaldo()
      setFormEntrada(FORM_MOVIMENTO_VAZIO)
    } catch (e) {
      setErro(e.message)
    } finally {
      setEnviando(false)
    }
  }

  async function aoRegistrarSaida(evento) {
    evento.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      await estoqueService.registrarSaida(id, { quantidade: Number(formSaida.quantidade) })
      await recarregarSaldo()
      setFormSaida(FORM_MOVIMENTO_VAZIO)
    } catch (e) {
      setErro(e.message)
    } finally {
      setEnviando(false)
    }
  }

  if (carregando) {
    return <Carregando />
  }

  return (
    <div className="container py-4" style={{ maxWidth: '560px' }}>
      <h1 className="h3 mb-1">Estoque</h1>
      {produto && (
        <p className="text-muted mb-3">
          {produto.nome} ({produto.sku})
        </p>
      )}

      <Alerta mensagem={erro} />

      {saldo === null ? (
        <Form onSubmit={aoIniciarSaldo}>
          <p>Este produto ainda não tem saldo de estoque definido.</p>

          <Form.Group className="mb-3" controlId="quantidadeInicial">
            <Form.Label>Quantidade inicial</Form.Label>
            <Form.Control
              type="number"
              name="quantidadeInicial"
              value={formIniciar.quantidadeInicial}
              onChange={(e) => setFormIniciar((atual) => ({ ...atual, quantidadeInicial: e.target.value }))}
              required
              min="0"
              step="0.001"
            />
          </Form.Group>

          <Form.Group className="mb-4" controlId="quantidadeMinima">
            <Form.Label>Quantidade mínima (dispara alerta)</Form.Label>
            <Form.Control
              type="number"
              name="quantidadeMinima"
              value={formIniciar.quantidadeMinima}
              onChange={(e) => setFormIniciar((atual) => ({ ...atual, quantidadeMinima: e.target.value }))}
              required
              min="0"
              step="0.001"
            />
          </Form.Group>

          <div className="d-flex gap-2">
            <Button type="submit" variant="primary" disabled={enviando}>
              {enviando ? 'Salvando...' : 'Definir saldo inicial'}
            </Button>
            <Link to="/produtos" className="btn btn-outline-secondary">
              Voltar
            </Link>
          </div>
        </Form>
      ) : (
        <>
          <div className="d-flex align-items-center gap-2 mb-4">
            <span>
              Quantidade atual: <strong>{saldo.quantidadeAtual}</strong> · mínima:{' '}
              <strong>{saldo.quantidadeMinima}</strong>
            </span>
            {saldo.abaixoDoMinimo && <Badge bg="danger">Abaixo do mínimo</Badge>}
          </div>

          <Form onSubmit={aoRegistrarEntrada} className="mb-4">
            <Form.Group className="mb-2" controlId="quantidadeEntrada">
              <Form.Label>Registrar entrada</Form.Label>
              <Form.Control
                type="number"
                name="quantidade"
                value={formEntrada.quantidade}
                onChange={(e) => setFormEntrada({ quantidade: e.target.value })}
                required
                min="0.001"
                step="0.001"
              />
            </Form.Group>
            <Button type="submit" variant="success" size="sm" disabled={enviando}>
              Registrar entrada
            </Button>
          </Form>

          <Form onSubmit={aoRegistrarSaida} className="mb-4">
            <Form.Group className="mb-2" controlId="quantidadeSaida">
              <Form.Label>Registrar saída</Form.Label>
              <Form.Control
                type="number"
                name="quantidade"
                value={formSaida.quantidade}
                onChange={(e) => setFormSaida({ quantidade: e.target.value })}
                required
                min="0.001"
                step="0.001"
              />
            </Form.Group>
            <Button type="submit" variant="warning" size="sm" disabled={enviando}>
              Registrar saída
            </Button>
          </Form>

          <Link to="/produtos" className="btn btn-outline-secondary">
            Voltar
          </Link>
        </>
      )}
    </div>
  )
}
