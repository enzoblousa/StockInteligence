package com.stockinteligence.estoque.infrastructure.adapter.out.persistence.read;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockinteligence.estoque.application.query.SaldoEstoqueResult;
import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.Preco;
import com.stockinteligence.estoque.domain.model.Produto;
import com.stockinteligence.estoque.domain.model.Quantidade;
import com.stockinteligence.estoque.domain.model.SKU;
import com.stockinteligence.estoque.domain.model.SaldoEstoque;
import com.stockinteligence.estoque.domain.model.UnidadeMedida;
import com.stockinteligence.estoque.infrastructure.adapter.out.persistence.write.ProdutoRepositoryImpl;
import com.stockinteligence.estoque.infrastructure.adapter.out.persistence.write.SaldoEstoqueRepositoryImpl;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Prova o que só o banco real prova neste side: corretude de query e do
 * cálculo de {@code abaixoDoMinimo}. Não retesta regra de negócio nem
 * orquestração — ver memory/testing-strategy.md.
 */
@QuarkusTest
class SaldoEstoqueQueryRepositoryImplTest {

    @Inject
    ProdutoRepositoryImpl produtoRepository;

    @Inject
    SaldoEstoqueRepositoryImpl writeRepository;

    @Inject
    SaldoEstoqueQueryRepositoryImpl queryRepository;

    private static SKU skuUnico(String prefixo) {
        return new SKU(prefixo + "-" + UUID.randomUUID().toString().substring(0, 8));
    }

    private Produto produtoSemeado(String prefixo) {
        Produto produto = Produto.cadastrar(skuUnico(prefixo), "Produto " + prefixo, Categoria.BEBIDAS,
                UnidadeMedida.UN, new Preco(new BigDecimal("4.50")), new Preco(new BigDecimal("7.90")));
        produtoRepository.salvar(produto);
        return produto;
    }

    private SaldoEstoque semearSaldo(Produto produto, String quantidadeAtual, String quantidadeMinima) {
        SaldoEstoque saldo = SaldoEstoque.iniciar(produto.id(), produto.sku(),
                new Quantidade(new BigDecimal(quantidadeAtual)), new Quantidade(new BigDecimal(quantidadeMinima)));
        writeRepository.salvar(saldo);
        return saldo;
    }

    @Test
    void deveBuscarPorProdutoIdExistente() {
        Produto produto = produtoSemeado("RQS-ID");
        semearSaldo(produto, "10", "5");

        assertThat(queryRepository.buscarPorProdutoId(produto.id()))
                .isPresent()
                .get()
                .extracting(SaldoEstoqueResult::quantidadeAtual)
                .satisfies(quantidade -> assertThat(quantidade).isEqualByComparingTo("10.000"));
    }

    @Test
    void buscarPorProdutoIdInexistenteRetornaVazio() {
        assertThat(queryRepository.buscarPorProdutoId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void deveBuscarPorSkuExistente() {
        Produto produto = produtoSemeado("RQS-SKU");
        semearSaldo(produto, "10", "5");

        assertThat(queryRepository.buscarPorSku(produto.sku().valor())).isPresent();
    }

    @Test
    void buscarPorSkuInexistenteRetornaVazio() {
        assertThat(queryRepository.buscarPorSku("SKU-QUE-NAO-EXISTE-000")).isEmpty();
    }

    @Test
    void abaixoDoMinimoEhFalsoQuandoAcimaDoMinimo() {
        Produto produto = produtoSemeado("RQS-ACIMA");
        semearSaldo(produto, "10", "5");

        assertThat(queryRepository.buscarPorProdutoId(produto.id())).get()
                .extracting(SaldoEstoqueResult::abaixoDoMinimo).isEqualTo(false);
    }

    @Test
    void abaixoDoMinimoEhVerdadeiroQuandoIgualOuAbaixoDoMinimo() {
        Produto produto = produtoSemeado("RQS-ABAIXO");
        semearSaldo(produto, "5", "5");

        assertThat(queryRepository.buscarPorProdutoId(produto.id())).get()
                .extracting(SaldoEstoqueResult::abaixoDoMinimo).isEqualTo(true);
    }
}
