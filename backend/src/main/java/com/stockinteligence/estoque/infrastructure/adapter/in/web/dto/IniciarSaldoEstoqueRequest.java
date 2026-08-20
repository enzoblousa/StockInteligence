package com.stockinteligence.estoque.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Corpo de request de {@code POST /api/produtos/{produtoId}/saldo-estoque} (US-1). */
public record IniciarSaldoEstoqueRequest(
        @NotNull(message = "Quantidade inicial é obrigatória.")
        @PositiveOrZero(message = "Quantidade inicial não pode ser negativa.") BigDecimal quantidadeInicial,
        @NotNull(message = "Quantidade mínima é obrigatória.")
        @PositiveOrZero(message = "Quantidade mínima não pode ser negativa.") BigDecimal quantidadeMinima) {
}
