package com.stockinteligence.estoque.infrastructure.adapter.in.web.dto;

import com.stockinteligence.estoque.application.query.ProdutoResult;
import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.StatusProduto;
import com.stockinteligence.estoque.domain.model.UnidadeMedida;
import java.math.BigDecimal;
import java.util.UUID;

/** Corpo de resposta com os dados de um produto (US-2, US-3). */
public record ProdutoResponse(
        UUID id,
        String sku,
        String nome,
        Categoria categoria,
        UnidadeMedida unidadeMedida,
        BigDecimal precoCusto,
        BigDecimal precoVenda,
        StatusProduto status) {

    public static ProdutoResponse de(ProdutoResult result) {
        return new ProdutoResponse(result.id(), result.sku(), result.nome(), result.categoria(),
                result.unidadeMedida(), result.precoCusto(), result.precoVenda(), result.status());
    }
}
