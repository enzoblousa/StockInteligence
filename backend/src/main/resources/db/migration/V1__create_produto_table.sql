CREATE TABLE produto (
    id              UUID PRIMARY KEY,
    sku             VARCHAR(50)     NOT NULL,
    nome            VARCHAR(200)    NOT NULL,
    categoria       VARCHAR(30)     NOT NULL,
    unidade_medida  VARCHAR(10)     NOT NULL,
    preco_custo     NUMERIC(12,2)   NOT NULL CHECK (preco_custo >= 0),
    preco_venda     NUMERIC(12,2)   NOT NULL CHECK (preco_venda >= 0),
    status          VARCHAR(10)     NOT NULL,
    criado_em       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Unicidade de SKU apenas entre produtos ATIVOs (specs/001-cadastro-produto/plan.md).
CREATE UNIQUE INDEX uq_produto_sku_ativo ON produto (sku) WHERE status = 'ATIVO';

CREATE INDEX ix_produto_categoria ON produto (categoria);
CREATE INDEX ix_produto_status ON produto (status);
