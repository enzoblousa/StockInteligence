package com.stockinteligence.estoque.application.command;

import com.stockinteligence.estoque.domain.model.Preco;
import com.stockinteligence.estoque.domain.model.Produto;
import com.stockinteligence.estoque.domain.model.ProdutoNaoEncontradoException;
import com.stockinteligence.estoque.domain.repository.ProdutoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Trata {@link AtualizarProdutoCommand} (US-4).
 */
@ApplicationScoped
public class AtualizarProdutoCommandHandler {

    private final ProdutoRepository repository;

    @Inject
    public AtualizarProdutoCommandHandler(ProdutoRepository repository) {
        this.repository = repository;
    }

    public void executar(AtualizarProdutoCommand command) {
        Produto produto = repository.buscarPorId(command.id())
                .orElseThrow(() -> new ProdutoNaoEncontradoException(command.id()));

        produto.atualizarDados(
                command.nome(),
                command.categoria(),
                command.unidadeMedida(),
                new Preco(command.precoCusto()),
                new Preco(command.precoVenda()));

        repository.salvar(produto);
    }
}
