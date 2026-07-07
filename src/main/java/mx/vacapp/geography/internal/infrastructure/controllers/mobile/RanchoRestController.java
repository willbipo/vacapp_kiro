package mx.vacapp.geography.internal.infrastructure.controllers.mobile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.RanchoResult;
import mx.vacapp.geography.internal.application.usecases.commands.RanchoStatsResult;
import mx.vacapp.geography.internal.application.usecases.rancho.ArchiveRanchoUseCase;
import mx.vacapp.geography.internal.application.usecases.rancho.CreateRanchoUseCase;
import mx.vacapp.geography.internal.application.usecases.rancho.GetRanchoStatsUseCase;
import mx.vacapp.geography.internal.application.usecases.rancho.GetRanchoUseCase;
import mx.vacapp.geography.internal.application.usecases.rancho.ListRanchosUseCase;
import mx.vacapp.geography.internal.application.usecases.rancho.UpdateRanchoUseCase;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.CreateRanchoRequest;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.PaginationMeta;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.RanchoResponse;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.RanchoStatsResponse;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.UpdateRanchoRequest;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.mappers.RanchoDtoMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controlador REST para endpoints de Ranchos.
 * Rutas base: /api/v1/geography/ranchos
 * 
 * Implementa todos los endpoints definidos en openapi-geography.yaml.
 */
@RestController
@RequestMapping("/api/v1/geography/ranchos")
@RequiredArgsConstructor
@Slf4j
public class RanchoRestController {
    
    private final CreateRanchoUseCase createRanchoUseCase;
    private final UpdateRanchoUseCase updateRanchoUseCase;
    private final GetRanchoUseCase getRanchoUseCase;
    private final ListRanchosUseCase listRanchosUseCase;
    private final ArchiveRanchoUseCase archiveRanchoUseCase;
    private final GetRanchoStatsUseCase getRanchoStatsUseCase;
    private final RanchoDtoMapper mapper;
    
    /**
     * POST /api/v1/geography/ranchos
     * Crear nuevo rancho.
     */
    @PostMapping
    public ResponseEntity<RanchoResponse> createRancho(@Valid @RequestBody CreateRanchoRequest request) {
        log.debug("POST /api/v1/geography/ranchos - Creando rancho: {}", request.nombre());
        
        RanchoResult result = createRanchoUseCase.execute(mapper.toCommand(request));
        RanchoResponse response = mapper.toResponse(result);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * GET /api/v1/geography/ranchos
     * Listar ranchos del tenant con paginación.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listRanchos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        log.debug("GET /api/v1/geography/ranchos - page: {}, size: {}", page, size);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001"); // Mock temporal
        
        List<RanchoResult> results = listRanchosUseCase.execute(tenantId, page, size);
        long total = listRanchosUseCase.countByTenant(tenantId);
        
        List<RanchoResponse> data = results.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        
        PaginationMeta pagination = new PaginationMeta(page, size, total);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        response.put("pagination", pagination);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/geography/ranchos/{id}
     * Obtener rancho por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RanchoResponse> getRanchoById(@PathVariable UUID id) {
        log.debug("GET /api/v1/geography/ranchos/{} - Obteniendo rancho", id);
        
        RanchoResult result = getRanchoUseCase.execute(id);
        RanchoResponse response = mapper.toResponse(result);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * PUT /api/v1/geography/ranchos/{id}
     * Actualizar rancho.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RanchoResponse> updateRancho(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRanchoRequest request) {
        
        log.debug("PUT /api/v1/geography/ranchos/{} - Actualizando rancho: {}", id, request.nombre());
        
        RanchoResult result = updateRanchoUseCase.execute(mapper.toCommand(id, request));
        RanchoResponse response = mapper.toResponse(result);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * DELETE /api/v1/geography/ranchos/{id}
     * Archivar rancho (soft delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveRancho(@PathVariable UUID id) {
        log.debug("DELETE /api/v1/geography/ranchos/{} - Archivando rancho", id);
        
        // TODO: Extraer userId del contexto de seguridad
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002"); // Mock temporal
        
        archiveRanchoUseCase.execute(id, userId);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * GET /api/v1/geography/ranchos/{id}/estadisticas
     * Obtener estadísticas de un rancho.
     */
    @GetMapping("/{id}/estadisticas")
    public ResponseEntity<RanchoStatsResponse> getRanchoStats(@PathVariable UUID id) {
        log.debug("GET /api/v1/geography/ranchos/{}/estadisticas - Obteniendo estadísticas", id);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001"); // Mock temporal
        
        RanchoStatsResult stats = getRanchoStatsUseCase.execute(id, tenantId);
        RanchoStatsResponse response = toStatsResponse(stats);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Convierte RanchoStatsResult a RanchoStatsResponse.
     */
    private RanchoStatsResponse toStatsResponse(RanchoStatsResult stats) {
        List<RanchoStatsResponse.SeccionDistributionDto> distribucion = stats.distribucionPorSeccion().stream()
                .map(d -> new RanchoStatsResponse.SeccionDistributionDto(
                        d.nombreSeccion(),
                        formatDecimal(d.superficie()),
                        formatDecimal(d.porcentaje()),
                        d.totalPotreros()
                ))
                .collect(Collectors.toList());
        
        return new RanchoStatsResponse(
                stats.totalSecciones(),
                stats.totalPotreros(),
                formatDecimal(stats.superficieTotal()),
                formatDecimal(stats.superficieUsada()),
                formatDecimal(stats.superficieDisponible()),
                formatDecimal(stats.porcentajeUso()),
                distribucion
        );
    }
    
    /**
     * Formatea BigDecimal con 2 decimales.
     */
    private BigDecimal formatDecimal(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
