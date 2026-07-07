package mx.vacapp.geography.internal.application.usecases.seccion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.SeccionResult;
import mx.vacapp.geography.internal.application.usecases.commands.UpdateSeccionCommand;
import mx.vacapp.geography.internal.domain.model.Rancho;
import mx.vacapp.geography.internal.domain.model.Seccion;
import mx.vacapp.geography.internal.domain.model.SurfaceCalculator;
import mx.vacapp.geography.internal.domain.model.exceptions.EntityNotFoundException;
import mx.vacapp.geography.internal.domain.model.exceptions.SurfaceExceededException;
import mx.vacapp.geography.internal.domain.repository.GeographyAuditRepository;
import mx.vacapp.geography.internal.domain.repository.RanchoRepository;
import mx.vacapp.geography.internal.domain.repository.SeccionRepository;
import mx.vacapp.geography.internal.infrastructure.config.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Caso de uso para actualizar una sección existente.
 * 
 * Validaciones:
 * - Sección existe
 * - Nueva superficie >= superficie usada por potreros hijos
 * - Suma de superficies de secciones hermanas + nueva superficie <= rancho.superficieTotal
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateSeccionUseCase {
    
    private final SeccionRepository seccionRepository;
    private final RanchoRepository ranchoRepository;
    private final GeographyAuditRepository auditRepository;
    
    /**
     * Ejecuta la actualización de una sección.
     * 
     * Invalida todas las entradas del cache de estadísticas al actualizar la sección.
     * 
     * @param command comando con datos actualizados
     * @return resultado con información de la sección actualizada
     * @throws EntityNotFoundException si la sección no existe
     * @throws SurfaceExceededException si la validación de superficie falla
     */
    @CacheEvict(value = CacheNames.RANCHO_STATS, allEntries = true)
    @Transactional
    public SeccionResult execute(UpdateSeccionCommand command) {
        log.debug("Actualizando sección: {}", command.seccionId());
        
        // Buscar sección existente
        Seccion seccion = seccionRepository.findById(command.seccionId())
            .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada con ID: " + command.seccionId()));
        
        // Buscar rancho padre
        Rancho rancho = ranchoRepository.findById(seccion.getRanchoId())
            .orElseThrow(() -> new EntityNotFoundException("Rancho no encontrado"));
        
        // Calcular superficie usada por potreros de esta sección
        BigDecimal superficieUsadaPotreros = seccionRepository.sumSuperficiePotrerosSeccionId(seccion.getSeccionId());
        
        // Validar que nueva superficie >= superficie usada por potreros
        if (command.superficie().compareTo(superficieUsadaPotreros) < 0) {
            throw new SurfaceExceededException(
                String.format("La superficie de la sección debe ser mayor o igual a la superficie usada por potreros (%s metros cuadrados)", 
                    superficieUsadaPotreros)
            );
        }
        
        // Calcular suma de superficies de secciones hermanas (excluyendo esta)
        List<Seccion> seccionesHermanas = seccionRepository.findActiveByRanchoIdAndTenantId(seccion.getRanchoId(), seccion.getTenantId());
        BigDecimal sumaHermanas = seccionesHermanas.stream()
            .filter(s -> !s.getSeccionId().equals(seccion.getSeccionId()))
            .map(Seccion::getSuperficie)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Validar que suma de hermanas + nueva superficie <= rancho.superficieTotal
        if (!SurfaceCalculator.validateAddition(rancho.getSuperficieTotal(), sumaHermanas, command.superficie())) {
            BigDecimal disponible = SurfaceCalculator.calculateAvailable(rancho.getSuperficieTotal(), sumaHermanas);
            throw new SurfaceExceededException(
                String.format("La suma de superficies de secciones excede la superficie total del rancho (disponible: %s metros cuadrados)", 
                    disponible)
            );
        }
        
        // Actualizar mediante Builder
        Seccion updatedSeccion = new Seccion.Builder()
            .from(seccion)
            .nombre(command.nombre())
            .superficie(command.superficie())
            .descripcion(command.descripcion())
            .updatedBy(command.userId())
            .build();
        
        // Persistir
        Seccion savedSeccion = seccionRepository.save(updatedSeccion);
        
        // Registrar auditoría
        auditRepository.logSeccionUpdate(seccion, savedSeccion, command.userId());
        
        log.info("Sección actualizada: {}", savedSeccion.getSeccionId());
        
        // Calcular superficie disponible
        BigDecimal superficieDisponible = SurfaceCalculator.calculateAvailable(
            savedSeccion.getSuperficie(), 
            superficieUsadaPotreros
        );
        
        return new SeccionResult(
            savedSeccion.getSeccionId(),
            savedSeccion.getNombre(),
            savedSeccion.getSuperficie(),
            superficieDisponible,
            superficieUsadaPotreros,
            savedSeccion.getRanchoId(),
            savedSeccion.getDescripcion(),
            savedSeccion.getStatus(),
            savedSeccion.getTenantId(),
            savedSeccion.getCreatedAt(),
            savedSeccion.getUpdatedAt()
        );
    }
}
