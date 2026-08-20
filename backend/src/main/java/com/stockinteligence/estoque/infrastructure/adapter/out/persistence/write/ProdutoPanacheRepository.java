package com.stockinteligence.estoque.infrastructure.adapter.out.persistence.write;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/** Repositório Panache técnico — detalhe de implementação de {@link ProdutoRepositoryImpl}. */
@ApplicationScoped
public class ProdutoPanacheRepository implements PanacheRepositoryBase<ProdutoJpaEntity, UUID> {
}
