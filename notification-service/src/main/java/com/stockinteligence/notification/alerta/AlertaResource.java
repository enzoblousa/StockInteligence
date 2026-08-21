package com.stockinteligence.notification.alerta;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/** Único endpoint deste serviço — só leitura, sem regra de negócio (Princípio VI/YAGNI, ver plan.md). */
@Path("/alertas")
@Produces(MediaType.APPLICATION_JSON)
public class AlertaResource {

    private final AlertaStore store;

    @Inject
    public AlertaResource(AlertaStore store) {
        this.store = store;
    }

    @GET
    public List<AlertaRecebido> listar() {
        return store.listarTodos();
    }
}
