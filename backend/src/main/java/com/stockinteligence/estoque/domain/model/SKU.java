package com.stockinteligence.estoque.domain.model;

import java.util.regex.Pattern;

/**
 * Value Object do SKU de um produto. Imutável e único entre produtos ativos
 * (invariante garantida fora deste tipo — ver
 * {@link com.stockinteligence.estoque.domain.repository.ProdutoRepository}).
 *
 * Formato: alfanumérico + hífen, 3 a 50 caracteres, normalizado para
 * maiúsculas na construção (specs/001-cadastro-produto/plan.md).
 */
public record SKU(String valor) {

    private static final Pattern FORMATO_VALIDO = Pattern.compile("^[A-Z0-9\\-]{3,50}$");

    public SKU {
        if (valor == null) {
            throw new IllegalArgumentException("SKU não pode ser nulo.");
        }
        valor = valor.toUpperCase();
        if (!FORMATO_VALIDO.matcher(valor).matches()) {
            throw new IllegalArgumentException(
                    "SKU '%s' inválido: deve ter de 3 a 50 caracteres alfanuméricos ou hífen.".formatted(valor));
        }
    }
}
