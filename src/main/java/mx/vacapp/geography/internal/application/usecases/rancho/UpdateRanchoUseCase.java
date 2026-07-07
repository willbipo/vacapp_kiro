package mx.vacapp.geography.internal.application.usecases.rancho;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.RanchoResult;
import mx.vacapp.geography.internal.application.usecases.commands.UpdateRanchoCommand;
import mx.vacapp.geography.internal.domain.model.Rancho;
import mx.vacapp.geography.internal.domain.model.SurfaceCalculator;
import mx.vacapp.geography.internal.domain.model.exceptions.EntityNotFoundException;
import mx.vacapp.geography.internal.domain.model.exceptions.SurfaceExceededException;
import mx.vacapp.geography.internal.domain.repository.GeographyAuditRepository;
import mx.vacapp.geography.internal.domain.repository.RanchoRepository;
import mx.vacapp.geography.internal.infrastructure.config.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Caso de uso para actualizar un rancho existente.
 * 
 * Validaciones:
 * - Rancho existe
 * - Nueva superficie >= superficie usada por secciones/potreros hijos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateRanchoUseCase {
    
    private final RanchoRepository ranchoRepository;
    private final GeographyAuditRepository auditRepository;
    
    /**
     * Ejecuta la actualización de un rancho.
     * 
     * Invalida el cache de estadísticas al actualizar el rancho.
     * 
     * @param command comando con datos actualizados
     * @return resultado con información del rancho actualizado
     * @throws EntityNotFoundException si el rancho no existe
     * @throws SurfaceExceededException si la nueva superficie es menor a la usada
     */
    @CacheEvict(value = CacheNames.RANCHO_STATS, key = "'stats:rancho:' + #command.ranchoId() + ':tenant:' + #command.tenantId()")
    @Transactional
    public RanchoResult execute(UpdateRanchoCommand command) {
        log.debug("Actualizando rancho: {}", command.ranchoId());
        
        // Buscar rancho existente
        Rancho rancho = ranchoRepository.findById(command.ranchoId())
            .orElseThrow(() -> new EntityNotFoundException("Rancho no encontrado con ID: " + command.ranchoId()));
        
        // Calcular superficie usada actual
        BigDecimal superficieUsadaSecciones = ranchoRepository.sumSuperficieSeccionesByRanchoId(rancho.getRanchoId());
        BigDecimal superficieUsadaPotreros = ranchoRepository.sumSuperficioPotrerosDirectosByRanchoId(rancho.getRanchoId());
        BigDecimal superficieUsadaTotal = superficieUsadaSecciones.add(superficieUsadaPotreros);
        
        // Validar que nueva superficie >= superficie usada
        if (command.superficieTotal().compareTo(superficieUsadaTotal) < 0) {
            throw new SurfaceExceededException(
                String.format("La superficie del rancho debe ser mayor o igual a la superficie usada (%s metros cuadrados)", 
                    superficieUsadaTotal)
            );
        }
        
        // Actualizar mediante método de dominio
        Rancho updatedRancho = new Rancho.Builder()
            .from(rancho)
            .nombre(command.nombre())
            .superficieTotal(command.superficieTotal())
            .descripcion(command.descripcion())
            .updatedBy(command.userId())
            .build();
        
        // Persistir
        Rancho savedRancho = ranchoRepository.save(updatedRancho);
        
        // Registrar auditoría
        auditRepository.logRanchoUpdate(rancho, savedRancho, command.userId());
        
        log.info("Rancho actualizado: {}", savedRancho.getRanchoId());
        
        // Calcular superficie disponible
        BigDecimal superficieDisponible = SurfaceCalculator.calculateAvailable(
            savedRancho.getSuperficieTotal(), 
            superficieUsadaTotal
        );
        
        return new RanchoResult(
            savedRancho.getRanchoId(),
            savedRancho.getNombre(),
            savedRancho.getSuperficieTotal(),
            superficieDisponible,
            superficieUsadaTotal,
            savedRancho.getDescripcion(),
            savedRancho.getStatus(),
            savedRancho.getTenantId(),
            savedRancho.getCreatedAt(),
            savedRancho.getUpdatedAt()
        );
    }
}
