package mx.vacapp.users.internal.application.usecases.commands;

/**
 * Comando inmutable para iniciar sesión.
 * Record utilizado para pasar datos desde el controlador al caso de uso LoginUseCase.
 */
public record LoginCommand(
    String email,
    String password,
    String clientIp,
    String userAgent
) {}
