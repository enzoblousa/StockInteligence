package com.stockinteligence.estoque.infrastructure.adapter.out.messaging;

import com.stockinteligence.estoque.domain.event.EstoqueBaixoAtingido;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

/**
 * Observa {@link EstoqueBaixoAtingido} apenas <b>após o commit</b> da
 * transação que o originou ({@code TransactionPhase.AFTER_SUCCESS}) —
 * evita publicar um alerta de operação que sofreu rollback (US-5, FR-009) —
 * e publica no tópico Kafka {@code estoque.baixo-atingido}
 * (canal {@code estoque-baixo-atingido}, ver application.properties).
 *
 * <p><b>Limitação conhecida</b> (specs/002-alerta-estoque-baixo/plan.md §
 * Decisões técnicas): isto não é um Transactional Outbox completo. Entre o
 * commit da transação PostgreSQL e {@code emitter.send}, não há garantia de
 * dual-write — uma falha do processo nesse intervalo perde o alerta
 * silenciosamente. Aceitável para o MVP desta spec (YAGNI, Princípio VI);
 * evolução futura se confiabilidade de entrega virar requisito de negócio
 * (outbox table + poller, ou CDC).
 */
@ApplicationScoped
public class EstoqueBaixoAtingidoKafkaPublisher {

    private final Emitter<EstoqueBaixoAtingidoMensagem> emitter;

    @Inject
    public EstoqueBaixoAtingidoKafkaPublisher(
            @Channel("estoque-baixo-atingido") Emitter<EstoqueBaixoAtingidoMensagem> emitter) {
        this.emitter = emitter;
    }

    void aoDetectarEstoqueBaixo(@Observes(during = TransactionPhase.AFTER_SUCCESS) EstoqueBaixoAtingido evento) {
        emitter.send(EstoqueBaixoAtingidoMensagem.de(evento));
    }
}
