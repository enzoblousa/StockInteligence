package com.stockinteligence.estoque.domain.repository;

import com.stockinteligence.estoque.domain.model.SaldoEstoque;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída do write side (Princípio IV — contratos explícitos).
 * Implementação concreta em
 * infrastructure.adapter.out.persistence.write.SaldoEstoqueRepositoryImpl.
 *
 * Expõe apenas as operações que o command side precisa — nunca um CRUD
 * genérico (memory/constitution.md, Princípio I).
 */
public interface SaldoEstoqueRepository {

    void salvar(SaldoEstoque saldoEstoque);

    Optional<SaldoEstoque> buscarPorProdutoId(UUID produtoId);

    boolean existePorProdutoId(UUID produtoId);
}
