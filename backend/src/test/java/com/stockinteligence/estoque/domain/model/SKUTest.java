package com.stockinteligence.estoque.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Cobertura exaustiva da regra de formato de SKU — esta é a camada dona
 * dessa regra (memory/testing-strategy.md); nenhuma outra camada a retesta.
 */
class SKUTest {

    @Test
    void deveAceitarSkuValido() {
        SKU sku = new SKU("BEB-001");

        assertThat(sku.valor()).isEqualTo("BEB-001");
    }

    @Test
    void deveNormalizarParaMaiusculas() {
        SKU sku = new SKU("beb-001");

        assertThat(sku.valor()).isEqualTo("BEB-001");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "AB", "SKU COM ESPAÇO", "SKU_UNDERSCORE", "SKU#INVALIDO"})
    void deveRejeitarSkuInvalido(String valorInvalido) {
        assertThatThrownBy(() -> new SKU(valorInvalido))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarSkuMuitoLongo() {
        String skuGigante = "A".repeat(51);

        assertThatThrownBy(() -> new SKU(skuGigante)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveConsiderarIgualPorValorJaNormalizado() {
        SKU a = new SKU("beb-001");
        SKU b = new SKU("BEB-001");

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
