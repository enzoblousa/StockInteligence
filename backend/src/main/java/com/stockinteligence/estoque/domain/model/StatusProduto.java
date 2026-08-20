package com.stockinteligence.estoque.domain.model;

/**
 * Status do ciclo de vida de um {@link Produto}. Não há exclusão física —
 * apenas inativação lógica (memory/constitution.md, US-5 de
 * specs/001-cadastro-produto/spec.md).
 */
public enum StatusProduto {
    ATIVO,
    INATIVO
}
