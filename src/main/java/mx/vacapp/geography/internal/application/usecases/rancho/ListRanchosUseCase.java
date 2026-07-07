package mx.vacapp.geography.internal.application.usecases.rancho;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.RanchoResult;
import mx.vacapp.geography.internal.domain.model.Rancho;
import mx.vacapp.geography.internal.domain.model.SurfaceCalculator;
import mx.vacapp.geography.internal.domain.repository.RanchoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso para listar ranchos del tenant actual con paginación.
 * 
 * Incluye métricas calculadas para cada rancho.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListRanchosUseCase {
    
    private final RanchoRepository ranchoRepository;
    
    /**
     * Ejecuta la lista de ranchos con paginación.
     * 
     * @param tenantId ID del tenant
     * @param page número de página (0-based)
     * @param size tamaño de página (máximo 100)
     * @return lista de resultados con información de ranchos
     */
    @Transactional(readOnly = true)
    public List<RanchoResult> execute(UUID tenantId, int page, int size) {
        log.debug("Listando ranchos para tenant: {} (page: {}, size: {})", tenantId, page, size);
        
        // Limitar tamaño máximo de página
        int limitedSize = Math.min(size, 100);
        
        // Obtener lista de ranchos
        List<Rancho> ranchos = ranchoRepository.findByTenantId(tenantId, page, limitedSize);
        
        // Mapear a resultados con métricas calculadas
        return ranchos.stream()
            .map(this::mapToResultWithMetrics)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene el total de ranchos del tenant.
     * 
     * @param tenantId ID del tenant
     * @return cantidad total de ranchos
     */
    @Transactional(readOnly = true)
    public long countByTenant(UUID tenantId) {
        return ranchoRepository.countByTenantId(tenantId);
    }
    
    /**
     * Mapea un rancho a resultado con métricas calculadas.
     */
    private RanchoResult mapToResultWithMetrics(Rancho rancho) {
        // Calcular superficie usada
        BigDecimal superficieUsadaSecciones = ranchoRepository.sumSuperficieSeccionesByRanchoId(rancho.getRanchoId());
        BigDecimal superficieUsadaPotreros = ranchoRepository.sumSuperficioPotrerosDirectosByRanchoId(rancho.getRanchoId());
        BigDecimal superficieUsadaTotal = superficieUsadaSecciones.add(superficieUsadaPotreros);
        
        // Calcular superficie disponible
        BigDecimal superficieDisponible = SurfaceCalculator.calculateAvailable(
            rancho.getSuperficieTotal(), 
            superficieUsadaTotal
        );
        
        return new RanchoResult(
            rancho.getRanchoId(),
            rancho.getNombre(),
            rancho.getSuperficieTotal(),
            superficieDisponible,
            superficieUsadaTotal,
            rancho.getDescripcion(),
            rancho.getStatus(),
            rancho.getTenantId(),
            rancho.getCreatedAt(),
            rancho.getUpdatedAt()
        );
    }
}
