package com.stockinteligence.estoque.domain.model;

import com.stockinteligence.estoque.domain.event.DomainEvent;
import com.stockinteligence.estoque.domain.event.EstoqueBaixoAtingido;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root do saldo de estoque de um produto
 * (specs/002-alerta-estoque-baixo). Saldo é global por produto — sem
 * multi-armazém (fora de escopo desta feature).
 *
 * Toda alteração de estado passa por esta classe. Além das invariantes
 * usuais de DDD, este agregado também é o primeiro do projeto a capturar
 * Domain Events: {@link #registrarSaida} adiciona um
 * {@link EstoqueBaixoAtingido} a {@link #eventosPendentes} quando a saída
 * faz o saldo cruzar o limiar da quantidade mínima. Quem drena e publica
 * esses eventos é o {@code CommandHandler} (application layer), nunca o
 * agregado — aqui dentro é só coleta pura, sem framework (Princípio III).
 */
public class SaldoEstoque {

    private final UUID id;
    private final UUID produtoId;
    private final SKU sku;
    private Quantidade quantidadeAtual;
    private Quantidade quantidadeMinima;
    private final List<DomainEvent> eventosPendentes = new ArrayList<>();

    private SaldoEstoque(UUID id, UUID produtoId, SKU sku, Quantidade quantidadeAtual, Quantidade quantidadeMinima) {
        this.id = Objects.requireNonNull(id, "id é obrigatório.");
        this.produtoId = Objects.requireNonNull(produtoId, "produtoId é obrigatório.");
        this.sku = Objects.requireNonNull(sku, "sku é obrigatório.");
        this.quantidadeAtual = Objects.requireNonNull(quantidadeAtual, "quantidadeAtual é obrigatória.");
        this.quantidadeMinima = Objects.requireNonNull(quantidadeMinima, "quantidadeMinima é obrigatória.");
    }

    /** Define o saldo inicial de um produto (US-1): gera id novo. */
    public static SaldoEstoque iniciar(UUID produtoId, SKU sku, Quantidade quantidadeInicial,
            Quantidade quantidadeMinima) {
        return new SaldoEstoque(UUID.randomUUID(), produtoId, sku, quantidadeInicial, quantidadeMinima);
    }

    /** Reconstitui um saldo já existente a partir do estado persistido. */
    public static SaldoEstoque reconstituir(UUID id, UUID produtoId, SKU sku, Quantidade quantidadeAtual,
            Quantidade quantidadeMinima) {
        return new SaldoEstoque(id, produtoId, sku, quantidadeAtual, quantidadeMinima);
    }

    /** Registra entrada de estoque (US-2, FR-004). Não sinaliza evento. */
    public void registrarEntrada(Quantidade quantidade) {
        validarQuantidadeDeMovimento(quantidade);
        quantidadeAtual = quantidadeAtual.somar(quantidade);
    }

    /**
     * Registra saída de estoque (US-3, FR-005/FR-006). Lança
     * {@link SaldoInsuficienteException} se a quantidade solicitada exceder
     * o saldo atual — o saldo permanece inalterado nesse caso.
     *
     * Sinaliza {@link EstoqueBaixoAtingido} apenas na <b>transição</b>: o
     * saldo estava acima da quantidade mínima antes desta saída e caiu para
     * igual ou abaixo dela depois (FR-008). Saídas subsequentes enquanto o
     * saldo já está baixo não repetem o alerta; uma entrada que devolve o
     * saldo para acima do mínimo rearma a próxima transição.
     */
    public void registrarSaida(Quantidade quantidade) {
        validarQuantidadeDeMovimento(quantidade);
        if (quantidade.compareTo(quantidadeAtual) > 0) {
            throw new SaldoInsuficienteException(produtoId, quantidadeAtual, quantidade);
        }

        boolean estavaAcimaDoMinimo = quantidadeAtual.compareTo(quantidadeMinima) > 0;
        quantidadeAtual = quantidadeAtual.subtrair(quantidade);

        if (estavaAcimaDoMinimo && quantidadeAtual.compareTo(quantidadeMinima) <= 0) {
            eventosPendentes.add(new EstoqueBaixoAtingido(
                    produtoId, sku.valor(), quantidadeAtual.valor(), quantidadeMinima.valor(), OffsetDateTime.now()));
        }
    }

    private static void validarQuantidadeDeMovimento(Quantidade quantidade) {
        if (quantidade.ehZero()) {
            throw new IllegalArgumentException("Quantidade de movimento deve ser maior que zero.");
        }
    }

    /** Cópia imutável dos eventos ainda não drenados por um CommandHandler. */
    public List<DomainEvent> eventosPendentes() {
        return List.copyOf(eventosPendentes);
    }

    /** Esvazia os eventos pendentes — chamado pelo CommandHandler após publicá-los. */
    public void limparEventosPendentes() {
        eventosPendentes.clear();
    }

    public UUID id() {
        return id;
    }

    public UUID produtoId() {
        return produtoId;
    }

    public SKU sku() {
        return sku;
    }

    public Quantidade quantidadeAtual() {
        return quantidadeAtual;
    }

    public Quantidade quantidadeMinima() {
        return quantidadeMinima;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SaldoEstoque saldoEstoque)) {
            return false;
        }
        return id.equals(saldoEstoque.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
