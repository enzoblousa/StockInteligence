package com.stockinteligence.estoque.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Intenção de definir o saldo inicial e a quantidade mínima de um produto
 * (US-1). Tipos primitivos — a tradução para Value Objects (Quantidade)
 * acontece no {@link IniciarSaldoEstoqueCommandHandler}, não aqui.
 */
public record IniciarSaldoEstoqueCommand(UUID produtoId, BigDecimal quantidadeInicial, BigDecimal quantidadeMinima) {
}
