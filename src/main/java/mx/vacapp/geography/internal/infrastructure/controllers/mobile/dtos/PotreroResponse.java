package mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO Response para potrero.
 * Record inmutable sin validaciones (salida de API).
 */
public record PotreroResponse(
    UUID potreroId,
    String nombre,
    BigDecimal superficie,
    UUID ranchoId,
    UUID seccionId,  // Nullable
    Integer cattleCount,
    String descripcion,
    String status,
    UUID tenantId,
    String createdAt,  // ISO 8601 UTC
    String updatedAt   // ISO 8601 UTC
) {
}
