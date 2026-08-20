package com.stockinteligence.estoque.application.command;

import com.stockinteligence.estoque.domain.model.Produto;
import com.stockinteligence.estoque.domain.model.ProdutoNaoEncontradoException;
import com.stockinteligence.estoque.domain.model.Quantidade;
import com.stockinteligence.estoque.domain.model.SaldoEstoque;
import com.stockinteligence.estoque.domain.model.SaldoEstoqueJaExisteException;
import com.stockinteligence.estoque.domain.repository.ProdutoRepository;
import com.stockinteligence.estoque.domain.repository.SaldoEstoqueRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

/**
 * Trata {@link IniciarSaldoEstoqueCommand} (US-1). Depende de
 * {@link ProdutoRepository} (feature 001) só para validar que o produto
 * existe e obter seu SKU — nunca chama métodos de negócio do agregado
 * {@link Produto}.
 */
@ApplicationScoped
public class IniciarSaldoEstoqueCommandHandler {

    private final SaldoEstoqueRepository saldoRepository;
    private final ProdutoRepository produtoRepository;

    @Inject
    public IniciarSaldoEstoqueCommandHandler(SaldoEstoqueRepository saldoRepository, ProdutoRepository produtoRepository) {
        this.saldoRepository = saldoRepository;
        this.produtoRepository = produtoRepository;
    }

    public UUID executar(IniciarSaldoEstoqueCommand command) {
        Produto produto = produtoRepository.buscarPorId(command.produtoId())
                .orElseThrow(() -> new ProdutoNaoEncontradoException(command.produtoId()));

        if (saldoRepository.existePorProdutoId(command.produtoId())) {
            throw new SaldoEstoqueJaExisteException(command.produtoId());
        }

        SaldoEstoque saldo = SaldoEstoque.iniciar(
                produto.id(),
                produto.sku(),
                new Quantidade(command.quantidadeInicial()),
                new Quantidade(command.quantidadeMinima()));

        saldoRepository.salvar(saldo);
        return saldo.id();
    }
}
