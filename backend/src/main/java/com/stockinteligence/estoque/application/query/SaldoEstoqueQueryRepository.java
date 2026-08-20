package com.stockinteligence.estoque.application.query;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de leitura (Princípio II — CQRS). Exclusiva do read side: sua
 * implementação não passa pelo agregado {@code SaldoEstoque} nem por
 * {@link com.stockinteligence.estoque.domain.repository.SaldoEstoqueRepository}.
 * Implementação concreta em
 * infrastructure.adapter.out.persistence.read.SaldoEstoqueQueryRepositoryImpl.
 */
public interface SaldoEstoqueQueryRepository {

    Optional<SaldoEstoqueResult> buscarPorProdutoId(UUID produtoId);

    Optional<SaldoEstoqueResult> buscarPorSku(String sku);
}
