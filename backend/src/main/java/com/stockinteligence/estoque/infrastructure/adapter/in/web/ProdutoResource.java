package com.stockinteligence.estoque.infrastructure.adapter.in.web;

import com.stockinteligence.estoque.application.command.AtualizarProdutoCommand;
import com.stockinteligence.estoque.application.command.AtualizarProdutoCommandHandler;
import com.stockinteligence.estoque.application.command.CadastrarProdutoCommand;
import com.stockinteligence.estoque.application.command.CadastrarProdutoCommandHandler;
import com.stockinteligence.estoque.application.command.InativarProdutoCommand;
import com.stockinteligence.estoque.application.command.InativarProdutoCommandHandler;
import com.stockinteligence.estoque.application.command.ReativarProdutoCommand;
import com.stockinteligence.estoque.application.command.ReativarProdutoCommandHandler;
import com.stockinteligence.estoque.application.query.BuscarProdutoPorIdQuery;
import com.stockinteligence.estoque.application.query.BuscarProdutoPorSkuQuery;
import com.stockinteligence.estoque.application.query.BuscarProdutoQueryHandler;
import com.stockinteligence.estoque.application.query.ListarProdutosQuery;
import com.stockinteligence.estoque.application.query.ListarProdutosQueryHandler;
import com.stockinteligence.estoque.domain.model.Categoria;
import com.stockinteligence.estoque.domain.model.StatusProduto;
import com.stockinteligence.estoque.infrastructure.adapter.in.web.dto.AtualizarProdutoRequest;
import com.stockinteligence.estoque.infrastructure.adapter.in.web.dto.CadastrarProdutoRequest;
import com.stockinteligence.estoque.infrastructure.adapter.in.web.dto.PaginaDeProdutosResponse;
import com.stockinteligence.estoque.infrastructure.adapter.in.web.dto.ProdutoResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

/**
 * Adapter de entrada (Princípio IV): só traduz request → Command/Query e
 * despacha para o handler correspondente. Nenhuma regra de negócio aqui.
 * Rotas conforme specs/001-cadastro-produto/plan.md.
 */
@Path("/api/produtos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProdutoResource {

    private final CadastrarProdutoCommandHandler cadastrarHandler;
    private final AtualizarProdutoCommandHandler atualizarHandler;
    private final InativarProdutoCommandHandler inativarHandler;
    private final ReativarProdutoCommandHandler reativarHandler;
    private final BuscarProdutoQueryHandler buscarHandler;
    private final ListarProdutosQueryHandler listarHandler;

    @Inject
    public ProdutoResource(
            CadastrarProdutoCommandHandler cadastrarHandler,
            AtualizarProdutoCommandHandler atualizarHandler,
            InativarProdutoCommandHandler inativarHandler,
            ReativarProdutoCommandHandler reativarHandler,
            BuscarProdutoQueryHandler buscarHandler,
            ListarProdutosQueryHandler listarHandler) {
        this.cadastrarHandler = cadastrarHandler;
        this.atualizarHandler = atualizarHandler;
        this.inativarHandler = inativarHandler;
        this.reativarHandler = reativarHandler;
        this.buscarHandler = buscarHandler;
        this.listarHandler = listarHandler;
    }

    @POST
    public Response cadastrar(@Valid CadastrarProdutoRequest request) {
        UUID id = cadastrarHandler.executar(new CadastrarProdutoCommand(
                request.sku(), request.nome(), request.categoria(), request.unidadeMedida(),
                request.precoCusto(), request.precoVenda()));

        ProdutoResponse response = ProdutoResponse.de(buscarHandler.executar(new BuscarProdutoPorIdQuery(id)));
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/{id}")
    public ProdutoResponse buscarPorId(@PathParam("id") UUID id) {
        return ProdutoResponse.de(buscarHandler.executar(new BuscarProdutoPorIdQuery(id)));
    }

    @GET
    @Path("/sku/{sku}")
    public ProdutoResponse buscarPorSku(@PathParam("sku") String sku) {
        return ProdutoResponse.de(buscarHandler.executar(new BuscarProdutoPorSkuQuery(sku)));
    }

    @GET
    public PaginaDeProdutosResponse listar(
            @QueryParam("categoria") Categoria categoria,
            @QueryParam("status") StatusProduto status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return PaginaDeProdutosResponse.de(listarHandler.executar(new ListarProdutosQuery(categoria, status, page, size)));
    }

    @PUT
    @Path("/{id}")
    public ProdutoResponse atualizar(@PathParam("id") UUID id, @Valid AtualizarProdutoRequest request) {
        atualizarHandler.executar(new AtualizarProdutoCommand(
                id, request.nome(), request.categoria(), request.unidadeMedida(),
                request.precoCusto(), request.precoVenda()));

        return ProdutoResponse.de(buscarHandler.executar(new BuscarProdutoPorIdQuery(id)));
    }

    @PATCH
    @Path("/{id}/inativar")
    public ProdutoResponse inativar(@PathParam("id") UUID id) {
        inativarHandler.executar(new InativarProdutoCommand(id));
        return ProdutoResponse.de(buscarHandler.executar(new BuscarProdutoPorIdQuery(id)));
    }

    @PATCH
    @Path("/{id}/reativar")
    public ProdutoResponse reativar(@PathParam("id") UUID id) {
        reativarHandler.executar(new ReativarProdutoCommand(id));
        return ProdutoResponse.de(buscarHandler.executar(new BuscarProdutoPorIdQuery(id)));
    }
}
