package mx.vacapp.geography.internal.application.usecases.potrero;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.CreatePotreroCommand;
import mx.vacapp.geography.internal.application.usecases.commands.PotreroResult;
import mx.vacapp.geography.internal.domain.model.Potrero;
import mx.vacapp.geography.internal.domain.model.Rancho;
import mx.vacapp.geography.internal.domain.model.Seccion;
import mx.vacapp.geography.internal.domain.model.SurfaceCalculator;
import mx.vacapp.geography.internal.domain.model.exceptions.DuplicateNameException;
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

/**
 * Caso de uso para crear un nuevo potrero.
 * 
 * El potrero puede vincularse:
 * - Directamente al rancho (seccionId = null) si el rancho no tiene secciones
 * - A una sección (seccionId != null) si el rancho usa configuración compleja
 * 
 * Validaciones:
 * - Rancho existe
 * - Si seccionId != null: sección existe y pertenece al rancho
 * - Nombre único dentro del rancho/sección (case-insensitive)
 * - Superficie <= superficie disponible del contenedor padre
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreatePotreroUseCase {
    
    private final PotreroRepository potreroRepository;
    private final RanchoRepository ranchoRepository;
    private final SeccionRepository seccionRepository;
    private final GeographyAuditRepository auditRepository;
    
    /**
     * Ejecuta la creación de un potrero.
     * 
     * Invalida el cache de estadísticas del rancho al crear un nuevo potrero.
     * 
     * @param command comando con datos del potrero
     * @return resultado con información del potrero creado
     * @throws EntityNotFoundException si el rancho o sección no existen
     * @throws DuplicateNameException si ya existe un potrero con ese nombre
     * @throws SurfaceExceededException si la superficie excede la disponible
     */
    @CacheEvict(value = CacheNames.RANCHO_STATS, key = "'stats:rancho:' + #command.ranchoId() + ':tenant:' + #command.tenantId()")
    @Transactional
    public PotreroResult execute(CreatePotreroCommand command) {
        log.debug("Creando potrero: {} en rancho: {}", command.nombre(), command.ranchoId());
        
        // Validar que el rancho existe
        Rancho rancho = ranchoRepository.findById(command.ranchoId())
            .orElseThrow(() -> new EntityNotFoundException("Rancho no encontrado con ID: " + command.ranchoId()));
        
        Seccion seccion = null;
        BigDecimal superficieDisponible;
        
        // Si tiene seccionId, validar configuración compleja
        if (command.seccionId() != null) {
            // Buscar sección
            seccion = seccionRepository.findById(command.seccionId())
                .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada con ID: " + command.seccionId()));
            
            // Validar que la sección pertenece al rancho
            if (!seccion.getRanchoId().equals(command.ranchoId())) {
                throw new IllegalArgumentException("La sección no pertenece al rancho especificado");
            }
            
            // Validar nombre único dentro de la sección
            if (potreroRepository.existsByNombreAndSeccionIdAndTenantId(command.nombre(), command.seccionId(), command.tenantId())) {
                throw new DuplicateNameException("Ya existe un potrero con el nombre: " + command.nombre() + " en esta sección");
            }
            
            // Calcular superficie disponible en la sección
            BigDecimal superficieUsadaPotreros = seccionRepository.sumSuperficiePotrerosSeccionId(command.seccionId());
            superficieDisponible = SurfaceCalculator.calculateAvailable(seccion.getSuperficie(), superficieUsadaPotreros);
            
        } else {
            // Potrero directo al rancho
            
            // Validar que el rancho no tenga secciones (configuración simple)
            if (ranchoRepository.hasActiveSecciones(command.ranchoId())) {
                throw new IllegalArgumentException(
                    "Este rancho utiliza secciones. Cree el potrero dentro de una sección"
                );
            }
            
            // Validar nombre único dentro del rancho
            if (potreroRepository.existsByNombreAndRanchoIdAndTenantId(command.nombre(), command.ranchoId(), command.tenantId())) {
                throw new DuplicateNameException("Ya existe un potrero con el nombre: " + command.nombre() + " en este rancho");
            }
            
            // Calcular superficie disponible en el rancho
            BigDecimal superficieUsadaPotreros = ranchoRepository.sumSuperficioPotrerosDirectosByRanchoId(command.ranchoId());
            superficieDisponible = SurfaceCalculator.calculateAvailable(rancho.getSuperficieTotal(), superficieUsadaPotreros);
        }
        
        // Validar que la superficie del potrero <= disponible
        if (command.superficie().compareTo(superficieDisponible) > 0) {
            throw new SurfaceExceededException(
                String.format("La superficie excede la disponible (disponible: %s metros cuadrados)", superficieDisponible)
            );
        }
        
        // Crear entidad de dominio con status ACTIVE y cattle_count = 0
        Potrero potrero = Potrero.create(
            command.nombre(),
            command.superficie(),
            command.ranchoId(),
            command.seccionId(),
            command.descripcion(),
            command.tenantId(),
            command.userId()
        );
        
        // Persistir
        Potrero savedPotrero = potreroRepository.save(potrero);
        
        // Registrar auditoría
        auditRepository.logPotreroCreation(savedPotrero, command.userId());
        
        log.info("Potrero creado: {} con ID: {}", savedPotrero.getNombre(), savedPotrero.getPotreroId());
        
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
