package mx.vacapp.geography.internal.application.usecases.rancho;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.RanchoStatsResult;
import mx.vacapp.geography.internal.application.usecases.commands.RanchoStatsResult.SeccionDistribution;
import mx.vacapp.geography.internal.domain.model.Potrero;
import mx.vacapp.geography.internal.domain.model.Rancho;
import mx.vacapp.geography.internal.domain.model.Seccion;
import mx.vacapp.geography.internal.domain.model.SurfaceCalculator;
import mx.vacapp.geography.internal.domain.model.exceptions.EntityNotFoundException;
import mx.vacapp.geography.internal.domain.repository.PotreroRepository;
import mx.vacapp.geography.internal.domain.repository.RanchoRepository;
import mx.vacapp.geography.internal.domain.repository.SeccionRepository;
import mx.vacapp.geography.internal.infrastructure.config.CacheNames;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso para obtener estadísticas completas de un rancho.
 * 
 * Calcula:
 * - Total secciones y potreros
 * - Superficie usada, disponible y porcentaje de uso
 * - Distribución por sección
 * 
 * Utiliza SurfaceCalculator para cálculos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetRanchoStatsUseCase {
    
    private final RanchoRepository ranchoRepository;
    private final SeccionRepository seccionRepository;
    private final PotreroRepository potreroRepository;
    
    /**
     * Ejecuta el cálculo de estadísticas de un rancho.
     * 
     * Cache key pattern: stats:rancho:{ranchoId}:tenant:{tenantId}
     * TTL: 5 minutos
     * 
     * @param ranchoId UUID del rancho
     * @param tenantId ID del tenant
     * @return resultado con estadísticas completas
     * @throws EntityNotFoundException si el rancho no existe
     */
    @Cacheable(value = CacheNames.RANCHO_STATS, key = "'stats:rancho:' + #ranchoId + ':tenant:' + #tenantId")
    @Transactional(readOnly = true)
    public RanchoStatsResult execute(UUID ranchoId, UUID tenantId) {
        log.debug("Calculando estadísticas para rancho: {}", ranchoId);
        
        // Buscar rancho
        Rancho rancho = ranchoRepository.findById(ranchoId)
            .orElseThrow(() -> new EntityNotFoundException("Rancho no encontrado con ID: " + ranchoId));
        
        // Obtener secciones activas
        List<Seccion> secciones = seccionRepository.findActiveByRanchoIdAndTenantId(ranchoId, tenantId);
        int totalSecciones = secciones.size();
        
        // Obtener potreros activos (directos + en secciones)
        List<Potrero> potreros = potreroRepository.findActiveByRanchoIdAndTenantId(ranchoId, tenantId);
        int totalPotreros = potreros.size();
        
        // Calcular superficie usada
        BigDecimal superficieUsadaSecciones = ranchoRepository.sumSuperficieSeccionesByRanchoId(ranchoId);
        BigDecimal superficieUsadaPotreros = ranchoRepository.sumSuperficioPotrerosDirectosByRanchoId(ranchoId);
        BigDecimal superficieUsadaTotal = superficieUsadaSecciones.add(superficieUsadaPotreros);
        
        // Calcular superficie disponible
        BigDecimal superficieDisponible = SurfaceCalculator.calculateAvailable(
            rancho.getSuperficieTotal(), 
            superficieUsadaTotal
        );
        
        // Calcular porcentaje de uso
        BigDecimal porcentajeUso = SurfaceCalculator.calculateUsagePercentage(
            rancho.getSuperficieTotal(), 
            superficieUsadaTotal
        );
        
        // Calcular distribución por sección
        List<SeccionDistribution> distribucion = calculateSeccionDistribution(secciones, rancho.getSuperficieTotal());
        
        return new RanchoStatsResult(
            totalSecciones,
            totalPotreros,
            rancho.getSuperficieTotal(),
            superficieUsadaTotal,
            superficieDisponible,
            porcentajeUso,
            distribucion
        );
    }
    
    /**
     * Calcula la distribución de superficie por sección.
     */
    private List<SeccionDistribution> calculateSeccionDistribution(List<Seccion> secciones, BigDecimal superficieTotal) {
        List<SeccionDistribution> distribucion = new ArrayList<>();
        
        for (Seccion seccion : secciones) {
            // Calcular porcentaje de esta sección respecto al total del rancho
            BigDecimal porcentaje = SurfaceCalculator.calculateUsagePercentage(
                superficieTotal, 
                seccion.getSuperficie()
            );
            
            // Contar potreros de esta sección
            long countPotreros = seccionRepository.countPotrerosActivosBySeccionId(seccion.getSeccionId());
            
            distribucion.add(new SeccionDistribution(
                seccion.getNombre(),
                seccion.getSuperficie(),
                porcentaje,
                (int) countPotreros
            ));
        }
        
        return distribucion;
    }
}
