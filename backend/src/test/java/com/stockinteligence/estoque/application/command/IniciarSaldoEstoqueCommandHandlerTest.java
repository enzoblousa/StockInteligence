package com.stockinteligence.estoque.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.Preco;
import com.stockinteligence.estoque.domain.model.Produto;
import com.stockinteligence.estoque.domain.model.ProdutoNaoEncontradoException;
import com.stockinteligence.estoque.domain.model.SKU;
import com.stockinteligence.estoque.domain.model.SaldoEstoque;
import com.stockinteligence.estoque.domain.model.SaldoEstoqueJaExisteException;
import com.stockinteligence.estoque.domain.model.UnidadeMedida;
import com.stockinteligence.estoque.domain.repository.ProdutoRepository;
import com.stockinteligence.estoque.domain.repository.SaldoEstoqueRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Testa apenas orquestração (US-1). A invalidade de quantidade em si já é
 * coberta exaustivamente em QuantidadeTest — ver memory/testing-strategy.md.
 */
class IniciarSaldoEstoqueCommandHandlerTest {

    @Mock
    private SaldoEstoqueRepository saldoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    private IniciarSaldoEstoqueCommandHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new IniciarSaldoEstoqueCommandHandler(saldoRepository, produtoRepository);
    }

    private static Produto produtoAtivo() {
        return Produto.cadastrar(new SKU("BEB-001"), "Refrigerante 2L", Categoria.BEBIDAS, UnidadeMedida.UN,
                new Preco(new BigDecimal("4.50")), new Preco(new BigDecimal("7.90")));
    }

    @Test
    void deveIniciarSaldoQuandoProdutoExisteESaldoNaoExiste() {
        Produto produto = produtoAtivo();
        when(produtoRepository.buscarPorId(produto.id())).thenReturn(Optional.of(produto));
        when(saldoRepository.existePorProdutoId(produto.id())).thenReturn(false);

        var id = handler.executar(new IniciarSaldoEstoqueCommand(produto.id(), new BigDecimal("10"), new BigDecimal("5")));

        assertThat(id).isNotNull();
        verify(saldoRepository).salvar(any(SaldoEstoque.class));
    }

    @Test
    void deveRejeitarQuandoProdutoNaoExiste() {
        when(produtoRepository.buscarPorId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.executar(
                new IniciarSaldoEstoqueCommand(java.util.UUID.randomUUID(), new BigDecimal("10"), new BigDecimal("5"))))
                .isInstanceOf(ProdutoNaoEncontradoException.class);

        verify(saldoRepository, never()).salvar(any());
    }

    @Test
    void deveRejeitarQuandoSaldoJaExiste() {
        Produto produto = produtoAtivo();
        when(produtoRepository.buscarPorId(produto.id())).thenReturn(Optional.of(produto));
        when(saldoRepository.existePorProdutoId(produto.id())).thenReturn(true);

        assertThatThrownBy(() -> handler.executar(
                new IniciarSaldoEstoqueCommand(produto.id(), new BigDecimal("10"), new BigDecimal("5"))))
                .isInstanceOf(SaldoEstoqueJaExisteException.class);

        verify(saldoRepository, never()).salvar(any());
    }
}
