package mx.vacapp.geography.internal.infrastructure.controllers.mobile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.PotreroResult;
import mx.vacapp.geography.internal.application.usecases.potrero.ArchivePotreroUseCase;
import mx.vacapp.geography.internal.application.usecases.potrero.CreatePotreroUseCase;
import mx.vacapp.geography.internal.application.usecases.potrero.GetPotreroUseCase;
import mx.vacapp.geography.internal.application.usecases.potrero.ListPotrerosUseCase;
import mx.vacapp.geography.internal.application.usecases.potrero.UpdatePotreroUseCase;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.CreatePotreroRequest;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.PaginationMeta;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.PotreroResponse;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos.UpdatePotreroRequest;
import mx.vacapp.geography.internal.infrastructure.controllers.mobile.mappers.PotreroDtoMapper;
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
 * Controlador REST para endpoints de Potreros.
 * Rutas base: /api/v1/geography/potreros
 * 
 * Implementa todos los endpoints definidos en openapi-geography.yaml.
 */
@RestController
@RequestMapping("/api/v1/geography/potreros")
@RequiredArgsConstructor
@Slf4j
public class PotreroRestController {
    
    private final CreatePotreroUseCase createPotreroUseCase;
    private final UpdatePotreroUseCase updatePotreroUseCase;
    private final GetPotreroUseCase getPotreroUseCase;
    private final ListPotrerosUseCase listPotrerosUseCase;
    private final ArchivePotreroUseCase archivePotreroUseCase;
    private final PotreroDtoMapper mapper;
    
    /**
     * POST /api/v1/geography/potreros
     * Crear nuevo potrero.
     */
    @PostMapping
    public ResponseEntity<PotreroResponse> createPotrero(@Valid @RequestBody CreatePotreroRequest request) {
        log.debug("POST /api/v1/geography/potreros - Creando potrero: {}", request.nombre());
        
        PotreroResult result = createPotreroUseCase.execute(mapper.toCommand(request));
        PotreroResponse response = mapper.toResponse(result);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * GET /api/v1/geography/potreros
     * Listar potreros del tenant con paginación.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listPotreros(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        log.debug("GET /api/v1/geography/potreros - page: {}, size: {}", page, size);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001"); // Mock temporal
        
        List<PotreroResult> results = listPotrerosUseCase.executeAll(tenantId, page, size);
        long total = listPotrerosUseCase.countByTenant(tenantId);
        
        List<PotreroResponse> data = results.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        
        PaginationMeta pagination = new PaginationMeta(page, size, total);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        response.put("pagination", pagination);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/geography/potreros/{id}
     * Obtener potrero por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PotreroResponse> getPotreroById(@PathVariable UUID id) {
        log.debug("GET /api/v1/geography/potreros/{} - Obteniendo potrero", id);
        
        PotreroResult result = getPotreroUseCase.execute(id);
        PotreroResponse response = mapper.toResponse(result);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * PUT /api/v1/geography/potreros/{id}
     * Actualizar potrero.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PotreroResponse> updatePotrero(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePotreroRequest request) {
        
        log.debug("PUT /api/v1/geography/potreros/{} - Actualizando potrero: {}", id, request.nombre());
        
        PotreroResult result = updatePotreroUseCase.execute(mapper.toCommand(id, request));
        PotreroResponse response = mapper.toResponse(result);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * DELETE /api/v1/geography/potreros/{id}
     * Archivar potrero (soft delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archivePotrero(@PathVariable UUID id) {
        log.debug("DELETE /api/v1/geography/potreros/{} - Archivando potrero", id);
        
        // TODO: Extraer userId del contexto de seguridad
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002"); // Mock temporal
        
        archivePotreroUseCase.execute(id, userId);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * GET /api/v1/geography/ranchos/{ranchoId}/potreros
     * Listar potreros de un rancho específico.
     */
    @GetMapping
    @RequestMapping("/api/v1/geography/ranchos/{ranchoId}/potreros")
    public ResponseEntity<List<PotreroResponse>> listPotrerosByRancho(@PathVariable UUID ranchoId) {
        log.debug("GET /api/v1/geography/ranchos/{}/potreros - Listando potreros del rancho", ranchoId);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001"); // Mock temporal
        
        List<PotreroResult> results = listPotrerosUseCase.executeByRancho(ranchoId, tenantId);
        
        List<PotreroResponse> data = results.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(data);
    }
    
    /**
     * GET /api/v1/geography/secciones/{seccionId}/potreros
     * Listar potreros de una sección específica.
     */
    @GetMapping
    @RequestMapping("/api/v1/geography/secciones/{seccionId}/potreros")
    public ResponseEntity<List<PotreroResponse>> listPotrerosBySeccion(@PathVariable UUID seccionId) {
        log.debug("GET /api/v1/geography/secciones/{}/potreros - Listando potreros de la sección", seccionId);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001"); // Mock temporal
        
        List<PotreroResult> results = listPotrerosUseCase.executeBySeccion(seccionId, tenantId);
        
        List<PotreroResponse> data = results.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(data);
    }
}
