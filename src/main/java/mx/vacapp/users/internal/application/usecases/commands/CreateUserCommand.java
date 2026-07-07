package mx.vacapp.users.internal.application.usecases.commands;

import java.util.UUID;

/**
 * Comando inmutable para crear un nuevo usuario.
 * Record utilizado para pasar datos desde el controlador al caso de uso CreateUserUseCase.
 */
public record CreateUserCommand(
    String email,
    String name,
    String phone,
    String password,
    String role,
    UUID tenantId,
    UUID createdBy
) {}
