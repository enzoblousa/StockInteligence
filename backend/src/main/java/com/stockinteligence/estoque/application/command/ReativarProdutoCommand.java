package com.stockinteligence.estoque.application.command;

import java.util.UUID;

/** Intenção de reativar um produto inativo (US-5). */
public record ReativarProdutoCommand(UUID id) {
}
