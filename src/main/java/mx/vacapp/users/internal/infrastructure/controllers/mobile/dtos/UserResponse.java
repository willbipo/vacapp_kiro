package mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos;

import java.util.UUID;

/**
 * DTO de respuesta con datos completos de usuario.
 * Omite el campo password por seguridad.
 * 
 * Los campos deben coincidir exactamente con el esquema UserResponse definido
 * en openapi-users.yaml para mantener el enfoque Design-First.
 */
public record UserResponse(
    UUID userId,
    String email,
    String name,
    String phone,
    String role,
    String status,
    UUID tenantId,
    String createdAt,
    String updatedAt
) {}
