package com.stockinteligence.estoque.domain.model;

/**
 * Lançada quando um SKU já está em uso por outro produto ativo
 * (specs/001-cadastro-produto/spec.md FR-003).
 */
public class SkuJaCadastradoException extends RuntimeException {

    public SkuJaCadastradoException(SKU sku) {
        super("SKU '%s' já está em uso por outro produto ativo.".formatted(sku.valor()));
    }
}
