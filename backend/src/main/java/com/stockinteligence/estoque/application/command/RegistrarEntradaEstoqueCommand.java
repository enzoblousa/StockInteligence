package com.stockinteligence.estoque.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/** Intenção de registrar entrada de estoque de um produto (US-2). */
public record RegistrarEntradaEstoqueCommand(UUID produtoId, BigDecimal quantidade) {
}
