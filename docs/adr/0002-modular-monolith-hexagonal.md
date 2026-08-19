# ADR-0002: Monólito modular com arquitetura hexagonal (não microsserviços)

Status: **Aceito** · Data: 2026-08-18

## Contexto

O domínio (catálogo, parceiros, estoque, compras, vendas, alertas) é coeso e de escopo
controlado (MVP explicitamente limitado — ver `docs/spec/00-vision.md`). Microsserviços trariam
consistência eventual entre "estoque" e "vendas" exatamente onde a spec exige consistência forte
(RF-VND-5, RNF-1).

## Decisão

Um único serviço deployável, dividido internamente em módulos por bounded context
(`catalog`, `partners`, `inventory`, `purchasing`, `sales`, `notifications`), cada um em camadas
`domain` / `application` / `infrastructure` (portas e adaptadores). Módulos só se comunicam via
portas de aplicação explícitas (interfaces), nunca acessando repositório interno de outro módulo.

## Alternativas consideradas

- **Microsserviços por bounded context**: complexidade operacional (deploy distribuído, tracing
  cross-serviço, consistência eventual) desproporcional ao tamanho do domínio; adiada para se o
  projeto crescer de verdade além do MVP.
- **CRUD simples em camadas (controller-service-repository) sem hexagonal**: mais rápido de
  escrever, mas não demonstra separação de domínio/infraestrutura — objetivo explícito do projeto
  é mostrar isso (ver `00-vision.md`, seção "por que este projeto").

## Consequências

- Mais boilerplate (interfaces de porta) do que um CRUD direto — aceito como custo de
  demonstrar a competência arquitetural.
- Fronteiras de módulo bem definidas facilitam extrair um módulo para serviço separado no futuro,
  se necessário.
- Exige disciplina: revisões de código devem vetar import direto de `infrastructure`/`repository`
  de outro módulo.
