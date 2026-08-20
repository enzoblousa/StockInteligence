package com.stockinteligence.estoque.infrastructure.adapter.out.messaging;

import com.stockinteligence.estoque.domain.event.EstoqueBaixoAtingido;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payload publicado no tópico Kafka {@code estoque.baixo-atingido} —
 * desacoplado do tipo de domínio {@link EstoqueBaixoAtingido} (Princípio
 * III: o domínio nunca conhece o formato de serialização de mensageria).
 */
public record EstoqueBaixoAtingidoMensagem(
        UUID produtoId,
        String sku,
        BigDecimal quantidadeAtual,
        BigDecimal quantidadeMinima,
        OffsetDateTime ocorridoEm) {

    public static EstoqueBaixoAtingidoMensagem de(EstoqueBaixoAtingido evento) {
        return new EstoqueBaixoAtingidoMensagem(
                evento.produtoId(), evento.sku(), evento.quantidadeAtual(), evento.quantidadeMinima(),
                evento.ocorridoEm());
    }
}
