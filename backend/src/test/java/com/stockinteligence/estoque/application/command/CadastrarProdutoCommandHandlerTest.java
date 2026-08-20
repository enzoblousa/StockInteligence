package com.stockinteligence.estoque.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.Produto;
import com.stockinteligence.estoque.domain.model.SKU;
import com.stockinteligence.estoque.domain.model.SkuJaCadastradoException;
import com.stockinteligence.estoque.domain.model.UnidadeMedida;
import com.stockinteligence.estoque.domain.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Testa apenas orquestração (não retesta formato de SKU/preço — isso é do
 * domínio, ver memory/testing-strategy.md).
 */
class CadastrarProdutoCommandHandlerTest {

    @Mock
    private ProdutoRepository repository;

    private CadastrarProdutoCommandHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new CadastrarProdutoCommandHandler(repository);
    }

    private static CadastrarProdutoCommand comandoValido() {
        return new CadastrarProdutoCommand("BEB-001", "Refrigerante 2L", Categoria.BEBIDAS, UnidadeMedida.UN,
                new BigDecimal("4.50"), new BigDecimal("7.90"));
    }

    @Test
    void deveCadastrarQuandoSkuNaoEstaEmUsoPorAtivo() {
        when(repository.existeAtivoComSku(any())).thenReturn(false);

        UUID id = handler.executar(comandoValido());

        assertThat(id).isNotNull();
        verify(repository).salvar(any(Produto.class));
    }

    @Test
    void deveRejeitarQuandoSkuJaEstaEmUsoPorAtivo() {
        when(repository.existeAtivoComSku(new SKU("BEB-001"))).thenReturn(true);

        assertThatThrownBy(() -> handler.executar(comandoValido())).isInstanceOf(SkuJaCadastradoException.class);

        verify(repository, never()).salvar(any());
    }
}
