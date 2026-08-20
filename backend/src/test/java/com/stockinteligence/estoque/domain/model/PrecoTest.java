package com.stockinteligence.estoque.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Cobertura exaustiva da regra de preço não-negativo — camada dona
 * (memory/testing-strategy.md).
 */
class PrecoTest {

    @Test
    void deveAceitarValorPositivo() {
        Preco preco = new Preco(new BigDecimal("7.90"));

        assertThat(preco.valor()).isEqualByComparingTo("7.90");
    }

    @Test
    void deveAceitarZero() {
        Preco preco = new Preco(BigDecimal.ZERO);

        assertThat(preco.valor()).isEqualByComparingTo("0");
    }

    @Test
    void deveRejeitarValorNegativo() {
        assertThatThrownBy(() -> new Preco(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarValorNulo() {
        assertThatThrownBy(() -> new Preco(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveNormalizarEscalaParaDuasCasas() {
        Preco preco = new Preco(new BigDecimal("7.9"));

        assertThat(preco.valor()).isEqualByComparingTo("7.90");
        assertThat(preco.valor().scale()).isEqualTo(2);
    }
}
