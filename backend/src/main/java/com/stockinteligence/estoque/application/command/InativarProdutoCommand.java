package com.stockinteligence.estoque.application.command;

import java.util.UUID;

/** Intenção de inativar um produto ativo (US-5). */
public record InativarProdutoCommand(UUID id) {
}
