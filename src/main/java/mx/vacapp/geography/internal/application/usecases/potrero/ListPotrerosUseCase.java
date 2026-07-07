package mx.vacapp.geography.internal.application.usecases.potrero;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.PotreroResult;
import mx.vacapp.geography.internal.domain.model.Potrero;
import mx.vacapp.geography.internal.domain.repository.PotreroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso para listar potreros del tenant actual.
 * 
 * Soporta filtrado por:
 * - Todos los potreros del tenant (paginado)
 * - Potreros de un rancho específico
 * - Potreros de una sección específica
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListPotrerosUseCase {
    
    private final PotreroRepository potreroRepository;
    
    /**
     * Lista potreros de un rancho específico (directos + en secciones).
     * 
     * @param ranchoId UUID del rancho
     * @param tenantId ID del tenant
     * @return lista de resultados con información de potreros
     */
    @Transactional(readOnly = true)
    public List<PotreroResult> executeByRancho(UUID ranchoId, UUID tenantId) {
        log.debug("Listando potreros del rancho: {}", ranchoId);
        
        List<Potrero> potreros = potreroRepository.findByRanchoIdAndTenantId(ranchoId, tenantId);
        
        return potreros.stream()
            .map(this::mapToResult)
            .collect(Collectors.toList());
    }
    
    /**
     * Lista potreros de una sección específica.
     * 
     * @param seccionId UUID de la sección
     * @param tenantId ID del tenant
     * @return lista de resultados con información de potreros
     */
    @Transactional(readOnly = true)
    public List<PotreroResult> executeBySeccion(UUID seccionId, UUID tenantId) {
        log.debug("Listando potreros de la sección: {}", seccionId);
        
        List<Potrero> potreros = potreroRepository.findBySeccionIdAndTenantId(seccionId, tenantId);
        
        return potreros.stream()
            .map(this::mapToResult)
            .collect(Collectors.toList());
    }
    
    /**
     * Lista todos los potreros del tenant con paginación.
     * 
     * @param tenantId ID del tenant
     * @param page número de página (0-based)
     * @param size tamaño de página (máximo 100)
     * @return lista de resultados con información de potreros
     */
    @Transactional(readOnly = true)
    public List<PotreroResult> executeAll(UUID tenantId, int page, int size) {
        log.debug("Listando potreros para tenant: {} (page: {}, size: {})", tenantId, page, size);
        
        // Limitar tamaño máximo de página
        int limitedSize = Math.min(size, 100);
        
        List<Potrero> potreros = potreroRepository.findByTenantId(tenantId, page, limitedSize);
        
        return potreros.stream()
            .map(this::mapToResult)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene el total de potreros del tenant.
     * 
     * @param tenantId ID del tenant
     * @return cantidad total de potreros
     */
    @Transactional(readOnly = true)
    public long countByTenant(UUID tenantId) {
        return potreroRepository.countByTenantId(tenantId);
    }
    
    /**
     * Mapea un potrero a resultado.
     */
    private PotreroResult mapToResult(Potrero potrero) {
        return new PotreroResult(
            potrero.getPotreroId(),
            potrero.getNombre(),
            potrero.getSuperficie(),
            potrero.getRanchoId(),
            potrero.getSeccionId(),
            potrero.getCattleCount(),
            potrero.getDescripcion(),
            potrero.getStatus(),
            potrero.getTenantId(),
            potrero.getCreatedAt(),
            potrero.getUpdatedAt()
        );
    }
}
