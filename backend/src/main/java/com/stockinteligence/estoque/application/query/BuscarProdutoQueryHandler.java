package com.stockinteligence.estoque.application.query;

import com.stockinteligence.estoque.domain.model.ProdutoNaoEncontradoException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Trata {@link BuscarProdutoPorIdQuery} e {@link BuscarProdutoPorSkuQuery}
 * (US-2). Não passa pelo agregado {@code Produto} — lê diretamente via
 * {@link ProdutoQueryRepository}.
 */
@ApplicationScoped
public class BuscarProdutoQueryHandler {

    private final ProdutoQueryRepository queryRepository;

    @Inject
    public BuscarProdutoQueryHandler(ProdutoQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public ProdutoResult executar(BuscarProdutoPorIdQuery query) {
        return queryRepository.buscarPorId(query.id())
                .orElseThrow(() -> new ProdutoNaoEncontradoException(query.id()));
    }

    public ProdutoResult executar(BuscarProdutoPorSkuQuery query) {
        return queryRepository.buscarPorSku(query.sku())
                .orElseThrow(() -> ProdutoNaoEncontradoException.paraSku(query.sku()));
    }
}
