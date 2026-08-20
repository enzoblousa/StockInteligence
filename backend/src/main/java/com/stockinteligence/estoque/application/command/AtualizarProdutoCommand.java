package com.stockinteligence.estoque.application.command;

import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.UnidadeMedida;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Intenção de editar dados cadastrais de um produto existente (US-4). Sem
 * campo {@code sku} — SKU é imutável (FR-004).
 */
public record AtualizarProdutoCommand(
        UUID id,
        String nome,
        Categoria categoria,
        UnidadeMedida unidadeMedida,
        BigDecimal precoCusto,
        BigDecimal precoVenda) {
}
