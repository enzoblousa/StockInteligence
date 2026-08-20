package com.stockinteligence.estoque.infrastructure.adapter.out.persistence.write;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.Preco;
import com.stockinteligence.estoque.domain.model.Produto;
import com.stockinteligence.estoque.domain.model.Quantidade;
import com.stockinteligence.estoque.domain.model.SKU;
import com.stockinteligence.estoque.domain.model.SaldoEstoque;
import com.stockinteligence.estoque.domain.model.UnidadeMedida;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Prova o que só o banco real prova: round-trip de mapeamento e a
 * constraint de unicidade (uq_saldo_estoque_produto_id). Não retesta regra
 * de negócio nem orquestração — ver memory/testing-strategy.md.
 */
@QuarkusTest
class SaldoEstoqueRepositoryImplTest {

    @Inject
    SaldoEstoqueRepositoryImpl repository;

    @Inject
    ProdutoRepositoryImpl produtoRepository;

    private static SKU skuUnico(String prefixo) {
        return new SKU(prefixo + "-" + UUID.randomUUID().toString().substring(0, 8));
    }

    private Produto produtoSemeado() {
        Produto produto = Produto.cadastrar(skuUnico("SLD-RT"), "Refrigerante 2L", Categoria.BEBIDAS,
                UnidadeMedida.UN, new Preco(new BigDecimal("4.50")), new Preco(new BigDecimal("7.90")));
        produtoRepository.salvar(produto);
        return produto;
    }

    private static SaldoEstoque saldoPara(Produto produto) {
        return SaldoEstoque.iniciar(produto.id(), produto.sku(),
                new Quantidade(new BigDecimal("10")), new Quantidade(new BigDecimal("5")));
    }

    @Test
    void deveSalvarEBuscarPorProdutoId() {
        Produto produto = produtoSemeado();
        SaldoEstoque saldo = saldoPara(produto);

        repository.salvar(saldo);

        assertThat(repository.buscarPorProdutoId(produto.id()))
                .isPresent()
                .get()
                .satisfies(recuperado -> assertThat(recuperado.quantidadeAtual().valor()).isEqualByComparingTo("10"));
    }

    @Test
    void buscarPorProdutoIdInexistenteRetornaVazio() {
        assertThat(repository.buscarPorProdutoId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void existePorProdutoIdRefleteEstadoPersistido() {
        Produto produto = produtoSemeado();
        assertThat(repository.existePorProdutoId(produto.id())).isFalse();

        repository.salvar(saldoPara(produto));

        assertThat(repository.existePorProdutoId(produto.id())).isTrue();
    }

    @Test
    void naoDevePermitirDoisSaldosParaOMesmoProdutoIndiceUnico() {
        Produto produto = produtoSemeado();
        repository.salvar(saldoPara(produto));

        assertThatThrownBy(() -> repository.salvar(saldoPara(produto))).isInstanceOf(RuntimeException.class);
    }
}
