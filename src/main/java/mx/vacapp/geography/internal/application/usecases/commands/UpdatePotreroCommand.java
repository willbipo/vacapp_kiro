package mx.vacapp.geography.internal.application.usecases.commands;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando para actualizar un potrero existente.
 */
public record UpdatePotreroCommand(
    UUID potreroId,
    String nombre,
    BigDecimal superficie,
    String descripcion,
    UUID userId
) {
}
