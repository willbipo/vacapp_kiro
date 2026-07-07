package mx.vacapp.geography.internal.infrastructure.controllers.mobile.mappers;

import lombok.RequiredArgsConstructor;
import mx.vacapp.geography.internal.application.usecases.commands.CreatePotreroCommand;
import mx.vacapp.geography.internal.application.usecases.commands.PotreroResult;
import mx.vacapp.geography.internal.application.usecases.commands.UpdatePotreroCommand;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.CreatePotreroRequest;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.PotreroResponse;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.UpdatePotreroRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Mapper para transformar entre DTOs REST y Commands/Results de Potrero.
 */
@Component
@RequiredArgsConstructor
public class PotreroDtoMapper {
    
    // TODO: Inyectar TenantContext cuando esté disponible
    // private final TenantContext tenantContext;
    
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    
    /**
     * Convierte CreatePotreroRequest a CreatePotreroCommand.
     */
    public CreatePotreroCommand toCommand(CreatePotreroRequest request) {
        // TODO: Extraer del TenantContext
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001"); // Mock temporal
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");   // Mock temporal
        
        return new CreatePotreroCommand(
            request.nombre(),
            request.superficie(),
            request.ranchoId(),
            request.seccionId(), // Nullable
            request.descripcion(),
            tenantId,
            userId
        );
    }
    
    /**
     * Convierte UpdatePotreroRequest a UpdatePotreroCommand.
     */
    public UpdatePotreroCommand toCommand(UUID potreroId, UpdatePotreroRequest request) {
        // TODO: Extraer del TenantContext
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002"); // Mock temporal
        
        return new UpdatePotreroCommand(
            potreroId,
            request.nombre(),
            request.superficie(),
            request.descripcion(),
            userId
        );
    }
    
    /**
     * Convierte PotreroResult a PotreroResponse.
     */
    public PotreroResponse toResponse(PotreroResult result) {
        return new PotreroResponse(
            result.potreroId(),
            result.nombre(),
            formatDecimal(result.superficie()),
            result.ranchoId(),
            result.seccionId(), // Nullable
            result.cattleCount(),
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
