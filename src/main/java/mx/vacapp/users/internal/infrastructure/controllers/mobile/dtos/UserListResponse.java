package mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos;

import java.util.List;

/**
 * DTO de respuesta para lista paginada de usuarios.
 * Contiene la lista de usuarios y los metadatos de paginación.
 */
public record UserListResponse(
    List<UserResponse> users,
    PaginationMetadata pagination
) {}
