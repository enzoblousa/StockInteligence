package com.stockinteligence.estoque.infrastructure.adapter.in.web;

import com.stockinteligence.estoque.application.command.IniciarSaldoEstoqueCommand;
import com.stockinteligence.estoque.application.command.IniciarSaldoEstoqueCommandHandler;
import com.stockinteligence.estoque.application.command.RegistrarEntradaEstoqueCommand;
import com.stockinteligence.estoque.application.command.RegistrarEntradaEstoqueCommandHandler;
import com.stockinteligence.estoque.application.command.RegistrarSaidaEstoqueCommand;
import com.stockinteligence.estoque.application.command.RegistrarSaidaEstoqueCommandHandler;
import com.stockinteligence.estoque.application.query.BuscarSaldoEstoquePorProdutoIdQuery;
import com.stockinteligence.estoque.application.query.BuscarSaldoEstoqueQueryHandler;
import com.stockinteligence.estoque.infrastructure.adapter.in.web.dto.IniciarSaldoEstoqueRequest;
import com.stockinteligence.estoque.infrastructure.adapter.in.web.dto.RegistrarMovimentoEstoqueRequest;
import com.stockinteligence.estoque.infrastructure.adapter.in.web.dto.SaldoEstoqueResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

/**
 * Adapter de entrada (Princípio IV): só traduz request → Command/Query e
 * despacha para o handler correspondente. Nenhuma regra de negócio aqui.
 * Rotas conforme specs/002-alerta-estoque-baixo/plan.md.
 *
 * Raiz igual à de {@link ProdutoResource} ("/api/produtos") deliberadamente
 * — misturar classes JAX-RS com raízes de granularidade diferente
 * (ex.: uma em "/api/produtos" e outra em "/api" com sub-rotas
 * "/produtos/...") confunde o roteador do RESTEasy Reactive em tempo de
 * execução (o endpoint aparece listado na página 404 de diagnóstico, mas
 * nunca é de fato casado). A busca por SKU, que não cabe sob esta raiz,
 * fica em {@link SaldoEstoquePorSkuResource}.
 */
@Path("/api/produtos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SaldoEstoqueResource {

    private final IniciarSaldoEstoqueCommandHandler iniciarHandler;
    private final RegistrarEntradaEstoqueCommandHandler entradaHandler;
    private final RegistrarSaidaEstoqueCommandHandler saidaHandler;
    private final BuscarSaldoEstoqueQueryHandler buscarHandler;

    @Inject
    public SaldoEstoqueResource(
            IniciarSaldoEstoqueCommandHandler iniciarHandler,
            RegistrarEntradaEstoqueCommandHandler entradaHandler,
            RegistrarSaidaEstoqueCommandHandler saidaHandler,
            BuscarSaldoEstoqueQueryHandler buscarHandler) {
        this.iniciarHandler = iniciarHandler;
        this.entradaHandler = entradaHandler;
        this.saidaHandler = saidaHandler;
        this.buscarHandler = buscarHandler;
    }

    @POST
    @Path("/{produtoId}/saldo-estoque")
    public Response iniciar(@PathParam("produtoId") UUID produtoId, @Valid IniciarSaldoEstoqueRequest request) {
        iniciarHandler.executar(new IniciarSaldoEstoqueCommand(produtoId, request.quantidadeInicial(), request.quantidadeMinima()));

        SaldoEstoqueResponse response = SaldoEstoqueResponse.de(
                buscarHandler.executar(new BuscarSaldoEstoquePorProdutoIdQuery(produtoId)));
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @POST
    @Path("/{produtoId}/saldo-estoque/entradas")
    public SaldoEstoqueResponse registrarEntrada(@PathParam("produtoId") UUID produtoId,
            @Valid RegistrarMovimentoEstoqueRequest request) {
        entradaHandler.executar(new RegistrarEntradaEstoqueCommand(produtoId, request.quantidade()));
        return SaldoEstoqueResponse.de(buscarHandler.executar(new BuscarSaldoEstoquePorProdutoIdQuery(produtoId)));
    }

    @POST
    @Path("/{produtoId}/saldo-estoque/saidas")
    public SaldoEstoqueResponse registrarSaida(@PathParam("produtoId") UUID produtoId,
            @Valid RegistrarMovimentoEstoqueRequest request) {
        saidaHandler.executar(new RegistrarSaidaEstoqueCommand(produtoId, request.quantidade()));
        return SaldoEstoqueResponse.de(buscarHandler.executar(new BuscarSaldoEstoquePorProdutoIdQuery(produtoId)));
    }

    @GET
    @Path("/{produtoId}/saldo-estoque")
    public SaldoEstoqueResponse buscarPorProdutoId(@PathParam("produtoId") UUID produtoId) {
        return SaldoEstoqueResponse.de(buscarHandler.executar(new BuscarSaldoEstoquePorProdutoIdQuery(produtoId)));
    }
}
