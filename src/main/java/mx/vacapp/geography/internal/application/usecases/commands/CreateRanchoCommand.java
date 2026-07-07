package mx.vacapp.geography.internal.application.usecases.commands;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando para crear un nuevo rancho.
 * Inmutable record utilizado para pasar datos al caso de uso CreateRanchoUseCase.
 */
public record CreateRanchoCommand(
    String nombre,
    BigDecimal superficieTotal,
    String descripcion,
    UUID tenantId,
    UUID userId
) {
}
