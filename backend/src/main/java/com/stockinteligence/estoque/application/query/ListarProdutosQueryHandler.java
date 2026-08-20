package com.stockinteligence.estoque.application.query;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Trata {@link ListarProdutosQuery} (US-3). */
@ApplicationScoped
public class ListarProdutosQueryHandler {

    private final ProdutoQueryRepository queryRepository;

    @Inject
    public ListarProdutosQueryHandler(ProdutoQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public PaginaDeProdutos executar(ListarProdutosQuery query) {
        return queryRepository.listar(query.categoria(), query.status(), query.page(), query.size());
    }
}
