package com.stockinteligence.estoque.infrastructure.adapter.out.persistence.write;

import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.StatusProduto;
import com.stockinteligence.estoque.domain.model.UnidadeMedida;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mapeamento 1:1 da tabela {@code produto} (V1__create_produto_table.sql).
 * Sem nenhuma regra de negócio — só mapeamento (Princípio III). O agregado
 * {@link com.stockinteligence.estoque.domain.model.Produto} nunca é
 * anotado com JPA; a conversão vive em {@link ProdutoRepositoryImpl}.
 */
@Entity
@Table(name = "produto")
public class ProdutoJpaEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, length = 50)
    public String sku;

    @Column(nullable = false, length = 200)
    public String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_medida", nullable = false, length = 10)
    public UnidadeMedida unidadeMedida;

    @Column(name = "preco_custo", nullable = false, precision = 12, scale = 2)
    public BigDecimal precoCusto;

    @Column(name = "preco_venda", nullable = false, precision = 12, scale = 2)
    public BigDecimal precoVenda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public StatusProduto status;

    @Column(name = "criado_em", nullable = false)
    public OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    public OffsetDateTime atualizadoEm;

    @PrePersist
    void aoPersistir() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = OffsetDateTime.now();
    }
}
