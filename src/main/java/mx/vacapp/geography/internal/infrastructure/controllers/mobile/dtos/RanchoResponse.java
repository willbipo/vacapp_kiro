package mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO Response para rancho.
 * Record inmutable sin validaciones (salida de API).
 */
public record RanchoResponse(
    UUID ranchoId,
    String nombre,
    BigDecimal superficieTotal,
    BigDecimal superficieDisponible,
    BigDecimal superficieUsada,
    BigDecimal porcentajeUso,
    String descripcion,
    String status,
    UUID tenantId,
    String createdAt,  // ISO 8601 UTC
    String updatedAt   // ISO 8601 UTC
) {
}
