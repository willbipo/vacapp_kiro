package mx.vacapp.geography.internal.application.usecases.commands;

import mx.vacapp.geography.internal.domain.model.GeographicStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Resultado que contiene información de una sección.
 */
public record SeccionResult(
    UUID seccionId,
    String nombre,
    BigDecimal superficie,
    BigDecimal superficieDisponible,
    BigDecimal superficieUsada,
    UUID ranchoId,
    String descripcion,
    GeographicStatus status,
    UUID tenantId,
    Instant createdAt,
    Instant updatedAt
) {
}
