package com.stockinteligence.estoque.infrastructure.adapter.out.persistence.write;

import com.stockinteligence.estoque.domain.model.Quantidade;
import com.stockinteligence.estoque.domain.model.SKU;
import com.stockinteligence.estoque.domain.model.SaldoEstoque;
import com.stockinteligence.estoque.domain.repository.SaldoEstoqueRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementa {@link SaldoEstoqueRepository} (write side). Converte
 * {@link SaldoEstoque} ↔ {@link SaldoEstoqueJpaEntity} nas duas direções —
 * o agregado nunca é anotado com JPA (Princípio III).
 */
@ApplicationScoped
public class SaldoEstoqueRepositoryImpl implements SaldoEstoqueRepository {

    private final SaldoEstoquePanacheRepository panache;

    @Inject
    public SaldoEstoqueRepositoryImpl(SaldoEstoquePanacheRepository panache) {
        this.panache = panache;
    }

    @Override
    @Transactional
    public void salvar(SaldoEstoque saldoEstoque) {
        SaldoEstoqueJpaEntity entity = panache.findByIdOptional(saldoEstoque.id()).orElseGet(SaldoEstoqueJpaEntity::new);
        preencher(entity, saldoEstoque);
        panache.persist(entity);
    }

    @Override
    public Optional<SaldoEstoque> buscarPorProdutoId(UUID produtoId) {
        return panache.find("produtoId", produtoId).firstResultOptional().map(this::paraAgregado);
    }

    @Override
    public boolean existePorProdutoId(UUID produtoId) {
        return panache.count("produtoId", produtoId) > 0;
    }

    private void preencher(SaldoEstoqueJpaEntity entity, SaldoEstoque saldoEstoque) {
        entity.id = saldoEstoque.id();
        entity.produtoId = saldoEstoque.produtoId();
        entity.sku = saldoEstoque.sku().valor();
        entity.quantidade = saldoEstoque.quantidadeAtual().valor();
        entity.quantidadeMinima = saldoEstoque.quantidadeMinima().valor();
    }

    private SaldoEstoque paraAgregado(SaldoEstoqueJpaEntity entity) {
        return SaldoEstoque.reconstituir(
                entity.id,
                entity.produtoId,
                new SKU(entity.sku),
                new Quantidade(entity.quantidade),
                new Quantidade(entity.quantidadeMinima));
    }
}
