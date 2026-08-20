package com.stockinteligence.estoque.infrastructure.adapter.in.web.exception;

import com.stockinteligence.estoque.infrastructure.adapter.in.web.dto.ErroResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Mapeia falhas de invariante de Value Object (SKU inválido, preço
 * negativo, nome em branco/muito longo) para 400. Bean Validation dos
 * DTOs de request (ConstraintViolationException) já é mapeada para 400
 * automaticamente pelo Quarkus — não precisa de mapper próprio aqui.
 */
@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response.status(Response.Status.BAD_REQUEST).entity(new ErroResponse(exception.getMessage())).build();
    }
}
