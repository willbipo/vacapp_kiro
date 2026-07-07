package mx.vacapp.geography.internal.application.usecases.seccion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.SeccionResult;
import mx.vacapp.geography.internal.domain.model.Seccion;
import mx.vacapp.geography.internal.domain.model.SurfaceCalculator;
import mx.vacapp.geography.internal.domain.repository.SeccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso para listar secciones del tenant actual.
 * 
 * Puede filtrar por rancho específico o listar todas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListSeccionesUseCase {
    
    private final SeccionRepository seccionRepository;
    
    /**
     * Lista secciones de un rancho específico.
     * 
     * @param ranchoId UUID del rancho
     * @param tenantId ID del tenant
     * @return lista de resultados con información de secciones
     */
    @Transactional(readOnly = true)
    public List<SeccionResult> executeByRancho(UUID ranchoId, UUID tenantId) {
        log.debug("Listando secciones del rancho: {}", ranchoId);
        
        List<Seccion> secciones = seccionRepository.findByRanchoIdAndTenantId(ranchoId, tenantId);
        
        return secciones.stream()
            .map(this::mapToResultWithMetrics)
            .collect(Collectors.toList());
    }
    
    /**
     * Lista todas las secciones del tenant con paginación.
     * 
     * @param tenantId ID del tenant
     * @param page número de página (0-based)
     * @param size tamaño de página (máximo 100)
     * @return lista de resultados con información de secciones
     */
    @Transactional(readOnly = true)
    public List<SeccionResult> executeAll(UUID tenantId, int page, int size) {
        log.debug("Listando secciones para tenant: {} (page: {}, size: {})", tenantId, page, size);
        
        // Limitar tamaño máximo de página
        int limitedSize = Math.min(size, 100);
        
        List<Seccion> secciones = seccionRepository.findByTenantId(tenantId, page, limitedSize);
        
        return secciones.stream()
            .map(this::mapToResultWithMetrics)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene el total de secciones del tenant.
     * 
     * @param tenantId ID del tenant
     * @return cantidad total de secciones
     */
    @Transactional(readOnly = true)
    public long countByTenant(UUID tenantId) {
        return seccionRepository.countByTenantId(tenantId);
    }
    
    /**
     * Mapea una sección a resultado con métricas calculadas.
     */
    private SeccionResult mapToResultWithMetrics(Seccion seccion) {
        // Calcular superficie usada por potreros
        BigDecimal superficieUsada = seccionRepository.sumSuperficiePotrerosSeccionId(seccion.getSeccionId());
        
        // Calcular superficie disponible
        BigDecimal superficieDisponible = SurfaceCalculator.calculateAvailable(
            seccion.getSuperficie(), 
            superficieUsada
        );
        
        return new SeccionResult(
            seccion.getSeccionId(),
            seccion.getNombre(),
            seccion.getSuperficie(),
            superficieDisponible,
            superficieUsada,
            seccion.getRanchoId(),
            seccion.getDescripcion(),
            seccion.getStatus(),
            seccion.getTenantId(),
            seccion.getCreatedAt(),
            seccion.getUpdatedAt()
        );
    }
}
