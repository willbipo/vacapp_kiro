package mx.vacapp.geography.internal.application.usecases.potrero;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.PotreroResult;
import mx.vacapp.geography.internal.application.usecases.commands.UpdatePotreroCommand;
import mx.vacapp.geography.internal.domain.model.Potrero;
import mx.vacapp.geography.internal.domain.model.Rancho;
import mx.vacapp.geography.internal.domain.model.Seccion;
import mx.vacapp.geography.internal.domain.model.SurfaceCalculator;
import mx.vacapp.geography.internal.domain.model.exceptions.EntityNotFoundException;
import mx.vacapp.geography.internal.domain.model.exceptions.SurfaceExceededException;
import mx.vacapp.geography.internal.domain.repository.GeographyAuditRepository;
import mx.vacapp.geography.internal.domain.repository.PotreroRepository;
import mx.vacapp.geography.internal.domain.repository.RanchoRepository;
import mx.vacapp.geography.internal.domain.repository.SeccionRepository;
import mx.vacapp.geography.internal.infrastructure.config.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Caso de uso para actualizar un potrero existente.
 * 
 * Validaciones:
 * - Potrero existe
 * - Nueva superficie >= 0
 * - Suma de superficies de potreros hermanos + nueva superficie <= superficie del contenedor padre
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdatePotreroUseCase {
    
    private final PotreroRepository potreroRepository;
    private final SeccionRepository seccionRepository;
    private final RanchoRepository ranchoRepository;
    private final GeographyAuditRepository auditRepository;
    
    /**
     * Ejecuta la actualización de un potrero.
     * 
     * Invalida todas las entradas del cache de estadísticas al actualizar el potrero.
     * 
     * @param command comando con datos actualizados
     * @return resultado con información del potrero actualizado
     * @throws EntityNotFoundException si el potrero no existe
     * @throws SurfaceExceededException si la validación de superficie falla
     */
    @CacheEvict(value = CacheNames.RANCHO_STATS, allEntries = true)
    @Transactional
    public PotreroResult execute(UpdatePotreroCommand command) {
        log.debug("Actualizando potrero: {}", command.potreroId());
        
        // Buscar potrero existente
        Potrero potrero = potreroRepository.findById(command.potreroId())
            .orElseThrow(() -> new EntityNotFoundException("Potrero no encontrado con ID: " + command.potreroId()));
        
        // Determinar el contenedor padre y validar superficie
        BigDecimal superficieContenedor;
        List<Potrero> potrerosHermanos;
        
        if (potrero.getSeccionId() != null) {
            // Potrero vinculado a sección
            Seccion seccion = seccionRepository.findById(potrero.getSeccionId())
                .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada"));
            
            superficieContenedor = seccion.getSuperficie();
            potrerosHermanos = potreroRepository.findActiveBySeccionIdAndTenantId(potrero.getSeccionId(), potrero.getTenantId());
            
        } else {
            // Potrero vinculado directamente al rancho
            Rancho rancho = ranchoRepository.findById(potrero.getRanchoId())
                .orElseThrow(() -> new EntityNotFoundException("Rancho no encontrado"));
            
            superficieContenedor = rancho.getSuperficieTotal();
            potrerosHermanos = potreroRepository.findActiveDirectByRanchoIdAndTenantId(potrero.getRanchoId(), potrero.getTenantId());
        }
        
        // Calcular suma de superficies de potreros hermanos (excluyendo este)
        BigDecimal sumaHermanos = potrerosHermanos.stream()
            .filter(p -> !p.getPotreroId().equals(potrero.getPotreroId()))
            .map(Potrero::getSuperficie)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Validar que suma de hermanos + nueva superficie <= superficie del contenedor
        if (!SurfaceCalculator.validateAddition(superficieContenedor, sumaHermanos, command.superficie())) {
            BigDecimal disponible = SurfaceCalculator.calculateAvailable(superficieContenedor, sumaHermanos);
            throw new SurfaceExceededException(
                String.format("La superficie excede la disponible (disponible: %s metros cuadrados)", disponible)
            );
        }
        
        // Actualizar mediante Builder
        Potrero updatedPotrero = new Potrero.Builder()
            .from(potrero)
            .nombre(command.nombre())
            .superficie(command.superficie())
            .descripcion(command.descripcion())
            .updatedBy(command.userId())
            .build();
        
        // Persistir
        Potrero savedPotrero = potreroRepository.save(updatedPotrero);
        
        // Registrar auditoría
        auditRepository.logPotreroUpdate(potrero, savedPotrero, command.userId());
        
        log.info("Potrero actualizado: {}", savedPotrero.getPotreroId());
        
        return new PotreroResult(
            savedPotrero.getPotreroId(),
            savedPotrero.getNombre(),
            savedPotrero.getSuperficie(),
            savedPotrero.getRanchoId(),
            savedPotrero.getSeccionId(),
            savedPotrero.getCattleCount(),
            savedPotrero.getDescripcion(),
            savedPotrero.getStatus(),
            savedPotrero.getTenantId(),
            savedPotrero.getCreatedAt(),
            savedPotrero.getUpdatedAt()
        );
    }
}
