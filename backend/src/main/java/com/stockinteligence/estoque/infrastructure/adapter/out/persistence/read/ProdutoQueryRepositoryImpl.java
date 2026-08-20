package com.stockinteligence.estoque.infrastructure.adapter.out.persistence.read;

import com.stockinteligence.estoque.application.query.PaginaDeProdutos;
import com.stockinteligence.estoque.application.query.ProdutoQueryRepository;
import com.stockinteligence.estoque.application.query.ProdutoResult;
import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.StatusProduto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementa {@link ProdutoQueryRepository} (read side). Lê a tabela
 * {@code produto} via projeção JPQL direto para {@link ProdutoResult} —
 * nunca instancia {@code Produto} (agregado) nem
 * {@code ProdutoJpaEntity} completo (Princípio II — CQRS).
 */
@ApplicationScoped
public class ProdutoQueryRepositoryImpl implements ProdutoQueryRepository {

    private static final String SELECT_RESULT = """
            SELECT new com.stockinteligence.estoque.application.query.ProdutoResult(
                p.id, p.sku, p.nome, p.categoria, p.unidadeMedida, p.precoCusto, p.precoVenda, p.status)
            FROM ProdutoJpaEntity p
            """;

    private final EntityManager entityManager;

    @Inject
    public ProdutoQueryRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<ProdutoResult> buscarPorId(UUID id) {
        TypedQuery<ProdutoResult> query = entityManager
                .createQuery(SELECT_RESULT + " WHERE p.id = :id", ProdutoResult.class)
                .setParameter("id", id);
        return query.getResultStream().findFirst();
    }

    @Override
    public Optional<ProdutoResult> buscarPorSku(String sku) {
        TypedQuery<ProdutoResult> query = entityManager
                .createQuery(SELECT_RESULT + " WHERE p.sku = :sku", ProdutoResult.class)
                .setParameter("sku", sku == null ? null : sku.toUpperCase());
        return query.getResultStream().findFirst();
    }

    @Override
    public PaginaDeProdutos listar(Categoria categoria, StatusProduto status, int page, int size) {
        String whereClause = montarFiltro(categoria, status);

        TypedQuery<ProdutoResult> query = entityManager.createQuery(
                SELECT_RESULT + whereClause + " ORDER BY p.nome", ProdutoResult.class);
        aplicarParametros(query, categoria, status);
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<ProdutoResult> conteudo = query.getResultList();
        long total = contar(categoria, status);
        return new PaginaDeProdutos(conteudo, page, size, total);
    }

    private long contar(Categoria categoria, StatusProduto status) {
        String whereClause = montarFiltro(categoria, status);
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(p) FROM ProdutoJpaEntity p" + whereClause, Long.class);
        aplicarParametros(query, categoria, status);
        return query.getSingleResult();
    }

    private String montarFiltro(Categoria categoria, StatusProduto status) {
        StringBuilder condicoes = new StringBuilder();
        if (categoria != null) {
            condicoes.append(" AND p.categoria = :categoria");
        }
        if (status != null) {
            condicoes.append(" AND p.status = :status");
        }
        return condicoes.isEmpty() ? "" : " WHERE " + condicoes.substring(5);
    }

    private void aplicarParametros(jakarta.persistence.Query query, Categoria categoria, StatusProduto status) {
        if (categoria != null) {
            query.setParameter("categoria", categoria);
        }
        if (status != null) {
            query.setParameter("status", status);
        }
    }
}
