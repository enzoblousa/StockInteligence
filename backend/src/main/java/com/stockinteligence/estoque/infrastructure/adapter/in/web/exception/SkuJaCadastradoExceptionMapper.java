package com.stockinteligence.estoque.infrastructure.adapter.in.web.exception;

import com.stockinteligence.estoque.domain.model.SkuJaCadastradoException;
import com.stockinteligence.estoque.infrastructure.adapter.in.web.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SkuJaCadastradoExceptionMapper implements ExceptionMapper<SkuJaCadastradoException> {

    @Override
    public Response toResponse(SkuJaCadastradoException exception) {
        return Response.status(Response.Status.CONFLICT).entity(new ErroResponse(exception.getMessage())).build();
    }
}
