package mx.vacapp.geography.internal.application.usecases.seccion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.CreateSeccionCommand;
import mx.vacapp.geography.internal.application.usecases.commands.SeccionResult;
import mx.vacapp.geography.internal.domain.model.Rancho;
import mx.vacapp.geography.internal.domain.model.Seccion;
import mx.vacapp.geography.internal.domain.model.SurfaceCalculator;
import mx.vacapp.geography.internal.domain.model.exceptions.DuplicateNameException;
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

/**
 * Caso de uso para crear una nueva sección dentro de un rancho.
 * 
 * Validaciones:
 * - Rancho existe
 * - Nombre único dentro del rancho (case-insensitive)
 * - Suma de superficies de secciones <= rancho.superficieTotal
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateSeccionUseCase {
    
    private final SeccionRepository seccionRepository;
    private final RanchoRepository ranchoRepository;
    private final GeographyAuditRepository auditRepository;
    
    /**
     * Ejecuta la creación de una sección.
     * 
     * Invalida el cache de estadísticas del rancho al crear una nueva sección.
     * 
     * @param command comando con datos de la sección
     * @return resultado con información de la sección creada
     * @throws EntityNotFoundException si el rancho no existe
     * @throws DuplicateNameException si ya existe una sección con ese nombre en el rancho
     * @throws SurfaceExceededException si la suma excede la superficie del rancho
     */
    @CacheEvict(value = CacheNames.RANCHO_STATS, key = "'stats:rancho:' + #command.ranchoId() + ':tenant:' + #command.tenantId()")
    @Transactional
    public SeccionResult execute(CreateSeccionCommand command) {
        log.debug("Creando sección: {} en rancho: {}", command.nombre(), command.ranchoId());
        
        // Validar que el rancho existe
        Rancho rancho = ranchoRepository.findById(command.ranchoId())
            .orElseThrow(() -> new EntityNotFoundException("Rancho no encontrado con ID: " + command.ranchoId()));
        
        // Validar unicidad de nombre dentro del rancho
        if (seccionRepository.existsByNombreAndRanchoIdAndTenantId(command.nombre(), command.ranchoId(), command.tenantId())) {
            throw new DuplicateNameException("Ya existe una sección con el nombre: " + command.nombre() + " en este rancho");
        }
        
        // Calcular superficie usada actual por secciones
        BigDecimal superficieUsadaSecciones = ranchoRepository.sumSuperficieSeccionesByRanchoId(command.ranchoId());
        
        // Validar que la suma no exceda la superficie total del rancho
        if (!SurfaceCalculator.validateAddition(rancho.getSuperficieTotal(), superficieUsadaSecciones, command.superficie())) {
            BigDecimal disponible = SurfaceCalculator.calculateAvailable(rancho.getSuperficieTotal(), superficieUsadaSecciones);
            throw new SurfaceExceededException(
                String.format("La suma de superficies de secciones excede la superficie total del rancho (disponible: %s metros cuadrados)", 
                    disponible)
            );
        }
        
        // Crear entidad de dominio con status ACTIVE
        Seccion seccion = Seccion.create(
            command.nombre(),
            command.superficie(),
            command.ranchoId(),
            command.descripcion(),
            command.tenantId(),
            command.userId()
        );
        
        // Persistir
        Seccion savedSeccion = seccionRepository.save(seccion);
        
        // Registrar auditoría
        auditRepository.logSeccionCreation(savedSeccion, command.userId());
        
        log.info("Sección creada: {} con ID: {}", savedSeccion.getNombre(), savedSeccion.getSeccionId());
        
        // Retornar resultado con superficie_disponible = superficie (sin potreros aún)
        return new SeccionResult(
            savedSeccion.getSeccionId(),
            savedSeccion.getNombre(),
            savedSeccion.getSuperficie(),
            savedSeccion.getSuperficie(), // disponible = total al inicio
            BigDecimal.ZERO, // usada = 0 al inicio
            savedSeccion.getRanchoId(),
            savedSeccion.getDescripcion(),
            savedSeccion.getStatus(),
            savedSeccion.getTenantId(),
            savedSeccion.getCreatedAt(),
            savedSeccion.getUpdatedAt()
        );
    }
}
