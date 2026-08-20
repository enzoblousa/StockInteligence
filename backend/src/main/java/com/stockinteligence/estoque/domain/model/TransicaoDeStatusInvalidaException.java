package com.stockinteligence.estoque.domain.model;

/**
 * Lançada quando {@link Produto#inativar()} ou {@link Produto#reativar()}
 * é chamado a partir de um status que não permite a transição.
 */
public class TransicaoDeStatusInvalidaException extends RuntimeException {

    public TransicaoDeStatusInvalidaException(String mensagem) {
        super(mensagem);
    }
}
