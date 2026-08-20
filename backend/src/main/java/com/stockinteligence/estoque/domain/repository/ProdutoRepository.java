package com.stockinteligence.estoque.domain.repository;

import com.stockinteligence.estoque.domain.model.Produto;
import com.stockinteligence.estoque.domain.model.SKU;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída do write side (Princípio IV — contratos explícitos).
 * Implementação concreta em
 * infrastructure.adapter.out.persistence.write.ProdutoRepositoryImpl.
 *
 * Expõe apenas as operações que o command side precisa — nunca um CRUD
 * genérico (memory/constitution.md, Princípio I).
 */
public interface ProdutoRepository {

    void salvar(Produto produto);

    Optional<Produto> buscarPorId(UUID id);

    Optional<Produto> buscarPorSku(SKU sku);

    boolean existeAtivoComSku(SKU sku);
}
