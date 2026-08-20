package com.stockinteligence.estoque.application.query;

import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.StatusProduto;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de leitura (Princípio II — CQRS). Exclusiva do read side: nunca é
 * usada pelo command side, e sua implementação não passa pelo agregado
 * {@code Produto} nem por {@link com.stockinteligence.estoque.domain.repository.ProdutoRepository}.
 * Implementação concreta em
 * infrastructure.adapter.out.persistence.read.ProdutoQueryRepositoryImpl.
 */
public interface ProdutoQueryRepository {

    Optional<ProdutoResult> buscarPorId(UUID id);

    Optional<ProdutoResult> buscarPorSku(String sku);

    PaginaDeProdutos listar(Categoria categoria, StatusProduto status, int page, int size);
}
