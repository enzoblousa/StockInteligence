package com.stockinteligence.estoque.infrastructure.adapter.in.web.exception;

import com.stockinteligence.estoque.domain.model.SaldoEstoqueNaoEncontradoException;
import com.stockinteligence.estoque.infrastructure.adapter.in.web.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SaldoEstoqueNaoEncontradoExceptionMapper implements ExceptionMapper<SaldoEstoqueNaoEncontradoException> {

    @Override
    public Response toResponse(SaldoEstoqueNaoEncontradoException exception) {
        return Response.status(Response.Status.NOT_FOUND).entity(new ErroResponse(exception.getMessage())).build();
    }
}
