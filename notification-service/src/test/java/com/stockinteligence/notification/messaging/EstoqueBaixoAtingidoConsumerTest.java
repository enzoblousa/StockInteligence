package com.stockinteligence.notification.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.stockinteligence.notification.alerta.AlertaResource;
import com.stockinteligence.notification.alerta.AlertaStore;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Prova o pipeline de consumo (Kafka -&gt; AlertaStore -&gt; GET /alertas) sem
 * broker real, via conector {@code smallrye-in-memory} (ativado em
 * {@code %test}, ver application.properties). Mesmo espírito do teste do
 * lado produtor no backend
 * (EstoqueBaixoAtingidoKafkaPublisherTest.java), mas para o lado
 * consumidor: injeta a mensagem via {@code connector.source(canal)}, não
 * {@code connector.sink(canal)}. Mesmo com o conector in-memory, o
 * processamento do {@code @Incoming} passa pelo pipeline reativo e não é
 * síncrono em relação ao {@code send()} — por isso o {@code await()}
 * (Awaitility), conforme a própria documentação do Quarkus recomenda para
 * este cenário.
 */
@QuarkusTest
class EstoqueBaixoAtingidoConsumerTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    AlertaStore alertaStore;

    @Inject
    AlertaResource alertaResource;

    private static EstoqueBaixoAtingidoRecebido mensagemDeTeste() {
        return new EstoqueBaixoAtingidoRecebido(UUID.randomUUID(), "BEB-001",
                new BigDecimal("4.000"), new BigDecimal("5.000"), OffsetDateTime.now());
    }

    @Test
    void mensagemRecebidaApareceNoStoreENoEndpointRest() {
        InMemorySource<EstoqueBaixoAtingidoRecebido> canal = connector.source("estoque-baixo-atingido");

        canal.send(mensagemDeTeste());

        await().atMost(Duration.ofSeconds(5)).until(() -> !alertaStore.listarTodos().isEmpty());

        assertThat(alertaStore.listarTodos()).hasSize(1);
        assertThat(alertaResource.listar().get(0).sku()).isEqualTo("BEB-001");
    }
}
