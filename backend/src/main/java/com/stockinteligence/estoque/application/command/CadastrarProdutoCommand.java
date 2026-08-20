package com.stockinteligence.estoque.application.command;

import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.UnidadeMedida;
import java.math.BigDecimal;

/**
 * Intenção de cadastrar um novo produto (US-1). Tipos primitivos/enum —
 * a tradução para Value Objects (SKU, Preco) acontece no
 * {@link CadastrarProdutoCommandHandler}, não aqui.
 */
public record CadastrarProdutoCommand(
        String sku,
        String nome,
        Categoria categoria,
        UnidadeMedida unidadeMedida,
        BigDecimal precoCusto,
        BigDecimal precoVenda) {
}
