package mx.vacapp.users.internal.infrastructure.controllers.mobile.dtos;

import java.util.UUID;

/**
 * DTO de respuesta para autenticación exitosa.
 * Contiene el token JWT y datos básicos del usuario.
 */
public record LoginResponse(
    String token,
    UUID userId,
    String email,
    String name,
    String role,
    UUID tenantId
) {}
