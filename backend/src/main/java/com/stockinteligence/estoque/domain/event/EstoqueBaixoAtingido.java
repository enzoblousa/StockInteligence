package com.stockinteligence.estoque.domain.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Fato: o saldo de um produto cruzou o limiar da quantidade mínima (estava
 * acima e caiu para igual/abaixo) em uma saída de estoque bem-sucedida
 * (specs/002-alerta-estoque-baixo/spec.md US-5, FR-008). Primeiro Domain
 * Event real do projeto — ver
 * {@link com.stockinteligence.estoque.domain.model.SaldoEstoque#registrarSaida}
 * para a regra de quando ele é sinalizado.
 */
public record EstoqueBaixoAtingido(
        UUID produtoId,
        String sku,
        BigDecimal quantidadeAtual,
        BigDecimal quantidadeMinima,
        OffsetDateTime ocorridoEm) implements DomainEvent {
}
