package com.stockinteligence.estoque.infrastructure.adapter.out.persistence.read;

import com.stockinteligence.estoque.application.query.SaldoEstoqueQueryRepository;
import com.stockinteligence.estoque.application.query.SaldoEstoqueResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementa {@link SaldoEstoqueQueryRepository} (read side). Lê a tabela
 * {@code saldo_estoque} via projeção JPQL direto para
 * {@link SaldoEstoqueResult} — nunca instancia {@code SaldoEstoque}
 * (agregado) nem {@code SaldoEstoqueJpaEntity} completo (Princípio II —
 * CQRS). {@code abaixoDoMinimo} é calculado na própria query.
 */
@ApplicationScoped
public class SaldoEstoqueQueryRepositoryImpl implements SaldoEstoqueQueryRepository {

    private static final String SELECT_RESULT = """
            SELECT new com.stockinteligence.estoque.application.query.SaldoEstoqueResult(
                s.id, s.produtoId, s.sku, s.quantidade, s.quantidadeMinima,
                CASE WHEN s.quantidade <= s.quantidadeMinima THEN true ELSE false END)
            FROM SaldoEstoqueJpaEntity s
            """;

    private final EntityManager entityManager;

    @Inject
    public SaldoEstoqueQueryRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<SaldoEstoqueResult> buscarPorProdutoId(UUID produtoId) {
        TypedQuery<SaldoEstoqueResult> query = entityManager
                .createQuery(SELECT_RESULT + " WHERE s.produtoId = :produtoId", SaldoEstoqueResult.class)
                .setParameter("produtoId", produtoId);
        return query.getResultStream().findFirst();
    }

    @Override
    public Optional<SaldoEstoqueResult> buscarPorSku(String sku) {
        TypedQuery<SaldoEstoqueResult> query = entityManager
                .createQuery(SELECT_RESULT + " WHERE s.sku = :sku", SaldoEstoqueResult.class)
                .setParameter("sku", sku == null ? null : sku.toUpperCase());
        return query.getResultStream().findFirst();
    }
}
