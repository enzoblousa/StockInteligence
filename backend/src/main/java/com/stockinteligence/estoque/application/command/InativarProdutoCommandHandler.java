package com.stockinteligence.estoque.application.command;

import com.stockinteligence.estoque.domain.model.Produto;
import com.stockinteligence.estoque.domain.model.ProdutoNaoEncontradoException;
import com.stockinteligence.estoque.domain.repository.ProdutoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Trata {@link InativarProdutoCommand} (US-5).
 */
@ApplicationScoped
public class InativarProdutoCommandHandler {

    private final ProdutoRepository repository;

    @Inject
    public InativarProdutoCommandHandler(ProdutoRepository repository) {
        this.repository = repository;
    }

    public void executar(InativarProdutoCommand command) {
        Produto produto = repository.buscarPorId(command.id())
                .orElseThrow(() -> new ProdutoNaoEncontradoException(command.id()));

        produto.inativar();

        repository.salvar(produto);
    }
}
