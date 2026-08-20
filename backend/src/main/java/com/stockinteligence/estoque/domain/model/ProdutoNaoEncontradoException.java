package com.stockinteligence.estoque.domain.model;

import java.util.UUID;

/**
 * Lançada quando um produto consultado/alterado por identificador ou SKU
 * não existe.
 */
public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException(UUID id) {
        super("Produto não encontrado: " + id);
    }

    public ProdutoNaoEncontradoException(SKU sku) {
        super("Produto não encontrado para o SKU: " + sku.valor());
    }

    private ProdutoNaoEncontradoException(String mensagemPronta) {
        super(mensagemPronta);
    }

    /** Usado pelo read side, que não normaliza a entrada em um {@link SKU}. */
    public static ProdutoNaoEncontradoException paraSku(String sku) {
        return new ProdutoNaoEncontradoException("Produto não encontrado para o SKU: " + sku);
    }
}
