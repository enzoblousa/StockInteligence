package com.stockinteligence.estoque.application.command;

import com.stockinteligence.estoque.domain.model.Produto;
import com.stockinteligence.estoque.domain.model.ProdutoNaoEncontradoException;
import com.stockinteligence.estoque.domain.model.SkuJaCadastradoException;
import com.stockinteligence.estoque.domain.repository.ProdutoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Trata {@link ReativarProdutoCommand} (US-5). Bloqueia a reativação se o
 * SKU estiver em uso por outro produto ativo (FR-003 + FR-008).
 */
@ApplicationScoped
public class ReativarProdutoCommandHandler {

    private final ProdutoRepository repository;

    @Inject
    public ReativarProdutoCommandHandler(ProdutoRepository repository) {
        this.repository = repository;
    }

    public void executar(ReativarProdutoCommand command) {
        Produto produto = repository.buscarPorId(command.id())
                .orElseThrow(() -> new ProdutoNaoEncontradoException(command.id()));

        if (repository.existeAtivoComSku(produto.sku())) {
            throw new SkuJaCadastradoException(produto.sku());
        }

        produto.reativar();

        repository.salvar(produto);
    }
}
