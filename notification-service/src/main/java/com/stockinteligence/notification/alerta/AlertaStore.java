package com.stockinteligence.notification.alerta;

import com.stockinteligence.notification.messaging.EstoqueBaixoAtingidoRecebido;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Armazenamento em memória — decisão deliberada: sem banco, sem volume
 * persistente. Perde o histórico quando o pod reciclar, mesma limitação
 * já aceita para o Kafka em si (infra/openshift/README.md § 8). Alertas
 * são notificações, não registro de negócio — o dado real (saldo de
 * estoque) continua seguro no Postgres do backend principal.
 *
 * Capacidade limitada a {@link #MAX_ALERTAS} para não crescer sem limite
 * num pod de longa duração; descarta o mais antigo primeiro (FIFO) ao
 * ultrapassar. {@code synchronized} em vez de uma estrutura lock-free
 * porque "adicionar + (talvez) remover o mais antigo" é uma operação
 * composta que precisa ser atômica — o volume esperado (alertas de
 * estoque baixo) é baixíssimo, sem motivo de performance para
 * complexidade extra aqui.
 *
 * <p><b>Atenção:</b> assume um único pod
 * (ver infra/openshift/19-notification-deployment.yaml, replicas: 1). Se
 * este serviço for escalado para múltiplas réplicas no futuro, cada
 * instância teria seu próprio {@code AlertaStore} isolado —
 * {@code GET /alertas} responderia de forma inconsistente dependendo de
 * qual pod atendeu a requisição. Resolver isso exigiria voltar a um
 * armazenamento compartilhado (banco), o que contraria a decisão atual.
 */
@ApplicationScoped
public class AlertaStore {

    private static final int MAX_ALERTAS = 100;

    private final Deque<AlertaRecebido> alertas = new ArrayDeque<>(MAX_ALERTAS);

    public synchronized void adicionar(EstoqueBaixoAtingidoRecebido mensagem) {
        alertas.addFirst(new AlertaRecebido(UUID.randomUUID(), mensagem.produtoId(), mensagem.sku(),
                mensagem.quantidadeAtual(), mensagem.quantidadeMinima(), mensagem.ocorridoEm(),
                OffsetDateTime.now()));
        if (alertas.size() > MAX_ALERTAS) {
            alertas.removeLast();
        }
    }

    /** Mais recentes primeiro (ordem de inserção via {@code addFirst}). */
    public synchronized List<AlertaRecebido> listarTodos() {
        return List.copyOf(alertas);
    }
}
