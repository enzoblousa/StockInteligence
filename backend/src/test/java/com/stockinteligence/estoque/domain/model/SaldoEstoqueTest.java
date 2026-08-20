package com.stockinteligence.estoque.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockinteligence.estoque.domain.event.EstoqueBaixoAtingido;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Cobertura exaustiva das invariantes do agregado SaldoEstoque, incluindo a
 * regra de transição do alerta de estoque baixo (FR-008) — camada dona
 * (memory/testing-strategy.md). CommandHandlers e testes de integração não
 * retestam este conjunto de cenários, apenas orquestração/HTTP.
 */
class SaldoEstoqueTest {

    private static final SKU SKU_VALIDO = new SKU("BEB-001");

    private static SaldoEstoque saldoComMinimo(String quantidadeInicial, String quantidadeMinima) {
        return SaldoEstoque.iniciar(UUID.randomUUID(), SKU_VALIDO,
                new Quantidade(new BigDecimal(quantidadeInicial)), new Quantidade(new BigDecimal(quantidadeMinima)));
    }

    private static Quantidade qtd(String valor) {
        return new Quantidade(new BigDecimal(valor));
    }

    @Test
    void deveIniciarComQuantidadesInformadas() {
        SaldoEstoque saldo = saldoComMinimo("10", "5");

        assertThat(saldo.id()).isNotNull();
        assertThat(saldo.quantidadeAtual().valor()).isEqualByComparingTo("10");
        assertThat(saldo.quantidadeMinima().valor()).isEqualByComparingTo("5");
        assertThat(saldo.eventosPendentes()).isEmpty();
    }

    @Test
    void entradaSomaAoSaldoAtual() {
        SaldoEstoque saldo = saldoComMinimo("10", "5");

        saldo.registrarEntrada(qtd("3"));

        assertThat(saldo.quantidadeAtual().valor()).isEqualByComparingTo("13");
    }

    @Test
    void entradaComQuantidadeZeroRejeita() {
        SaldoEstoque saldo = saldoComMinimo("10", "5");

        assertThatThrownBy(() -> saldo.registrarEntrada(Quantidade.zero()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saidaSubtraiDoSaldoAtual() {
        SaldoEstoque saldo = saldoComMinimo("10", "5");

        saldo.registrarSaida(qtd("3"));

        assertThat(saldo.quantidadeAtual().valor()).isEqualByComparingTo("7");
    }

    @Test
    void saidaComQuantidadeZeroRejeita() {
        SaldoEstoque saldo = saldoComMinimo("10", "5");

        assertThatThrownBy(() -> saldo.registrarSaida(Quantidade.zero()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saidaMaiorQueSaldoLancaExcecaoESaldoPermaneceInalterado() {
        SaldoEstoque saldo = saldoComMinimo("10", "5");

        assertThatThrownBy(() -> saldo.registrarSaida(qtd("11")))
                .isInstanceOf(SaldoInsuficienteException.class);

        assertThat(saldo.quantidadeAtual().valor()).isEqualByComparingTo("10");
        assertThat(saldo.eventosPendentes()).isEmpty();
    }

    @Test
    void saidaQueCruzaOLimiarSinalizaUmEventoDeEstoqueBaixo() {
        SaldoEstoque saldo = saldoComMinimo("10", "5");

        saldo.registrarSaida(qtd("6")); // 10 -> 4, cruza (estava > 5, cai para <= 5)

        assertThat(saldo.quantidadeAtual().valor()).isEqualByComparingTo("4");
        assertThat(saldo.eventosPendentes()).hasSize(1);
        EstoqueBaixoAtingido evento = (EstoqueBaixoAtingido) saldo.eventosPendentes().get(0);
        assertThat(evento.produtoId()).isEqualTo(saldo.produtoId());
        assertThat(evento.sku()).isEqualTo(SKU_VALIDO.valor());
        assertThat(evento.quantidadeAtual()).isEqualByComparingTo("4");
        assertThat(evento.quantidadeMinima()).isEqualByComparingTo("5");
    }

    @Test
    void saidaQueDeixaExatamenteNaQuantidadeMinimaSinalizaEvento() {
        SaldoEstoque saldo = saldoComMinimo("10", "5");

        saldo.registrarSaida(qtd("5")); // 10 -> 5, igual ao mínimo

        assertThat(saldo.eventosPendentes()).hasSize(1);
    }

    @Test
    void saidaQueNaoCruzaOLimiarNaoSinalizaEvento() {
        SaldoEstoque saldo = saldoComMinimo("10", "5");

        saldo.registrarSaida(qtd("2")); // 10 -> 8, continua acima do mínimo

        assertThat(saldo.eventosPendentes()).isEmpty();
    }

    @Test
    void saidaSubsequenteComSaldoJaAbaixoDoMinimoNaoRepeteOAlerta() {
        SaldoEstoque saldo = saldoComMinimo("10", "5");
        saldo.registrarSaida(qtd("6")); // 10 -> 4, cruza: 1 evento
        saldo.limparEventosPendentes();

        saldo.registrarSaida(qtd("1")); // 4 -> 3, já estava abaixo do mínimo

        assertThat(saldo.eventosPendentes()).isEmpty();
    }

    @Test
    void entradaQueDevolveAcimaDoMinimoRearmaProximaTransicao() {
        SaldoEstoque saldo = saldoComMinimo("10", "5");
        saldo.registrarSaida(qtd("6")); // 10 -> 4, cruza: 1 evento
        saldo.limparEventosPendentes();
        saldo.registrarEntrada(qtd("5")); // 4 -> 9, volta pra acima do mínimo

        saldo.registrarSaida(qtd("5")); // 9 -> 4, cruza de novo

        assertThat(saldo.eventosPendentes()).hasSize(1);
    }

    @Test
    void limparEventosPendentesEsvaziaALista() {
        SaldoEstoque saldo = saldoComMinimo("10", "5");
        saldo.registrarSaida(qtd("6"));
        assertThat(saldo.eventosPendentes()).hasSize(1);

        saldo.limparEventosPendentes();

        assertThat(saldo.eventosPendentes()).isEmpty();
    }
}
