package mx.vacapp.geography.internal.application.usecases.seccion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.SeccionResult;
import mx.vacapp.geography.internal.domain.model.Seccion;
import mx.vacapp.geography.internal.domain.model.SurfaceCalculator;
import mx.vacapp.geography.internal.domain.model.exceptions.EntityNotFoundException;
import mx.vacapp.geography.internal.domain.repository.SeccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Caso de uso para obtener una sección por ID.
 * 
 * Calcula superficie disponible y usada en tiempo real.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetSeccionUseCase {
    
    private final SeccionRepository seccionRepository;
    
    /**
     * Ejecuta la búsqueda de una sección por ID.
     * 
     * @param seccionId UUID de la sección
     * @return resultado con información de la sección y métricas calculadas
     * @throws EntityNotFoundException si la sección no existe
     */
    @Transactional(readOnly = true)
    public SeccionResult execute(UUID seccionId) {
        log.debug("Obteniendo sección: {}", seccionId);
        
        // Buscar sección
        Seccion seccion = seccionRepository.findById(seccionId)
            .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada con ID: " + seccionId));
        
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
