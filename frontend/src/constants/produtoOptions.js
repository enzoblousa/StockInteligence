/**
 * Espelha as listas fixas do backend (specs/001-cadastro-produto/plan.md
 * § Decisões técnicas). O backend não expõe endpoint de metadados de enum
 * — duplicação pequena e consciente (specs/002-frontend-cadastro-produto/
 * plan.md § Decisões técnicas). Se o backend mudar esses valores, este
 * arquivo precisa acompanhar manualmente.
 */
export const CATEGORIAS = [
  'ALIMENTOS',
  'BEBIDAS',
  'LIMPEZA',
  'ELETRONICOS',
  'VESTUARIO',
  'OUTROS',
]

export const UNIDADES_MEDIDA = ['UN', 'KG', 'G', 'L', 'ML', 'CX', 'PC', 'M']

export const STATUS = ['ATIVO', 'INATIVO']
