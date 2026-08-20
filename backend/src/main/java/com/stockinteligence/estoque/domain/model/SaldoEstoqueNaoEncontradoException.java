package com.stockinteligence.estoque.domain.model;

import java.util.UUID;

/**
 * Lançada quando um saldo de estoque consultado/alterado não existe para o
 * produto informado (specs/002-alerta-estoque-baixo/spec.md US-2, US-3, US-4).
 */
public class SaldoEstoqueNaoEncontradoException extends RuntimeException {

    public SaldoEstoqueNaoEncontradoException(UUID produtoId) {
        super("Não há saldo de estoque registrado para o produto: " + produtoId);
    }

    private SaldoEstoqueNaoEncontradoException(String mensagemPronta) {
        super(mensagemPronta);
    }

    /** Usado pelo read side, que consulta por SKU em vez de produtoId. */
    public static SaldoEstoqueNaoEncontradoException paraSku(String sku) {
        return new SaldoEstoqueNaoEncontradoException("Não há saldo de estoque registrado para o SKU: " + sku);
    }
}
