package com.stockinteligence.estoque.application.query;

import java.util.UUID;

/** Consulta o saldo de estoque de um produto pelo seu identificador (US-4). */
public record BuscarSaldoEstoquePorProdutoIdQuery(UUID produtoId) {
}
