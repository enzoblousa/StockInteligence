import { useEffect, useState } from 'react'
import { useNavigate, useParams, Link } from 'react-router-dom'
import Form from 'react-bootstrap/Form'
import Button from 'react-bootstrap/Button'
import * as produtoService from '../api/produtoService'
import Alerta from '../components/Alerta'
import Carregando from '../components/Carregando'
import { CATEGORIAS, UNIDADES_MEDIDA } from '../constants/produtoOptions'

const FORM_VAZIO = {
  sku: '',
  nome: '',
  categoria: '',
  unidadeMedida: '',
  precoCusto: '',
  precoVenda: '',
}

/** US-2 (criar) e US-3 (editar) — mesmo componente, modo decidido pelo :id da rota. */
export default function ProdutoFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const modoEdicao = Boolean(id)

  const [form, setForm] = useState(FORM_VAZIO)
  const [carregando, setCarregando] = useState(modoEdicao)
  const [erro, setErro] = useState(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    if (!modoEdicao) {
      return
    }

    let cancelado = false
    async function carregar() {
      setCarregando(true)
      setErro(null)
      try {
        const produto = await produtoService.buscarPorId(id)
        if (!cancelado) {
          setForm({
            sku: produto.sku,
            nome: produto.nome,
            categoria: produto.categoria,
            unidadeMedida: produto.unidadeMedida,
            precoCusto: produto.precoCusto,
            precoVenda: produto.precoVenda,
          })
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
  }, [id, modoEdicao])

  function aoMudarCampo(evento) {
    const { name, value } = evento.target
    setForm((atual) => ({ ...atual, [name]: value }))
  }

  async function aoSubmeter(evento) {
    evento.preventDefault()
    setErro(null)
    setEnviando(true)

    const dados = {
      nome: form.nome,
      categoria: form.categoria,
      unidadeMedida: form.unidadeMedida,
      precoCusto: Number(form.precoCusto),
      precoVenda: Number(form.precoVenda),
    }

    try {
      if (modoEdicao) {
        await produtoService.atualizar(id, dados)
      } else {
        await produtoService.criar({ ...dados, sku: form.sku })
      }
      navigate('/produtos')
    } catch (e) {
      // Não limpa o formulário em caso de erro (US-2) — só informa o problema.
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
      <h1 className="h3 mb-3">{modoEdicao ? 'Editar produto' : 'Novo produto'}</h1>

      <Alerta mensagem={erro} />

      <Form onSubmit={aoSubmeter}>
        <Form.Group className="mb-3" controlId="sku">
          <Form.Label>SKU</Form.Label>
          <Form.Control
            name="sku"
            value={form.sku}
            onChange={aoMudarCampo}
            required
            readOnly={modoEdicao}
            disabled={modoEdicao}
            minLength={3}
            maxLength={50}
            pattern="[A-Za-z0-9-]{3,50}"
            title="3 a 50 caracteres alfanuméricos ou hífen."
          />
          {modoEdicao && <Form.Text className="text-muted">SKU não pode ser alterado.</Form.Text>}
        </Form.Group>

        <Form.Group className="mb-3" controlId="nome">
          <Form.Label>Nome</Form.Label>
          <Form.Control name="nome" value={form.nome} onChange={aoMudarCampo} required maxLength={200} />
        </Form.Group>

        <Form.Group className="mb-3" controlId="categoria">
          <Form.Label>Categoria</Form.Label>
          <Form.Select name="categoria" value={form.categoria} onChange={aoMudarCampo} required>
            <option value="" disabled>
              Selecione...
            </option>
            {CATEGORIAS.map((valor) => (
              <option key={valor} value={valor}>
                {valor}
              </option>
            ))}
          </Form.Select>
        </Form.Group>

        <Form.Group className="mb-3" controlId="unidadeMedida">
          <Form.Label>Unidade de medida</Form.Label>
          <Form.Select name="unidadeMedida" value={form.unidadeMedida} onChange={aoMudarCampo} required>
            <option value="" disabled>
              Selecione...
            </option>
            {UNIDADES_MEDIDA.map((valor) => (
              <option key={valor} value={valor}>
                {valor}
              </option>
            ))}
          </Form.Select>
        </Form.Group>

        <Form.Group className="mb-3" controlId="precoCusto">
          <Form.Label>Preço de custo</Form.Label>
          <Form.Control
            type="number"
            name="precoCusto"
            value={form.precoCusto}
            onChange={aoMudarCampo}
            required
            min="0"
            step="0.01"
          />
        </Form.Group>

        <Form.Group className="mb-4" controlId="precoVenda">
          <Form.Label>Preço de venda</Form.Label>
          <Form.Control
            type="number"
            name="precoVenda"
            value={form.precoVenda}
            onChange={aoMudarCampo}
            required
            min="0"
            step="0.01"
          />
        </Form.Group>

        <div className="d-flex gap-2">
          <Button type="submit" variant="primary" disabled={enviando}>
            {enviando ? 'Salvando...' : 'Salvar'}
          </Button>
          <Link to="/produtos" className="btn btn-outline-secondary">
            Voltar
          </Link>
        </div>
      </Form>
    </div>
  )
}
