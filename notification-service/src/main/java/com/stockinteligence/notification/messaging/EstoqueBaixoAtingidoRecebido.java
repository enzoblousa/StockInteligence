package com.stockinteligence.notification.messaging;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cópia mínima e independente do payload publicado pelo backend em
 * {@code EstoqueBaixoAtingidoMensagem}
 * (backend/src/main/java/com/stockinteligence/estoque/infrastructure/adapter/out/messaging/EstoqueBaixoAtingidoMensagem.java) —
 * deliberadamente duplicada, não compartilhada via módulo comum
 * (independência de microsserviço: os dois lados não têm nenhum código em
 * comum, só concordam no formato JSON do tópico
 * {@code estoque.baixo-atingido}). Jackson casa os campos por nome de
 * propriedade — não precisa de {@code @JsonProperty} aqui, os nomes já
 * batem com o produtor.
 */
public record EstoqueBaixoAtingidoRecebido(
        UUID produtoId,
        String sku,
        BigDecimal quantidadeAtual,
        BigDecimal quantidadeMinima,
        OffsetDateTime ocorridoEm) {
}
