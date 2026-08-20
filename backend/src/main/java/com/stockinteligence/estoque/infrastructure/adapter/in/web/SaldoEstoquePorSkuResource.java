package com.stockinteligence.estoque.infrastructure.adapter.in.web;

import com.stockinteligence.estoque.application.query.BuscarSaldoEstoquePorSkuQuery;
import com.stockinteligence.estoque.application.query.BuscarSaldoEstoqueQueryHandler;
import com.stockinteligence.estoque.infrastructure.adapter.in.web.dto.SaldoEstoqueResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Busca de saldo de estoque por SKU (US-4). Classe separada de
 * {@link SaldoEstoqueResource} porque sua raiz ("/api/saldo-estoque") é
 * distinta da de {@link ProdutoResource}/{@link SaldoEstoqueResource}
 * ("/api/produtos") — ver o comentário em {@link SaldoEstoqueResource}
 * sobre por que raízes JAX-RS de granularidade diferente não podem ser
 * misturadas em uma única classe via sub-caminhos.
 */
@Path("/api/saldo-estoque")
@Produces(MediaType.APPLICATION_JSON)
public class SaldoEstoquePorSkuResource {

    private final BuscarSaldoEstoqueQueryHandler buscarHandler;

    @Inject
    public SaldoEstoquePorSkuResource(BuscarSaldoEstoqueQueryHandler buscarHandler) {
        this.buscarHandler = buscarHandler;
    }

    @GET
    @Path("/sku/{sku}")
    public SaldoEstoqueResponse buscarPorSku(@PathParam("sku") String sku) {
        return SaldoEstoqueResponse.de(buscarHandler.executar(new BuscarSaldoEstoquePorSkuQuery(sku)));
    }
}
