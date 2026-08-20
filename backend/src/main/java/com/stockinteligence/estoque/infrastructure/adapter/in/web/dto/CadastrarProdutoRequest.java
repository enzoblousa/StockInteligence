package com.stockinteligence.estoque.infrastructure.adapter.in.web.dto;

import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.UnidadeMedida;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Corpo de request de {@code POST /api/produtos} (US-1). */
public record CadastrarProdutoRequest(
        @NotBlank(message = "SKU é obrigatório.") String sku,
        @NotBlank(message = "Nome é obrigatório.") String nome,
        @NotNull(message = "Categoria é obrigatória.") Categoria categoria,
        @NotNull(message = "Unidade de medida é obrigatória.") UnidadeMedida unidadeMedida,
        @NotNull(message = "Preço de custo é obrigatório.")
        @PositiveOrZero(message = "Preço de custo não pode ser negativo.") BigDecimal precoCusto,
        @NotNull(message = "Preço de venda é obrigatório.")
        @PositiveOrZero(message = "Preço de venda não pode ser negativo.") BigDecimal precoVenda) {
}
