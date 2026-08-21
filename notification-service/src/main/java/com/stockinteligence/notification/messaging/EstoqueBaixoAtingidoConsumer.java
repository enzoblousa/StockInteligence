package com.stockinteligence.notification.messaging;

import com.stockinteligence.notification.alerta.AlertaStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * Consome o tópico Kafka {@code estoque.baixo-atingido} (publicado pelo
 * backend — ver EstoqueBaixoAtingidoKafkaPublisher lá) e guarda no
 * {@link AlertaStore}. Método {@code void} sem {@code Message<T>}/ack
 * manual: a estratégia de commit padrão do conector {@code smallrye-kafka}
 * ({@code throttled}) commita o offset automaticamente após o método
 * retornar com sucesso — suficiente para este caso simples, sem
 * processamento assíncrono.
 */
@ApplicationScoped
public class EstoqueBaixoAtingidoConsumer {

    private final AlertaStore store;

    @Inject
    public EstoqueBaixoAtingidoConsumer(AlertaStore store) {
        this.store = store;
    }

    @Incoming("estoque-baixo-atingido")
    void aoReceber(EstoqueBaixoAtingidoRecebido mensagem) {
        store.adicionar(mensagem);
    }
}
