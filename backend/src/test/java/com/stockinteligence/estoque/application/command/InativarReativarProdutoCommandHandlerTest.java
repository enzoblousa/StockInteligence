package com.stockinteligence.estoque.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.Preco;
import com.stockinteligence.estoque.domain.model.Produto;
import com.stockinteligence.estoque.domain.model.SKU;
import com.stockinteligence.estoque.domain.model.SkuJaCadastradoException;
import com.stockinteligence.estoque.domain.model.StatusProduto;
import com.stockinteligence.estoque.domain.model.UnidadeMedida;
import com.stockinteligence.estoque.domain.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Testa apenas orquestração (US-5). A invalidade da transição em si
 * (já-inativo/já-ativo) já é coberta exaustivamente em ProdutoTest — ver
 * memory/testing-strategy.md.
 */
class InativarReativarProdutoCommandHandlerTest {

    @Mock
    private ProdutoRepository repository;

    private InativarProdutoCommandHandler inativarHandler;
    private ReativarProdutoCommandHandler reativarHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        inativarHandler = new InativarProdutoCommandHandler(repository);
        reativarHandler = new ReativarProdutoCommandHandler(repository);
    }

    private static Produto produtoAtivo() {
        return Produto.cadastrar(new SKU("BEB-001"), "Refrigerante 2L", Categoria.BEBIDAS, UnidadeMedida.UN,
                new Preco(new BigDecimal("4.50")), new Preco(new BigDecimal("7.90")));
    }

    @Test
    void deveInativarProdutoExistente() {
        Produto produto = produtoAtivo();
        when(repository.buscarPorId(produto.id())).thenReturn(Optional.of(produto));

        inativarHandler.executar(new InativarProdutoCommand(produto.id()));

        assertThat(produto.status()).isEqualTo(StatusProduto.INATIVO);
    }

    @Test
    void deveReativarProdutoExistenteQuandoSkuLivre() {
        Produto produto = produtoAtivo();
        produto.inativar();
        when(repository.buscarPorId(produto.id())).thenReturn(Optional.of(produto));
        when(repository.existeAtivoComSku(produto.sku())).thenReturn(false);

        reativarHandler.executar(new ReativarProdutoCommand(produto.id()));

        assertThat(produto.status()).isEqualTo(StatusProduto.ATIVO);
    }

    @Test
    void naoDeveReativarQuandoSkuEmUsoPorOutroProdutoAtivo() {
        Produto produto = produtoAtivo();
        produto.inativar();
        when(repository.buscarPorId(produto.id())).thenReturn(Optional.of(produto));
        when(repository.existeAtivoComSku(produto.sku())).thenReturn(true);

        assertThatThrownBy(() -> reativarHandler.executar(new ReativarProdutoCommand(produto.id())))
                .isInstanceOf(SkuJaCadastradoException.class);
        assertThat(produto.status()).isEqualTo(StatusProduto.INATIVO);
    }
}
