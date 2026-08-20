package com.stockinteligence.estoque.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.stockinteligence.estoque.domain.model.Quantidade;
import com.stockinteligence.estoque.domain.model.SKU;
import com.stockinteligence.estoque.domain.model.SaldoEstoque;
import com.stockinteligence.estoque.domain.model.SaldoEstoqueNaoEncontradoException;
import com.stockinteligence.estoque.domain.repository.SaldoEstoqueRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Testa apenas orquestração (US-2). A regra de soma em si já é coberta
 * exaustivamente em SaldoEstoqueTest — ver memory/testing-strategy.md.
 */
class RegistrarEntradaEstoqueCommandHandlerTest {

    @Mock
    private SaldoEstoqueRepository repository;

    private RegistrarEntradaEstoqueCommandHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new RegistrarEntradaEstoqueCommandHandler(repository);
    }

    private static SaldoEstoque saldoExistente(UUID produtoId) {
        return SaldoEstoque.iniciar(produtoId, new SKU("BEB-001"),
                new Quantidade(new BigDecimal("10")), new Quantidade(new BigDecimal("5")));
    }

    @Test
    void deveRegistrarEntradaQuandoSaldoExiste() {
        UUID produtoId = UUID.randomUUID();
        SaldoEstoque saldo = saldoExistente(produtoId);
        when(repository.buscarPorProdutoId(produtoId)).thenReturn(Optional.of(saldo));

        handler.executar(new RegistrarEntradaEstoqueCommand(produtoId, new BigDecimal("3")));

        assertThat(saldo.quantidadeAtual().valor()).isEqualByComparingTo("13");
    }

    @Test
    void deveRejeitarQuandoSaldoNaoExiste() {
        when(repository.buscarPorProdutoId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.executar(new RegistrarEntradaEstoqueCommand(UUID.randomUUID(), new BigDecimal("3"))))
                .isInstanceOf(SaldoEstoqueNaoEncontradoException.class);
    }
}
