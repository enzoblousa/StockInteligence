package com.stockinteligence.estoque.infrastructure.adapter.out.persistence.write;

import com.stockinteligence.estoque.domain.model.Preco;
import com.stockinteligence.estoque.domain.model.Produto;
import com.stockinteligence.estoque.domain.model.SKU;
import com.stockinteligence.estoque.domain.model.StatusProduto;
import com.stockinteligence.estoque.domain.repository.ProdutoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementa {@link ProdutoRepository} (write side). Converte
 * {@link Produto} ↔ {@link ProdutoJpaEntity} nas duas direções — o
 * agregado nunca é anotado com JPA (Princípio III).
 */
@ApplicationScoped
public class ProdutoRepositoryImpl implements ProdutoRepository {

    private final ProdutoPanacheRepository panache;

    @Inject
    public ProdutoRepositoryImpl(ProdutoPanacheRepository panache) {
        this.panache = panache;
    }

    @Override
    @Transactional
    public void salvar(Produto produto) {
        ProdutoJpaEntity entity = panache.findByIdOptional(produto.id()).orElseGet(ProdutoJpaEntity::new);
        preencher(entity, produto);
        panache.persist(entity);
    }

    @Override
    public Optional<Produto> buscarPorId(UUID id) {
        return panache.findByIdOptional(id).map(this::paraAgregado);
    }

    @Override
    public Optional<Produto> buscarPorSku(SKU sku) {
        return panache.find("sku", sku.valor()).firstResultOptional().map(this::paraAgregado);
    }

    @Override
    public boolean existeAtivoComSku(SKU sku) {
        return panache.count("sku = ?1 and status = ?2", sku.valor(), StatusProduto.ATIVO) > 0;
    }

    private void preencher(ProdutoJpaEntity entity, Produto produto) {
        entity.id = produto.id();
        entity.sku = produto.sku().valor();
        entity.nome = produto.nome();
        entity.categoria = produto.categoria();
        entity.unidadeMedida = produto.unidadeMedida();
        entity.precoCusto = produto.precoCusto().valor();
        entity.precoVenda = produto.precoVenda().valor();
        entity.status = produto.status();
    }

    private Produto paraAgregado(ProdutoJpaEntity entity) {
        return Produto.reconstituir(
                entity.id,
                new SKU(entity.sku),
                entity.nome,
                entity.categoria,
                entity.unidadeMedida,
                new Preco(entity.precoCusto),
                new Preco(entity.precoVenda),
                entity.status);
    }
}
