package com.stockinteligence.estoque.infrastructure.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockinteligence.estoque.domain.event.EstoqueBaixoAtingido;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.QuarkusTransactionException;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Prova o requisito central desta feature (US-5, FR-009): o publisher só
 * emite a mensagem depois que a transação que originou o evento
 * commita — nunca para uma transação que sofre rollback. Usa o conector
 * {@code smallrye-in-memory} (ativado em {@code %test}, ver
 * application.properties) para não depender de um broker Kafka real. Não
 * retesta a regra de quando o evento é sinalizado — isso é papel de
 * SaldoEstoqueTest (ver memory/testing-strategy.md).
 */
@QuarkusTest
class EstoqueBaixoAtingidoKafkaPublisherTest {

    @Inject
    Event<EstoqueBaixoAtingido> estoqueBaixoAtingidoEvent;

    @Inject
    @Any
    InMemoryConnector connector;

    @BeforeEach
    void limparSink() {
        connector.sink("estoque-baixo-atingido").clear();
    }

    private static EstoqueBaixoAtingido eventoDeTeste() {
        return new EstoqueBaixoAtingido(UUID.randomUUID(), "BEB-001", new BigDecimal("4.000"),
                new BigDecimal("5.000"), OffsetDateTime.now());
    }

    @Test
    void transacaoQueComitaPublicaExatamenteUmaMensagem() {
        QuarkusTransaction.requiringNew().run(() -> estoqueBaixoAtingidoEvent.fire(eventoDeTeste()));

        assertThat(connector.sink("estoque-baixo-atingido").received()).hasSize(1);
    }

    @Test
    void transacaoComRollbackNaoPublicaNenhumaMensagem() {
        // QuarkusTransaction.run() propaga RollbackException quando a
        // transação está marcada rollback-only e de fato reverte — o ponto
        // deste teste é justamente essa reversão, não a ausência de
        // exceção; por isso ela é esperada aqui.
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().run(() -> {
            estoqueBaixoAtingidoEvent.fire(eventoDeTeste());
            QuarkusTransaction.setRollbackOnly();
        })).isInstanceOf(QuarkusTransactionException.class);

        assertThat(connector.sink("estoque-baixo-atingido").received()).isEmpty();
    }
}
