import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import * as produtoService from '../api/produtoService'
import Alerta from '../components/Alerta'
import Carregando from '../components/Carregando'
import FiltroProdutos from '../components/FiltroProdutos'
import ProdutoTable from '../components/ProdutoTable'

/** US-1 (listagem + filtro) e US-4 (inativar/reativar inline). */
export default function ProdutoListPage() {
  const [produtos, setProdutos] = useState([])
  const [filtro, setFiltro] = useState({ categoria: undefined, status: undefined })
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)

  useEffect(() => {
    let cancelado = false

    async function carregar() {
      setCarregando(true)
      setErro(null)
      try {
        const pagina = await produtoService.listar(filtro)
        if (!cancelado) {
          setProdutos(pagina.content)
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
  }, [filtro])

  function atualizarProdutoNaLista(produtoAtualizado) {
    setProdutos((atual) => atual.map((p) => (p.id === produtoAtualizado.id ? produtoAtualizado : p)))
  }

  async function aoInativar(produto) {
    if (!window.confirm(`Inativar o produto "${produto.nome}" (${produto.sku})?`)) {
      return
    }
    setErro(null)
    try {
      const atualizado = await produtoService.inativar(produto.id)
      atualizarProdutoNaLista(atualizado)
    } catch (e) {
      setErro(e.message)
    }
  }

  async function aoReativar(produto) {
    if (!window.confirm(`Reativar o produto "${produto.nome}" (${produto.sku})?`)) {
      return
    }
    setErro(null)
    try {
      const atualizado = await produtoService.reativar(produto.id)
      atualizarProdutoNaLista(atualizado)
    } catch (e) {
      setErro(e.message)
    }
  }

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h1 className="h3 m-0">Produtos</h1>
        <div className="d-flex gap-2">
          <Link to="/alertas" className="btn btn-outline-danger">
            Alertas
          </Link>
          <Link to="/produtos/novo" className="btn btn-primary">
            Novo produto
          </Link>
        </div>
      </div>

      <FiltroProdutos {...filtro} onFiltrar={setFiltro} />

      <Alerta mensagem={erro} />

      {carregando ? <Carregando /> : <ProdutoTable produtos={produtos} onInativar={aoInativar} onReativar={aoReativar} />}
    </div>
  )
}
