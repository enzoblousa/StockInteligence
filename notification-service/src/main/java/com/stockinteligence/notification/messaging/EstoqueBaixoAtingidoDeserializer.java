package com.stockinteligence.notification.messaging;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

/**
 * {@code ObjectMapperDeserializer<T>} (ao contrário de
 * {@code ObjectMapperSerializer}, usado no lado produtor do backend) não
 * tem construtor sem argumentos — exige uma subclasse informando a classe
 * alvo via {@code super(...)}. O construtor de um argumento delega para
 * {@code ObjectMapperProducer.get()}, que devolve o {@code ObjectMapper}
 * gerenciado por CDI (produzido pela extensão {@code quarkus-rest-jackson},
 * já com {@code JavaTimeModule} registrado — por isso este módulo depende
 * de {@code quarkus-rest-jackson} mesmo só expondo um GET simples: sem essa
 * extensão o fallback é {@code new ObjectMapper()} puro, que não
 * desserializa {@code OffsetDateTime}).
 */
public class EstoqueBaixoAtingidoDeserializer extends ObjectMapperDeserializer<EstoqueBaixoAtingidoRecebido> {

    public EstoqueBaixoAtingidoDeserializer() {
        super(EstoqueBaixoAtingidoRecebido.class);
    }
}
