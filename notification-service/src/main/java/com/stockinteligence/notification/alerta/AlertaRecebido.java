package com.stockinteligence.notification.alerta;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de resposta de {@code GET /alertas}. {@code id} é gerado na
 * recepção (identidade própria deste registro, nada a ver com o backend);
 * {@code recebidoEm} é quando este serviço processou a mensagem —
 * distinto de {@code ocorridoEm}, que é quando o backend detectou o
 * estoque baixo.
 */
public record AlertaRecebido(
        UUID id,
        UUID produtoId,
        String sku,
        BigDecimal quantidadeAtual,
        BigDecimal quantidadeMinima,
        OffsetDateTime ocorridoEm,
        OffsetDateTime recebidoEm) {
}
