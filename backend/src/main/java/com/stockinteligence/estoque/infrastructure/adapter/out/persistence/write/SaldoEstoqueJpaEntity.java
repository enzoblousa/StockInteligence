package com.stockinteligence.estoque.infrastructure.adapter.out.persistence.write;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mapeamento 1:1 da tabela {@code saldo_estoque}
 * (V2__create_saldo_estoque_table.sql). Sem nenhuma regra de negócio — só
 * mapeamento (Princípio III). O agregado
 * {@link com.stockinteligence.estoque.domain.model.SaldoEstoque} nunca é
 * anotado com JPA; a conversão vive em {@link SaldoEstoqueRepositoryImpl}.
 */
@Entity
@Table(name = "saldo_estoque")
public class SaldoEstoqueJpaEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "produto_id", nullable = false)
    public UUID produtoId;

    @Column(nullable = false, length = 50)
    public String sku;

    @Column(nullable = false, precision = 14, scale = 3)
    public BigDecimal quantidade;

    @Column(name = "quantidade_minima", nullable = false, precision = 14, scale = 3)
    public BigDecimal quantidadeMinima;

    @Column(name = "atualizado_em", nullable = false)
    public OffsetDateTime atualizadoEm;

    @PrePersist
    @PreUpdate
    void aoSalvar() {
        atualizadoEm = OffsetDateTime.now();
    }
}
