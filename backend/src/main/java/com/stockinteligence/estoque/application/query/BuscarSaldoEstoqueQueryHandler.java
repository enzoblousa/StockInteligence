package com.stockinteligence.estoque.application.query;

import com.stockinteligence.estoque.domain.model.SaldoEstoqueNaoEncontradoException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Trata as consultas de saldo de estoque (US-4). */
@ApplicationScoped
public class BuscarSaldoEstoqueQueryHandler {

    private final SaldoEstoqueQueryRepository queryRepository;

    @Inject
    public BuscarSaldoEstoqueQueryHandler(SaldoEstoqueQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public SaldoEstoqueResult executar(BuscarSaldoEstoquePorProdutoIdQuery query) {
        return queryRepository.buscarPorProdutoId(query.produtoId())
                .orElseThrow(() -> new SaldoEstoqueNaoEncontradoException(query.produtoId()));
    }

    public SaldoEstoqueResult executar(BuscarSaldoEstoquePorSkuQuery query) {
        return queryRepository.buscarPorSku(query.sku())
                .orElseThrow(() -> SaldoEstoqueNaoEncontradoException.paraSku(query.sku()));
    }
}
