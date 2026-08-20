package com.stockinteligence.estoque.application.query;

import java.util.UUID;

/** Consulta um produto pelo identificador (US-2). */
public record BuscarProdutoPorIdQuery(UUID id) {
}
