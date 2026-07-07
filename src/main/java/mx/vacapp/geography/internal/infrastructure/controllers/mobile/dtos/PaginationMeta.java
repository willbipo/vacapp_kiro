package mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos;

/**
 * DTO Response para metadatos de paginación.
 * Contiene información de la página actual, tamaño y total de elementos.
 */
public record PaginationMeta(
    int page,   // Página actual (0-based)
    int size,   // Tamaño de página
    long total  // Total de elementos
) {
}
