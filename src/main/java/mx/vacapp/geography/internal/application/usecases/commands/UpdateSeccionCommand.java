package mx.vacapp.geography.internal.application.usecases.commands;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando para actualizar una sección existente.
 */
public record UpdateSeccionCommand(
    UUID seccionId,
    String nombre,
    BigDecimal superficie,
    String descripcion,
    UUID userId
) {
}
