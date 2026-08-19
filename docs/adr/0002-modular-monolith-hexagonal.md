# ADR-0002: Monólito modular, hexagonal-lite, módulo Maven único

Status: **Aceito** · Data: 2026-08-19

## Contexto

O domínio do MVP (lojas, produtos, movimentações de estoque) é pequeno. O projeto quer
demonstrar arquitetura sólida (separação domínio/infraestrutura, testabilidade) sem incorrer em
overhead desproporcional ao tamanho real do sistema.

## Decisão

- **Um único módulo Maven** (`backend/`), sem `multi-module` — o build fica simples e rápido.
- **Separação por pacote**, não por módulo de build:
  - `domain`: entidades, invariantes, portas (interfaces de repositório), exceções de negócio.
    Zero dependência de Quarkus/JPA/JAX-RS.
  - `application`: casos de uso (services) que orquestram entidades de domínio através das
    portas, definem fronteira transacional.
  - `infrastructure`: adaptadores — REST resources (JAX-RS), repositórios JPA/Panache que
    implementam as portas do domínio, mapeamento de erro para RFC 7807, configuração.
- Fronteira entre camadas é reforçada por convenção de pacote + revisão de código (não por
  módulo de build separado) — ver detalhe em `docs/spec/03-architecture.md`.

## Alternativas consideradas

- **Hexagonal "cheio" com múltiplos módulos Maven** (`domain`, `application`, `infrastructure`
  como artefatos separados): rejeitado — ceremonial demais para um domínio deste tamanho; o
  ganho de isolamento não compensa a complexidade extra de build/versionamento.
- **MVC em camadas sem portas** (controller → service → repository concreto, sem interface de
  domínio): rejeitado — regra de negócio tende a vazar para o service acoplado a JPA/Panache,
  dificultando teste unitário puro (requisito não-negociável, ver `CLAUDE.md` regra 9).
- **Microsserviços:** descartado sem debate sério — não há equipe nem escala que justifique
  fronteira de rede entre "produtos" e "movimentações".

## Consequências

- Se o domínio crescer significativamente pós-MVP (ex.: pedidos multi-item, relatórios pesados),
  revisitar esta decisão — pode fazer sentido separar em módulos Maven nesse ponto.
- Visibilidade de pacote (`package-private`) é usada onde possível para impedir que
  `infrastructure` vaze detalhes para `domain`.
