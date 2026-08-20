package com.stockinteligence.estoque.infrastructure.adapter.out.persistence.read;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockinteligence.estoque.application.query.ProdutoResult;
import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.Preco;
import com.stockinteligence.estoque.domain.model.Produto;
import com.stockinteligence.estoque.domain.model.SKU;
import com.stockinteligence.estoque.domain.model.StatusProduto;
import com.stockinteligence.estoque.domain.model.UnidadeMedida;
import com.stockinteligence.estoque.infrastructure.adapter.out.persistence.write.ProdutoRepositoryImpl;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Prova o que só o banco real prova neste side: corretude de query e
 * filtro/paginação. Não retesta regra de negócio nem orquestração — ver
 * memory/testing-strategy.md. Não assume contagem total da tabela (outros
 * testes/execuções também escrevem nela) — assertions são sempre
 * localizadas em SKUs únicos gerados no próprio teste.
 */
@QuarkusTest
class ProdutoQueryRepositoryImplTest {

    @Inject
    ProdutoRepositoryImpl writeRepository;

    @Inject
    ProdutoQueryRepositoryImpl queryRepository;

    private static SKU skuUnico(String prefixo) {
        return new SKU(prefixo + "-" + UUID.randomUUID().toString().substring(0, 8));
    }

    private Produto semear(SKU sku, Categoria categoria, UnidadeMedida unidadeMedida) {
        Produto produto = Produto.cadastrar(sku, "Produto " + sku.valor(), categoria, unidadeMedida,
                new Preco(new BigDecimal("4.50")), new Preco(new BigDecimal("7.90")));
        writeRepository.salvar(produto);
        return produto;
    }

    @Test
    void deveBuscarPorIdExistente() {
        Produto produto = semear(skuUnico("RQ-ID"), Categoria.BEBIDAS, UnidadeMedida.UN);

        assertThat(queryRepository.buscarPorId(produto.id()))
                .isPresent()
                .get()
                .extracting(ProdutoResult::sku)
                .isEqualTo(produto.sku().valor());
    }

    @Test
    void buscarPorIdInexistenteRetornaVazio() {
        assertThat(queryRepository.buscarPorId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void deveBuscarPorSkuExistente() {
        SKU sku = skuUnico("RQ-SKU");
        semear(sku, Categoria.LIMPEZA, UnidadeMedida.UN);

        assertThat(queryRepository.buscarPorSku(sku.valor())).isPresent();
    }

    @Test
    void buscarPorSkuInexistenteRetornaVazio() {
        assertThat(queryRepository.buscarPorSku("SKU-QUE-NAO-EXISTE-000")).isEmpty();
    }

    @Test
    void listarComFiltroDeCategoriaRetornaApenasCorrespondentes() {
        SKU skuEletronico = skuUnico("RQ-CAT-ELE");
        SKU skuBebida = skuUnico("RQ-CAT-BEB");
        semear(skuEletronico, Categoria.ELETRONICOS, UnidadeMedida.UN);
        semear(skuBebida, Categoria.BEBIDAS, UnidadeMedida.UN);

        var pagina = queryRepository.listar(Categoria.ELETRONICOS, null, 0, 100);

        assertThat(pagina.conteudo()).extracting(ProdutoResult::sku).contains(skuEletronico.valor())
                .doesNotContain(skuBebida.valor());
    }

    @Test
    void listarComFiltroDeStatusRetornaApenasCorrespondentes() {
        SKU skuAtivo = skuUnico("RQ-STA-ATIVO");
        SKU skuInativo = skuUnico("RQ-STA-INATIVO");
        semear(skuAtivo, Categoria.OUTROS, UnidadeMedida.UN);
        Produto inativado = semear(skuInativo, Categoria.OUTROS, UnidadeMedida.UN);
        inativado.inativar();
        writeRepository.salvar(inativado);

        var pagina = queryRepository.listar(null, StatusProduto.INATIVO, 0, 100);

        assertThat(pagina.conteudo()).extracting(ProdutoResult::sku).contains(skuInativo.valor())
                .doesNotContain(skuAtivo.valor());
    }

    @Test
    void listarRespeitaPaginacao() {
        String prefixo = "RQ-PAG-" + UUID.randomUUID().toString().substring(0, 6);
        for (int i = 0; i < 3; i++) {
            semear(new SKU(prefixo + "-" + i), Categoria.VESTUARIO, UnidadeMedida.UN);
        }

        var primeiraPagina = queryRepository.listar(Categoria.VESTUARIO, null, 0, 2);
        var segundaPagina = queryRepository.listar(Categoria.VESTUARIO, null, 1, 2);

        assertThat(primeiraPagina.conteudo()).hasSizeLessThanOrEqualTo(2);
        assertThat(primeiraPagina.totalElements()).isGreaterThanOrEqualTo(3);
        assertThat(segundaPagina.page()).isEqualTo(1);
    }
}
