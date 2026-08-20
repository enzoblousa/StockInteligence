package com.stockinteligence.estoque.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Corpo de request das entradas/saídas de estoque (US-2, US-3). Mesma
 * forma para os dois — a semântica (soma ou subtrai) é dada pela rota.
 */
public record RegistrarMovimentoEstoqueRequest(
        @NotNull(message = "Quantidade é obrigatória.")
        @Positive(message = "Quantidade deve ser maior que zero.") BigDecimal quantidade) {
}
