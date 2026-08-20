package com.stockinteligence.estoque.infrastructure.adapter.out.persistence.write;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/** Repositório Panache técnico — detalhe de implementação de {@link SaldoEstoqueRepositoryImpl}. */
@ApplicationScoped
public class SaldoEstoquePanacheRepository implements PanacheRepositoryBase<SaldoEstoqueJpaEntity, UUID> {
}
