package mx.vacapp.geography.internal.application.usecases.rancho;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.CreateRanchoCommand;
import mx.vacapp.geography.internal.application.usecases.commands.RanchoResult;
import mx.vacapp.geography.internal.domain.model.Rancho;
import mx.vacapp.geography.internal.domain.model.SurfaceCalculator;
import mx.vacapp.geography.internal.domain.model.exceptions.DuplicateNameException;
import mx.vacapp.geography.internal.domain.repository.GeographyAuditRepository;
import mx.vacapp.geography.internal.domain.repository.RanchoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Caso de uso para crear un nuevo rancho.
 * 
 * Validaciones:
 * - Nombre único dentro del tenant (case-insensitive)
 * - Superficie válida (> 0)
 * 
 * Registra auditoría de creación.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateRanchoUseCase {
    
    private final RanchoRepository ranchoRepository;
    private final GeographyAuditRepository auditRepository;
    
    /**
     * Ejecuta la creación de un rancho.
     * 
     * @param command comando con datos del rancho
     * @return resultado con información del rancho creado
     * @throws DuplicateNameException si ya existe un rancho con ese nombre
     */
    @Transactional
    public RanchoResult execute(CreateRanchoCommand command) {
        log.debug("Creando rancho: {} para tenant: {}", command.nombre(), command.tenantId());
        
        // Validar unicidad de nombre
        if (ranchoRepository.existsByNombreAndTenantId(command.nombre(), command.tenantId())) {
            throw new DuplicateNameException("Ya existe un rancho con el nombre: " + command.nombre());
        }
        
        // Crear entidad de dominio con status ACTIVE
        Rancho rancho = Rancho.create(
            command.nombre(),
            command.superficieTotal(),
            command.descripcion(),
            command.tenantId(),
            command.userId()
        );
        
        // Persistir
        Rancho savedRancho = ranchoRepository.save(rancho);
        
        // Registrar auditoría
        auditRepository.logRanchoCreation(savedRancho, command.userId());
        
        log.info("Rancho creado: {} con ID: {}", savedRancho.getNombre(), savedRancho.getRanchoId());
        
        // Retornar resultado con superficie_disponible = superficie_total (sin hijos aún)
        return new RanchoResult(
            savedRancho.getRanchoId(),
            savedRancho.getNombre(),
            savedRancho.getSuperficieTotal(),
            savedRancho.getSuperficieTotal(), // disponible = total al inicio
            BigDecimal.ZERO, // usada = 0 al inicio
            savedRancho.getDescripcion(),
            savedRancho.getStatus(),
            savedRancho.getTenantId(),
            savedRancho.getCreatedAt(),
            savedRancho.getUpdatedAt()
        );
    }
}
