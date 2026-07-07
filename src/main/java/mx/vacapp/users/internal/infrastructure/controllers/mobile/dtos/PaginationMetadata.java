package mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos;

/**
 * Metadata de paginación para respuestas de listas.
 * Contiene información sobre la página actual, tamaño y total de elementos.
 */
public record PaginationMetadata(
    int page,
    int size,
    long total
) {}
