package com.stockinteligence.estoque.application.query;

import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.StatusProduto;
import com.stockinteligence.estoque.domain.model.UnidadeMedida;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Único DTO de leitura do produto — usado tanto para detalhe (US-2) quanto
 * para listagem (US-3). Sem regra de negócio nem invariantes (Princípio II
 * — CQRS); construído diretamente por projeção, sem passar pelo agregado.
 */
public record ProdutoResult(
        UUID id,
        String sku,
        String nome,
        Categoria categoria,
        UnidadeMedida unidadeMedida,
        BigDecimal precoCusto,
        BigDecimal precoVenda,
        StatusProduto status) {
}
