package mx.vacapp.geography.internal.application.usecases.commands;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando para crear una nueva sección dentro de un rancho.
 */
public record CreateSeccionCommand(
    String nombre,
    BigDecimal superficie,
    UUID ranchoId,
    String descripcion,
    UUID tenantId,
    UUID userId
) {
}
