package com.stockinteligence.estoque.infrastructure.adapter.in.web.dto;

import com.stockinteligence.estoque.application.query.SaldoEstoqueResult;
import java.math.BigDecimal;
import java.util.UUID;

/** Corpo de resposta com os dados do saldo de estoque de um produto (US-4). */
public record SaldoEstoqueResponse(
        UUID id,
        UUID produtoId,
        String sku,
        BigDecimal quantidadeAtual,
        BigDecimal quantidadeMinima,
        boolean abaixoDoMinimo) {

    public static SaldoEstoqueResponse de(SaldoEstoqueResult result) {
        return new SaldoEstoqueResponse(result.id(), result.produtoId(), result.sku(), result.quantidadeAtual(),
                result.quantidadeMinima(), result.abaixoDoMinimo());
    }
}
