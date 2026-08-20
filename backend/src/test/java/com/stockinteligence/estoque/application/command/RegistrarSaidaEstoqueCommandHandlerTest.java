package com.stockinteligence.estoque.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockinteligence.estoque.domain.event.EstoqueBaixoAtingido;
import com.stockinteligence.estoque.domain.model.Quantidade;
import com.stockinteligence.estoque.domain.model.SKU;
import com.stockinteligence.estoque.domain.model.SaldoEstoque;
import com.stockinteligence.estoque.domain.model.SaldoEstoqueNaoEncontradoException;
import com.stockinteligence.estoque.domain.model.SaldoInsuficienteException;
import com.stockinteligence.estoque.domain.repository.SaldoEstoqueRepository;
import jakarta.enterprise.event.Event;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Testa apenas orquestração (US-3), com foco no ponto central desta
 * feature: o CDI {@code Event<EstoqueBaixoAtingido>} só é disparado quando
 * o agregado de fato sinaliza o evento (SaldoEstoqueTest já cobre
 * exaustivamente quando isso acontece — ver memory/testing-strategy.md).
 */
class RegistrarSaidaEstoqueCommandHandlerTest {

    @Mock
    private SaldoEstoqueRepository repository;

    @Mock
    private Event<EstoqueBaixoAtingido> estoqueBaixoAtingidoEvent;

    private RegistrarSaidaEstoqueCommandHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new RegistrarSaidaEstoqueCommandHandler(repository, estoqueBaixoAtingidoEvent);
    }

    private static SaldoEstoque saldoExistente(UUID produtoId, String quantidadeInicial, String quantidadeMinima) {
        return SaldoEstoque.iniciar(produtoId, new SKU("BEB-001"),
                new Quantidade(new BigDecimal(quantidadeInicial)), new Quantidade(new BigDecimal(quantidadeMinima)));
    }

    @Test
    void deveRegistrarSaidaSemDispararEventoQuandoNaoCruzaOLimiar() {
        UUID produtoId = UUID.randomUUID();
        SaldoEstoque saldo = saldoExistente(produtoId, "10", "5");
        when(repository.buscarPorProdutoId(produtoId)).thenReturn(Optional.of(saldo));

        handler.executar(new RegistrarSaidaEstoqueCommand(produtoId, new BigDecimal("2")));

        assertThat(saldo.quantidadeAtual().valor()).isEqualByComparingTo("8");
        verify(estoqueBaixoAtingidoEvent, never()).fire(any());
    }

    @Test
    void deveDispararEventoExatamenteUmaVezQuandoCruzaOLimiar() {
        UUID produtoId = UUID.randomUUID();
        SaldoEstoque saldo = saldoExistente(produtoId, "10", "5");
        when(repository.buscarPorProdutoId(produtoId)).thenReturn(Optional.of(saldo));

        handler.executar(new RegistrarSaidaEstoqueCommand(produtoId, new BigDecimal("6")));

        verify(estoqueBaixoAtingidoEvent, times(1)).fire(any(EstoqueBaixoAtingido.class));
        assertThat(saldo.eventosPendentes()).isEmpty(); // já foi drenado pelo handler
    }

    @Test
    void naoDeveDispararEventoQuandoSaldoInsuficiente() {
        UUID produtoId = UUID.randomUUID();
        SaldoEstoque saldo = saldoExistente(produtoId, "10", "5");
        when(repository.buscarPorProdutoId(produtoId)).thenReturn(Optional.of(saldo));

        assertThatThrownBy(() -> handler.executar(new RegistrarSaidaEstoqueCommand(produtoId, new BigDecimal("11"))))
                .isInstanceOf(SaldoInsuficienteException.class);

        verify(estoqueBaixoAtingidoEvent, never()).fire(any());
    }

    @Test
    void deveRejeitarQuandoSaldoNaoExiste() {
        when(repository.buscarPorProdutoId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.executar(new RegistrarSaidaEstoqueCommand(UUID.randomUUID(), new BigDecimal("1"))))
                .isInstanceOf(SaldoEstoqueNaoEncontradoException.class);
    }
}
