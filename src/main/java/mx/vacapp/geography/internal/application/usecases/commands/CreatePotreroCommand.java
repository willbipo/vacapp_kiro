package mx.vacapp.geography.internal.application.usecases.commands;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando para crear un nuevo potrero.
 * El potrero puede estar vinculado directamente a un rancho (seccionId = null)
 * o vinculado a una sección (seccionId != null).
 */
public record CreatePotreroCommand(
    String nombre,
    BigDecimal superficie,
    UUID ranchoId,
    UUID seccionId, // Nullable - null si vinculado directamente al rancho
    String descripcion,
    UUID tenantId,
    UUID userId
) {
}
