package com.stockinteligence.estoque.application.command;

import com.stockinteligence.estoque.domain.event.EstoqueBaixoAtingido;
import com.stockinteligence.estoque.domain.model.Quantidade;
import com.stockinteligence.estoque.domain.model.SaldoEstoque;
import com.stockinteligence.estoque.domain.model.SaldoEstoqueNaoEncontradoException;
import com.stockinteligence.estoque.domain.repository.SaldoEstoqueRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Trata {@link RegistrarSaidaEstoqueCommand} (US-3, US-5).
 *
 * <p><b>Desvio deliberado</b> do padrão estreito de {@code @Transactional}
 * da feature 001 (lá, a anotação fica só no repositório): aqui ela envolve
 * o método inteiro, porque o CDI {@code Event<EstoqueBaixoAtingido>} precisa
 * ser disparado com uma transação JTA ainda ativa para que
 * {@code TransactionPhase.AFTER_SUCCESS} no observador
 * ({@code EstoqueBaixoAtingidoKafkaPublisher}, infrastructure) tenha efeito
 * real — sem isso, o evento seria entregue imediatamente, fora de qualquer
 * transação, tornando a fase decorativa (specs/002-alerta-estoque-baixo/plan.md
 * § Decisões técnicas).
 */
@ApplicationScoped
public class RegistrarSaidaEstoqueCommandHandler {

    private final SaldoEstoqueRepository repository;
    private final Event<EstoqueBaixoAtingido> estoqueBaixoAtingidoEvent;

    @Inject
    public RegistrarSaidaEstoqueCommandHandler(
            SaldoEstoqueRepository repository,
            Event<EstoqueBaixoAtingido> estoqueBaixoAtingidoEvent) {
        this.repository = repository;
        this.estoqueBaixoAtingidoEvent = estoqueBaixoAtingidoEvent;
    }

    @Transactional
    public void executar(RegistrarSaidaEstoqueCommand command) {
        SaldoEstoque saldo = repository.buscarPorProdutoId(command.produtoId())
                .orElseThrow(() -> new SaldoEstoqueNaoEncontradoException(command.produtoId()));

        saldo.registrarSaida(new Quantidade(command.quantidade()));
        repository.salvar(saldo);

        saldo.eventosPendentes().forEach(evento -> estoqueBaixoAtingidoEvent.fire((EstoqueBaixoAtingido) evento));
        saldo.limparEventosPendentes();
    }
}
