package com.stockinteligence.estoque.infrastructure.adapter.in.web.dto;

import com.stockinteligence.estoque.application.query.PaginaDeProdutos;
import java.util.List;

/** Corpo de resposta de {@code GET /api/produtos} (US-3). */
public record PaginaDeProdutosResponse(List<ProdutoResponse> content, int page, int size, long totalElements) {

    public static PaginaDeProdutosResponse de(PaginaDeProdutos pagina) {
        List<ProdutoResponse> content = pagina.conteudo().stream().map(ProdutoResponse::de).toList();
        return new PaginaDeProdutosResponse(content, pagina.page(), pagina.size(), pagina.totalElements());
    }
}
