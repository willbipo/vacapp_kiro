package mx.vacapp.geography.internal.infrastructure.controllers.mobile.mappers;

import lombok.RequiredArgsConstructor;
import mx.vacapp.geography.internal.application.usecases.commands.CreateRanchoCommand;
import mx.vacapp.geography.internal.application.usecases.commands.RanchoResult;
import mx.vacapp.geography.internal.application.usecases.commands.UpdateRanchoCommand;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.CreateRanchoRequest;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.RanchoResponse;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.UpdateRanchoRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Mapper para transformar entre DTOs REST y Commands/Results de Rancho.
 * Incluye formateo de timestamps ISO 8601 y BigDecimal con 2 decimales.
 */
@Component
@RequiredArgsConstructor
public class RanchoDtoMapper {
    
    // TODO: Inyectar TenantContext cuando esté disponible
    // private final TenantContext tenantContext;
    
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    
    /**
     * Convierte CreateRanchoRequest a CreateRanchoCommand.
     * Extrae tenantId y userId del contexto de seguridad.
     */
    public CreateRanchoCommand toCommand(CreateRanchoRequest request) {
        // TODO: Extraer del TenantContext
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001"); // Mock temporal
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");   // Mock temporal
        
        return new CreateRanchoCommand(
            request.nombre(),
            request.superficieTotal(),
            request.descripcion(),
            tenantId,
            userId
        );
    }
    
    /**
     * Convierte UpdateRanchoRequest a UpdateRanchoCommand.
     */
    public UpdateRanchoCommand toCommand(UUID ranchoId, UpdateRanchoRequest request) {
        // TODO: Extraer del TenantContext
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002"); // Mock temporal
        
        return new UpdateRanchoCommand(
            ranchoId,
            request.nombre(),
            request.superficieTotal(),
            request.descripcion(),
            userId
        );
    }
    
    /**
     * Convierte RanchoResult a RanchoResponse.
     * Formatea timestamps como ISO 8601 UTC y BigDecimal con 2 decimales.
     */
    public RanchoResponse toResponse(RanchoResult result) {
        return new RanchoResponse(
            result.ranchoId(),
            result.nombre(),
            formatDecimal(result.superficieTotal()),
            formatDecimal(result.superficieDisponible()),
            formatDecimal(result.superficieUsada()),
            calculatePercentage(result.superficieUsada(), result.superficieTotal()),
            result.descripcion(),
            result.status().name(),
            result.tenantId(),
            formatInstant(result.createdAt()),
            formatInstant(result.updatedAt())
        );
    }
    
    /**
     * Formatea Instant como ISO 8601 UTC.
     */
    private String formatInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atOffset(ZoneOffset.UTC).format(ISO_FORMATTER);
    }
    
    /**
     * Formatea BigDecimal con 2 decimales.
     */
    private BigDecimal formatDecimal(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calcula porcentaje de uso (0.00 - 100.00).
     */
    private BigDecimal calculatePercentage(BigDecimal used, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (used == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return used.divide(total, 4, RoundingMode.HALF_UP)
                   .multiply(BigDecimal.valueOf(100))
                   .setScale(2, RoundingMode.HALF_UP);
    }
}
