package com.stockinteligence.estoque.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/** Intenção de registrar saída de estoque de um produto (US-3). */
public record RegistrarSaidaEstoqueCommand(UUID produtoId, BigDecimal quantidade) {
}
