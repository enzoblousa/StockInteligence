package com.stockinteligence.estoque.application.query;

import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.StatusProduto;

/**
 * Lista produtos com filtro opcional de categoria/status e paginação
 * (US-3). {@code categoria}/{@code status} nulos significam "sem filtro".
 */
public record ListarProdutosQuery(Categoria categoria, StatusProduto status, int page, int size) {
}
