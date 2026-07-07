package mx.vacapp.users.internal.application.usecases.commands;

import java.util.UUID;

/**
 * Comando inmutable para actualizar un usuario existente.
 * Record utilizado para pasar datos desde el controlador al caso de uso UpdateUserUseCase.
 */
public record UpdateUserCommand(
    UUID userId,
    String name,
    String phone,
    String role,
    UUID updatedBy
) {}
