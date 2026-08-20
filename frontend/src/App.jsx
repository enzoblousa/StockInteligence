import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import ProdutoListPage from './pages/ProdutoListPage'
import ProdutoFormPage from './pages/ProdutoFormPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/produtos" replace />} />
        <Route path="/produtos" element={<ProdutoListPage />} />
        <Route path="/produtos/novo" element={<ProdutoFormPage />} />
        <Route path="/produtos/:id/editar" element={<ProdutoFormPage />} />
      </Routes>
    </BrowserRouter>
  )
}
