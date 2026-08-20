package com.stockinteligence.estoque.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.Preco;
import com.stockinteligence.estoque.domain.model.Produto;
import com.stockinteligence.estoque.domain.model.ProdutoNaoEncontradoException;
import com.stockinteligence.estoque.domain.model.SKU;
import com.stockinteligence.estoque.domain.model.UnidadeMedida;
import com.stockinteligence.estoque.domain.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AtualizarProdutoCommandHandlerTest {

    @Mock
    private ProdutoRepository repository;

    private AtualizarProdutoCommandHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new AtualizarProdutoCommandHandler(repository);
    }

    @Test
    void deveAtualizarProdutoExistente() {
        Produto existente = Produto.cadastrar(new SKU("BEB-001"), "Refrigerante 2L", Categoria.BEBIDAS,
                UnidadeMedida.UN, new Preco(new BigDecimal("4.50")), new Preco(new BigDecimal("7.90")));
        when(repository.buscarPorId(existente.id())).thenReturn(Optional.of(existente));

        AtualizarProdutoCommand command = new AtualizarProdutoCommand(existente.id(), "Refrigerante 2L Cola",
                Categoria.BEBIDAS, UnidadeMedida.UN, new BigDecimal("4.50"), new BigDecimal("8.50"));
        handler.executar(command);

        assertThat(existente.nome()).isEqualTo("Refrigerante 2L Cola");
        verify(repository).salvar(existente);
    }

    @Test
    void deveRejeitarQuandoProdutoNaoExiste() {
        UUID idInexistente = UUID.randomUUID();
        when(repository.buscarPorId(idInexistente)).thenReturn(Optional.empty());

        AtualizarProdutoCommand command = new AtualizarProdutoCommand(idInexistente, "Nome", Categoria.BEBIDAS,
                UnidadeMedida.UN, new BigDecimal("1.00"), new BigDecimal("2.00"));

        assertThatThrownBy(() -> handler.executar(command)).isInstanceOf(ProdutoNaoEncontradoException.class);
        verify(repository, org.mockito.Mockito.never()).salvar(any());
    }
}
