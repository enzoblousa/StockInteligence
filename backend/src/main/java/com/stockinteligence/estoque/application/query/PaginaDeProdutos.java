package com.stockinteligence.estoque.application.query;

import java.util.List;

/** Resultado paginado de {@link ListarProdutosQuery} (US-3). */
public record PaginaDeProdutos(List<ProdutoResult> conteudo, int page, int size, long totalElements) {
}
