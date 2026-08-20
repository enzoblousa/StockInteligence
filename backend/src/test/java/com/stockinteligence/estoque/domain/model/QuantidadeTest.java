package com.stockinteligence.estoque.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Cobertura exaustiva da regra de quantidade não-negativa — camada dona
 * (memory/testing-strategy.md).
 */
class QuantidadeTest {

    @Test
    void deveAceitarValorPositivo() {
        Quantidade quantidade = new Quantidade(new BigDecimal("10"));

        assertThat(quantidade.valor()).isEqualByComparingTo("10");
    }

    @Test
    void deveAceitarZero() {
        Quantidade quantidade = Quantidade.zero();

        assertThat(quantidade.ehZero()).isTrue();
    }

    @Test
    void deveRejeitarValorNegativo() {
        assertThatThrownBy(() -> new Quantidade(new BigDecimal("-0.001")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarValorNulo() {
        assertThatThrownBy(() -> new Quantidade(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveNormalizarEscalaParaTresCasas() {
        Quantidade quantidade = new Quantidade(new BigDecimal("1.5"));

        assertThat(quantidade.valor()).isEqualByComparingTo("1.500");
        assertThat(quantidade.valor().scale()).isEqualTo(3);
    }

    @Test
    void deveSomarQuantidades() {
        Quantidade resultado = new Quantidade(new BigDecimal("10")).somar(new Quantidade(new BigDecimal("5")));

        assertThat(resultado.valor()).isEqualByComparingTo("15");
    }

    @Test
    void deveSubtrairQuantidades() {
        Quantidade resultado = new Quantidade(new BigDecimal("10")).subtrair(new Quantidade(new BigDecimal("4")));

        assertThat(resultado.valor()).isEqualByComparingTo("6");
    }

    @Test
    void subtrairAlemDoValorLancaExcecao() {
        Quantidade dez = new Quantidade(new BigDecimal("10"));

        assertThatThrownBy(() -> dez.subtrair(new Quantidade(new BigDecimal("11"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compareToOrdenaPorValor() {
        Quantidade menor = new Quantidade(new BigDecimal("5"));
        Quantidade maior = new Quantidade(new BigDecimal("10"));

        assertThat(menor.compareTo(maior)).isNegative();
        assertThat(maior.compareTo(menor)).isPositive();
    }
}
