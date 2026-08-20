CREATE TABLE saldo_estoque (
    id                 UUID PRIMARY KEY,
    produto_id         UUID            NOT NULL REFERENCES produto(id),
    sku                VARCHAR(50)     NOT NULL,
    quantidade         NUMERIC(14,3)   NOT NULL CHECK (quantidade >= 0),
    quantidade_minima  NUMERIC(14,3)   NOT NULL CHECK (quantidade_minima >= 0),
    atualizado_em      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Um saldo por produto: sem multi-armazém/depósito nesta feature
-- (specs/002-alerta-estoque-baixo/spec.md, Fora de escopo).
CREATE UNIQUE INDEX uq_saldo_estoque_produto_id ON saldo_estoque (produto_id);
CREATE INDEX ix_saldo_estoque_sku ON saldo_estoque (sku);
