package com.stockinteligence.estoque.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root do domínio de estoque (specs/001-cadastro-produto).
 *
 * Toda alteração de estado passa por esta classe — nunca há mutação direta
 * de campo fora dela. SKU é imutável após a criação (FR-004); id é
 * {@code UUID} puro, sem Value Object dedicado (plan.md § Decisões
 * técnicas, corte de simplicidade).
 */
public class Produto {

    private static final int NOME_MAX_LENGTH = 200;

    private final UUID id;
    private final SKU sku;
    private String nome;
    private Categoria categoria;
    private UnidadeMedida unidadeMedida;
    private Preco precoCusto;
    private Preco precoVenda;
    private StatusProduto status;

    private Produto(UUID id, SKU sku, String nome, Categoria categoria, UnidadeMedida unidadeMedida,
            Preco precoCusto, Preco precoVenda, StatusProduto status) {
        this.id = Objects.requireNonNull(id, "id é obrigatório.");
        this.sku = Objects.requireNonNull(sku, "sku é obrigatório.");
        this.nome = validarNome(nome);
        this.categoria = Objects.requireNonNull(categoria, "categoria é obrigatória.");
        this.unidadeMedida = Objects.requireNonNull(unidadeMedida, "unidadeMedida é obrigatória.");
        this.precoCusto = Objects.requireNonNull(precoCusto, "precoCusto é obrigatório.");
        this.precoVenda = Objects.requireNonNull(precoVenda, "precoVenda é obrigatório.");
        this.status = Objects.requireNonNull(status, "status é obrigatório.");
    }

    /** Cadastra um novo produto (US-1): gera id e nasce com status ATIVO. */
    public static Produto cadastrar(SKU sku, String nome, Categoria categoria, UnidadeMedida unidadeMedida,
            Preco precoCusto, Preco precoVenda) {
        return new Produto(UUID.randomUUID(), sku, nome, categoria, unidadeMedida, precoCusto, precoVenda,
                StatusProduto.ATIVO);
    }

    /** Reconstitui um produto já existente a partir do estado persistido. */
    public static Produto reconstituir(UUID id, SKU sku, String nome, Categoria categoria,
            UnidadeMedida unidadeMedida, Preco precoCusto, Preco precoVenda, StatusProduto status) {
        return new Produto(id, sku, nome, categoria, unidadeMedida, precoCusto, precoVenda, status);
    }

    /** Edita os dados cadastrais (US-4). SKU nunca é alterado (FR-004). */
    public void atualizarDados(String nome, Categoria categoria, UnidadeMedida unidadeMedida, Preco precoCusto,
            Preco precoVenda) {
        this.nome = validarNome(nome);
        this.categoria = Objects.requireNonNull(categoria, "categoria é obrigatória.");
        this.unidadeMedida = Objects.requireNonNull(unidadeMedida, "unidadeMedida é obrigatória.");
        this.precoCusto = Objects.requireNonNull(precoCusto, "precoCusto é obrigatório.");
        this.precoVenda = Objects.requireNonNull(precoVenda, "precoVenda é obrigatório.");
    }

    /** Inativa o produto (US-5). Só permitido a partir de ATIVO. */
    public void inativar() {
        if (status != StatusProduto.ATIVO) {
            throw new TransicaoDeStatusInvalidaException("Produto %s já está inativo.".formatted(id));
        }
        status = StatusProduto.INATIVO;
    }

    /**
     * Reativa o produto (US-5). Só permitido a partir de INATIVO. A
     * unicidade de SKU entre produtos ativos é responsabilidade do
     * CommandHandler (via repositório) — o agregado não tem visão de
     * outros agregados.
     */
    public void reativar() {
        if (status != StatusProduto.INATIVO) {
            throw new TransicaoDeStatusInvalidaException("Produto %s já está ativo.".formatted(id));
        }
        status = StatusProduto.ATIVO;
    }

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do produto é obrigatório.");
        }
        if (nome.length() > NOME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Nome do produto deve ter no máximo %d caracteres.".formatted(NOME_MAX_LENGTH));
        }
        return nome;
    }

    public UUID id() {
        return id;
    }

    public SKU sku() {
        return sku;
    }

    public String nome() {
        return nome;
    }

    public Categoria categoria() {
        return categoria;
    }

    public UnidadeMedida unidadeMedida() {
        return unidadeMedida;
    }

    public Preco precoCusto() {
        return precoCusto;
    }

    public Preco precoVenda() {
        return precoVenda;
    }

    public StatusProduto status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Produto produto)) {
            return false;
        }
        return id.equals(produto.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
