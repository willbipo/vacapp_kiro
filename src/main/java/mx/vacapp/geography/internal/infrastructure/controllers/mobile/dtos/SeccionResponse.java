package mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO Response para sección.
 * Record inmutable sin validaciones (salida de API).
 */
public record SeccionResponse(
    UUID seccionId,
    String nombre,
    BigDecimal superficie,
    BigDecimal superficieDisponible,
    BigDecimal superficieUsada,
    UUID ranchoId,
    String descripcion,
    String status,
    UUID tenantId,
    String createdAt,  // ISO 8601 UTC
    String updatedAt   // ISO 8601 UTC
) {
}
