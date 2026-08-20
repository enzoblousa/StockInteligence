package com.stockinteligence.estoque.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value Object de quantidade de estoque. Nunca negativo, escala normalizada
 * para 3 casas decimais — suporta as unidades de medida fracionárias já
 * existentes em {@link UnidadeMedida} (ex.: KG, L, ML)
 * (specs/002-alerta-estoque-baixo/plan.md § Decisões técnicas).
 */
public record Quantidade(BigDecimal valor) implements Comparable<Quantidade> {

    public Quantidade {
        if (valor == null) {
            throw new IllegalArgumentException("Quantidade não pode ser nula.");
        }
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa: " + valor);
        }
        valor = valor.setScale(3, RoundingMode.HALF_UP);
    }

    public static Quantidade zero() {
        return new Quantidade(BigDecimal.ZERO);
    }

    public boolean ehZero() {
        return valor.signum() == 0;
    }

    public Quantidade somar(Quantidade outra) {
        return new Quantidade(valor.add(outra.valor));
    }

    public Quantidade subtrair(Quantidade outra) {
        return new Quantidade(valor.subtract(outra.valor));
    }

    @Override
    public int compareTo(Quantidade outra) {
        return valor.compareTo(outra.valor);
    }
}
