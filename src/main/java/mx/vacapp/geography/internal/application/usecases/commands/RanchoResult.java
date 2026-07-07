package mx.vacapp.geography.internal.application.usecases.commands;

import mx.vacapp.geography.internal.domain.model.GeographicStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Resultado que contiene información de un rancho.
 * Utilizado como respuesta de casos de uso de Rancho.
 */
public record RanchoResult(
    UUID ranchoId,
    String nombre,
    BigDecimal superficieTotal,
    BigDecimal superficieDisponible,
    BigDecimal superficieUsada,
    String descripcion,
    GeographicStatus status,
    UUID tenantId,
    Instant createdAt,
    Instant updatedAt
) {
}
