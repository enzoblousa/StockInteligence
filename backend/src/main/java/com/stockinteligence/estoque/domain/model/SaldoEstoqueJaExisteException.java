package com.stockinteligence.estoque.domain.model;

import java.util.UUID;

/**
 * Lançada ao tentar definir o saldo inicial de um produto que já tem saldo
 * de estoque registrado (specs/002-alerta-estoque-baixo/spec.md US-1).
 */
public class SaldoEstoqueJaExisteException extends RuntimeException {

    public SaldoEstoqueJaExisteException(UUID produtoId) {
        super("Já existe saldo de estoque registrado para o produto: " + produtoId);
    }
}
