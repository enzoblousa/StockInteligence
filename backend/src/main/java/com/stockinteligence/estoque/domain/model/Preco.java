package com.stockinteligence.estoque.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value Object monetário. Nunca negativo (specs/001-cadastro-produto/spec.md
 * FR-009), escala normalizada para 2 casas decimais.
 */
public record Preco(BigDecimal valor) {

    public Preco {
        if (valor == null) {
            throw new IllegalArgumentException("Preço não pode ser nulo.");
        }
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("Preço não pode ser negativo: " + valor);
        }
        valor = valor.setScale(2, RoundingMode.HALF_UP);
    }
}
