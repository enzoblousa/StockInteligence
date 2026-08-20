package com.stockinteligence.estoque.application.command;

import com.stockinteligence.estoque.domain.model.Preco;
import com.stockinteligence.estoque.domain.model.Produto;
import com.stockinteligence.estoque.domain.model.SKU;
import com.stockinteligence.estoque.domain.model.SkuJaCadastradoException;
import com.stockinteligence.estoque.domain.repository.ProdutoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

/**
 * Trata {@link CadastrarProdutoCommand} (US-1). Único caminho que passa
 * pelo agregado para criar um produto (Princípio II — CQRS).
 */
@ApplicationScoped
public class CadastrarProdutoCommandHandler {

    private final ProdutoRepository repository;

    @Inject
    public CadastrarProdutoCommandHandler(ProdutoRepository repository) {
        this.repository = repository;
    }

    public UUID executar(CadastrarProdutoCommand command) {
        SKU sku = new SKU(command.sku());
        if (repository.existeAtivoComSku(sku)) {
            throw new SkuJaCadastradoException(sku);
        }

        Produto produto = Produto.cadastrar(
                sku,
                command.nome(),
                command.categoria(),
                command.unidadeMedida(),
                new Preco(command.precoCusto()),
                new Preco(command.precoVenda()));

        repository.salvar(produto);
        return produto.id();
    }
}
