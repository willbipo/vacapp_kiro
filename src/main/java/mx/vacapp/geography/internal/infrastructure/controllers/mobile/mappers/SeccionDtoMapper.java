package mx.vacapp.geography.internal.infrastructure.controllers.mobile.mappers;

import lombok.RequiredArgsConstructor;
import mx.vacapp.geography.internal.application.usecases.commands.CreateSeccionCommand;
import mx.vacapp.geography.internal.application.usecases.commands.SeccionResult;
import mx.vacapp.geography.internal.application.usecases.commands.UpdateSeccionCommand;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.CreateSeccionRequest;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.SeccionResponse;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.UpdateSeccionRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Mapper para transformar entre DTOs REST y Commands/Results de Seccion.
 */
@Component
@RequiredArgsConstructor
public class SeccionDtoMapper {
    
    // TODO: Inyectar TenantContext cuando esté disponible
    // private final TenantContext tenantContext;
    
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    
    /**
     * Convierte CreateSeccionRequest a CreateSeccionCommand.
     */
    public CreateSeccionCommand toCommand(CreateSeccionRequest request) {
        // TODO: Extraer del TenantContext
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001"); // Mock temporal
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");   // Mock temporal
        
        return new CreateSeccionCommand(
            request.nombre(),
            request.superficie(),
            request.ranchoId(),
            request.descripcion(),
            tenantId,
            userId
        );
    }
    
    /**
     * Convierte UpdateSeccionRequest a UpdateSeccionCommand.
     */
    public UpdateSeccionCommand toCommand(UUID seccionId, UpdateSeccionRequest request) {
        // TODO: Extraer del TenantContext
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002"); // Mock temporal
        
        return new UpdateSeccionCommand(
            seccionId,
            request.nombre(),
            request.superficie(),
            request.descripcion(),
            userId
        );
    }
    
    /**
     * Convierte SeccionResult a SeccionResponse.
     */
    public SeccionResponse toResponse(SeccionResult result) {
        return new SeccionResponse(
            result.seccionId(),
            result.nombre(),
            formatDecimal(result.superficie()),
            formatDecimal(result.superficieDisponible()),
            formatDecimal(result.superficieUsada()),
            result.ranchoId(),
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
}
