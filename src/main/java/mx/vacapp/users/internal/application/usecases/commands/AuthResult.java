package mx.vacapp.users.internal.application.usecases.commands;

import java.util.UUID;

/**
 * Resultado inmutable de la autenticación exitosa.
 * Record que contiene el token JWT y la información básica del usuario autenticado.
 */
public record AuthResult(
    String token,
    UUID userId,
    String email,
    String name,
    String role,
    UUID tenantId
) {}
