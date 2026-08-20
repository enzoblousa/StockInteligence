package com.stockinteligence.estoque.application.command;

import com.stockinteligence.estoque.domain.model.Quantidade;
import com.stockinteligence.estoque.domain.model.SaldoEstoque;
import com.stockinteligence.estoque.domain.model.SaldoEstoqueNaoEncontradoException;
import com.stockinteligence.estoque.domain.repository.SaldoEstoqueRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Trata {@link RegistrarEntradaEstoqueCommand} (US-2). Entrada nunca
 * sinaliza {@code EstoqueBaixoAtingido} — mantém o padrão estreito de
 * {@code @Transactional} da feature 001, com a transação delegada a
 * {@link SaldoEstoqueRepository#salvar} (ver
 * {@link RegistrarSaidaEstoqueCommandHandler} para o desvio deliberado
 * necessário quando há evento de domínio a publicar).
 */
@ApplicationScoped
public class RegistrarEntradaEstoqueCommandHandler {

    private final SaldoEstoqueRepository repository;

    @Inject
    public RegistrarEntradaEstoqueCommandHandler(SaldoEstoqueRepository repository) {
        this.repository = repository;
    }

    public void executar(RegistrarEntradaEstoqueCommand command) {
        SaldoEstoque saldo = repository.buscarPorProdutoId(command.produtoId())
                .orElseThrow(() -> new SaldoEstoqueNaoEncontradoException(command.produtoId()));

        saldo.registrarEntrada(new Quantidade(command.quantidade()));

        repository.salvar(saldo);
    }
}
