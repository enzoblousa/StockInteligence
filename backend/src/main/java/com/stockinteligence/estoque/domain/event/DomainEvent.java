package com.stockinteligence.estoque.domain.event;

/**
 * Marca um fato de negócio relevante já ocorrido, capturado por um agregado
 * (memory/constitution.md, Princípio I). Interface pura — sem dependência de
 * framework; a publicação (in-process via CDI, ou externa via mensageria) é
 * responsabilidade da infraestrutura, nunca do domínio.
 */
public interface DomainEvent {
}
