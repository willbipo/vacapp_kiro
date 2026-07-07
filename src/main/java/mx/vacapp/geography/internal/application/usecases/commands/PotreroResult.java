package mx.vacapp.geography.internal.application.usecases.commands;

import mx.vacapp.geography.internal.domain.model.GeographicStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Resultado que contiene información de un potrero.
 */
public record PotreroResult(
    UUID potreroId,
    String nombre,
    BigDecimal superficie,
    UUID ranchoId,
    UUID seccionId, // Nullable
    Integer cattleCount,
    String descripcion,
    GeographicStatus status,
    UUID tenantId,
    Instant createdAt,
    Instant updatedAt
) {
}
