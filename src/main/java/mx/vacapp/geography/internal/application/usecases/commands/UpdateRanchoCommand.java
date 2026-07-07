package mx.vacapp.geography.internal.application.usecases.commands;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando para actualizar un rancho existente.
 */
public record UpdateRanchoCommand(
    UUID ranchoId,
    String nombre,
    BigDecimal superficieTotal,
    String descripcion,
    UUID userId
) {
}
