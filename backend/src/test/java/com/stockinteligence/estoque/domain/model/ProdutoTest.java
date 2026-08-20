package com.stockinteligence.estoque.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Cobertura exaustiva das invariantes do agregado Produto — camada dona
 * (memory/testing-strategy.md). CommandHandlers e testes de integração não
 * retestam este conjunto de cenários, apenas orquestração/HTTP.
 */
class ProdutoTest {

    private static final SKU SKU_VALIDO = new SKU("BEB-001");
    private static final Preco PRECO_CUSTO = new Preco(new BigDecimal("4.50"));
    private static final Preco PRECO_VENDA = new Preco(new BigDecimal("7.90"));

    private static Produto produtoValido() {
        return Produto.cadastrar(SKU_VALIDO, "Refrigerante 2L", Categoria.BEBIDAS, UnidadeMedida.UN, PRECO_CUSTO,
                PRECO_VENDA);
    }

    @Test
    void deveCadastrarProdutoValidoComoAtivo() {
        Produto produto = produtoValido();

        assertThat(produto.id()).isNotNull();
        assertThat(produto.sku()).isEqualTo(SKU_VALIDO);
        assertThat(produto.status()).isEqualTo(StatusProduto.ATIVO);
    }

    @Test
    void doisProdutosCadastradosTemIdsDiferentes() {
        Produto a = produtoValido();
        Produto b = produtoValido();

        assertThat(a.id()).isNotEqualTo(b.id());
    }

    @Test
    void deveRejeitarNomeEmBranco() {
        assertThatThrownBy(() -> Produto.cadastrar(SKU_VALIDO, "  ", Categoria.BEBIDAS, UnidadeMedida.UN,
                PRECO_CUSTO, PRECO_VENDA)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarNomeMuitoLongo() {
        String nomeGigante = "A".repeat(201);

        assertThatThrownBy(() -> Produto.cadastrar(SKU_VALIDO, nomeGigante, Categoria.BEBIDAS, UnidadeMedida.UN,
                PRECO_CUSTO, PRECO_VENDA)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void atualizarDadosNaoAlteraIdNemSku() {
        Produto produto = produtoValido();
        var idOriginal = produto.id();
        var skuOriginal = produto.sku();

        produto.atualizarDados("Refrigerante 2L Cola", Categoria.BEBIDAS, UnidadeMedida.UN, PRECO_CUSTO,
                new Preco(new BigDecimal("8.50")));

        assertThat(produto.id()).isEqualTo(idOriginal);
        assertThat(produto.sku()).isEqualTo(skuOriginal);
        assertThat(produto.nome()).isEqualTo("Refrigerante 2L Cola");
        assertThat(produto.precoVenda().valor()).isEqualByComparingTo("8.50");
    }

    @Test
    void deveInativarProdutoAtivo() {
        Produto produto = produtoValido();

        produto.inativar();

        assertThat(produto.status()).isEqualTo(StatusProduto.INATIVO);
    }

    @Test
    void naoDeveInativarProdutoJaInativo() {
        Produto produto = produtoValido();
        produto.inativar();

        assertThatThrownBy(produto::inativar).isInstanceOf(TransicaoDeStatusInvalidaException.class);
    }

    @Test
    void deveReativarProdutoInativo() {
        Produto produto = produtoValido();
        produto.inativar();

        produto.reativar();

        assertThat(produto.status()).isEqualTo(StatusProduto.ATIVO);
    }

    @Test
    void naoDeveReativarProdutoJaAtivo() {
        Produto produto = produtoValido();

        assertThatThrownBy(produto::reativar).isInstanceOf(TransicaoDeStatusInvalidaException.class);
    }
}
