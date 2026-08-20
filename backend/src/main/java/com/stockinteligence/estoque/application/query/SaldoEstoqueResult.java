package com.stockinteligence.estoque.application.query;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de leitura do saldo de estoque (US-4). Sem regra de negócio nem
 * invariantes (Princípio II — CQRS); construído diretamente por projeção,
 * sem passar pelo agregado {@code SaldoEstoque}. {@code abaixoDoMinimo} é
 * calculado na própria query, não recomputado aqui.
 */
public record SaldoEstoqueResult(
        UUID id,
        UUID produtoId,
        String sku,
        BigDecimal quantidadeAtual,
        BigDecimal quantidadeMinima,
        boolean abaixoDoMinimo) {
}
