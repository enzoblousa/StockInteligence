package com.stockinteligence.estoque.domain.model;

import java.util.UUID;

/**
 * Lançada quando uma saída de estoque (US-3) solicita quantidade maior que
 * o saldo atual do produto (specs/002-alerta-estoque-baixo/spec.md FR-006).
 */
public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(UUID produtoId, Quantidade disponivel, Quantidade solicitada) {
        super("Saldo insuficiente para o produto %s: disponível %s, solicitado %s."
                .formatted(produtoId, disponivel.valor(), solicitada.valor()));
    }
}
