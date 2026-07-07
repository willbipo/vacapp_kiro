package mx.vacapp.geography.internal.infrastructure.controllers.mobile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.SeccionResult;
import mx.vacapp.geography.internal.application.usecases.seccion.ArchiveSeccionUseCase;
import mx.vacapp.geography.internal.application.usecases.seccion.CreateSeccionUseCase;
import mx.vacapp.geography.internal.application.usecases.seccion.GetSeccionUseCase;
import mx.vacapp.geography.internal.application.usecases.seccion.ListSeccionesUseCase;
import mx.vacapp.geography.internal.application.usecases.seccion.UpdateSeccionUseCase;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.CreateSeccionRequest;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.PaginationMeta;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.SeccionResponse;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.UpdateSeccionRequest;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.mappers.SeccionDtoMapper;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controlador REST para endpoints de Secciones.
 * Rutas base: /api/v1/geography/secciones
 * 
 * Implementa todos los endpoints definidos en openapi-geography.yaml.
 */
@RestController
@RequestMapping("/api/v1/geography/secciones")
@RequiredArgsConstructor
@Slf4j
public class SeccionRestController {
    
    private final CreateSeccionUseCase createSeccionUseCase;
    private final UpdateSeccionUseCase updateSeccionUseCase;
    private final GetSeccionUseCase getSeccionUseCase;
    private final ListSeccionesUseCase listSeccionesUseCase;
    private final ArchiveSeccionUseCase archiveSeccionUseCase;
    private final SeccionDtoMapper mapper;
    
    /**
     * POST /api/v1/geography/secciones
     * Crear nueva sección.
     */
    @PostMapping
    public ResponseEntity<SeccionResponse> createSeccion(@Valid @RequestBody CreateSeccionRequest request) {
        log.debug("POST /api/v1/geography/secciones - Creando sección: {}", request.nombre());
        
        SeccionResult result = createSeccionUseCase.execute(mapper.toCommand(request));
        SeccionResponse response = mapper.toResponse(result);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * GET /api/v1/geography/secciones
     * Listar secciones del tenant con paginación.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listSecciones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        log.debug("GET /api/v1/geography/secciones - page: {}, size: {}", page, size);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001"); // Mock temporal
        
        List<SeccionResult> results = listSeccionesUseCase.executeAll(tenantId, page, size);
        long total = listSeccionesUseCase.countByTenant(tenantId);
        
        List<SeccionResponse> data = results.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        
        PaginationMeta pagination = new PaginationMeta(page, size, total);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        response.put("pagination", pagination);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/geography/secciones/{id}
     * Obtener sección por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SeccionResponse> getSeccionById(@PathVariable UUID id) {
        log.debug("GET /api/v1/geography/secciones/{} - Obteniendo sección", id);
        
        SeccionResult result = getSeccionUseCase.execute(id);
        SeccionResponse response = mapper.toResponse(result);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * PUT /api/v1/geography/secciones/{id}
     * Actualizar sección.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SeccionResponse> updateSeccion(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSeccionRequest request) {
        
        log.debug("PUT /api/v1/geography/secciones/{} - Actualizando sección: {}", id, request.nombre());
        
        SeccionResult result = updateSeccionUseCase.execute(mapper.toCommand(id, request));
        SeccionResponse response = mapper.toResponse(result);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * DELETE /api/v1/geography/secciones/{id}
     * Archivar sección (soft delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveSeccion(@PathVariable UUID id) {
        log.debug("DELETE /api/v1/geography/secciones/{} - Archivando sección", id);
        
        // TODO: Extraer userId del contexto de seguridad
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002"); // Mock temporal
        
        archiveSeccionUseCase.execute(id, userId);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * GET /api/v1/geography/ranchos/{ranchoId}/secciones
     * Listar secciones de un rancho específico.
     */
    @GetMapping
    @RequestMapping("/api/v1/geography/ranchos/{ranchoId}/secciones")
    public ResponseEntity<List<SeccionResponse>> listSeccionesByRancho(@PathVariable UUID ranchoId) {
        log.debug("GET /api/v1/geography/ranchos/{}/secciones - Listando secciones del rancho", ranchoId);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001"); // Mock temporal
        
        List<SeccionResult> results = listSeccionesUseCase.executeByRancho(ranchoId, tenantId);
        
        List<SeccionResponse> data = results.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(data);
    }
}
