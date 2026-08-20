package com.stockinteligence.estoque.infrastructure.adapter.out.persistence.write;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.Preco;
import com.stockinteligence.estoque.domain.model.Produto;
import com.stockinteligence.estoque.domain.model.SKU;
import com.stockinteligence.estoque.domain.model.UnidadeMedida;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Prova o que só o banco real prova: round-trip de mapeamento e a
 * constraint de unicidade parcial (uq_produto_sku_ativo). Não retesta
 * regra de negócio nem orquestração — ver memory/testing-strategy.md.
 *
 * {@code salvar()} já é {@code @Transactional} em si mesmo (uma transação
 * por chamada, comitada ao retornar); os métodos de teste não precisam de
 * anotação própria.
 */
@QuarkusTest
class ProdutoRepositoryImplTest {

    @Inject
    ProdutoRepositoryImpl repository;

    private static SKU skuUnico(String prefixo) {
        return new SKU(prefixo + "-" + UUID.randomUUID().toString().substring(0, 8));
    }

    private static Produto produtoComSku(SKU sku) {
        return Produto.cadastrar(sku, "Refrigerante 2L", Categoria.BEBIDAS, UnidadeMedida.UN,
                new Preco(new BigDecimal("4.50")), new Preco(new BigDecimal("7.90")));
    }

    @Test
    void deveSalvarEBuscarPorId() {
        Produto produto = produtoComSku(skuUnico("RT-ID"));

        repository.salvar(produto);

        assertThat(repository.buscarPorId(produto.id()))
                .isPresent()
                .get()
                .satisfies(recuperado -> assertThat(recuperado.sku()).isEqualTo(produto.sku()));
    }

    @Test
    void deveSalvarEBuscarPorSku() {
        SKU sku = skuUnico("RT-SKU");
        Produto produto = produtoComSku(sku);

        repository.salvar(produto);

        assertThat(repository.buscarPorSku(sku)).isPresent();
    }

    @Test
    void buscarPorIdInexistenteRetornaVazio() {
        assertThat(repository.buscarPorId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void existeAtivoComSkuRefleteEstadoPersistido() {
        SKU sku = skuUnico("RT-EXISTE");
        assertThat(repository.existeAtivoComSku(sku)).isFalse();

        repository.salvar(produtoComSku(sku));

        assertThat(repository.existeAtivoComSku(sku)).isTrue();
    }

    @Test
    void naoDevePermitirDoisAtivosComMesmoSkuIndiceUnicoParcial() {
        SKU sku = skuUnico("RT-DUP");

        repository.salvar(produtoComSku(sku));

        assertThatThrownBy(() -> repository.salvar(produtoComSku(sku))).isInstanceOf(RuntimeException.class);
    }
}
