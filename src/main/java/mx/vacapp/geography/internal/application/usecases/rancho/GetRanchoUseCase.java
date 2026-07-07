package mx.vacapp.geography.internal.application.usecases.rancho;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.RanchoResult;
import mx.vacapp.geography.internal.domain.model.Rancho;
import mx.vacapp.geography.internal.domain.model.SurfaceCalculator;
import mx.vacapp.geography.internal.domain.model.exceptions.EntityNotFoundException;
import mx.vacapp.geography.internal.domain.repository.RanchoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Caso de uso para obtener un rancho por ID.
 * 
 * Calcula superficie disponible y usada en tiempo real.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetRanchoUseCase {
    
    private final RanchoRepository ranchoRepository;
    
    /**
     * Ejecuta la búsqueda de un rancho por ID.
     * 
     * @param ranchoId UUID del rancho
     * @return resultado con información del rancho y métricas calculadas
     * @throws EntityNotFoundException si el rancho no existe
     */
    @Transactional(readOnly = true)
    public RanchoResult execute(UUID ranchoId) {
        log.debug("Obteniendo rancho: {}", ranchoId);
        
        // Buscar rancho
        Rancho rancho = ranchoRepository.findById(ranchoId)
            .orElseThrow(() -> new EntityNotFoundException("Rancho no encontrado con ID: " + ranchoId));
        
        // Calcular superficie usada
        BigDecimal superficieUsadaSecciones = ranchoRepository.sumSuperficieSeccionesByRanchoId(rancho.getRanchoId());
        BigDecimal superficieUsadaPotreros = ranchoRepository.sumSuperficiePotrerosDirectosByRanchoId(rancho.getRanchoId());
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
