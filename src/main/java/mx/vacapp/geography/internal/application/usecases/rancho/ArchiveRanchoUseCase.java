package mx.vacapp.geography.internal.application.usecases.rancho;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.domain.model.Rancho;
import mx.vacapp.geography.internal.domain.model.exceptions.CannotArchiveWithChildrenException;
import mx.vacapp.geography.internal.domain.model.exceptions.EntityNotFoundException;
import mx.vacapp.geography.internal.domain.repository.GeographyAuditRepository;
import mx.vacapp.geography.internal.domain.repository.RanchoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso para archivar un rancho.
 * 
 * Validaciones:
 * - Rancho existe
 * - No tiene secciones activas
 * - No tiene potreros activos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveRanchoUseCase {
    
    private final RanchoRepository ranchoRepository;
    private final GeographyAuditRepository auditRepository;
    
    /**
     * Ejecuta el archivado de un rancho.
     * 
     * @param ranchoId UUID del rancho a archivar
     * @param userId UUID del usuario que realiza la operación
     * @throws EntityNotFoundException si el rancho no existe
     * @throws CannotArchiveWithChildrenException si tiene secciones o potreros activos
     */
    @Transactional
    public void execute(UUID ranchoId, UUID userId) {
        log.debug("Archivando rancho: {}", ranchoId);
        
        // Buscar rancho existente
        Rancho rancho = ranchoRepository.findById(ranchoId)
            .orElseThrow(() -> new EntityNotFoundException("Rancho no encontrado con ID: " + ranchoId));
        
        // Validar que no tenga secciones activas
        if (ranchoRepository.hasActiveSecciones(ranchoId)) {
            throw new CannotArchiveWithChildrenException(
                "No se puede archivar un rancho con secciones activas"
            );
        }
        
        // Validar que no tenga potreros activos
        if (ranchoRepository.hasActivePotreros(ranchoId)) {
            throw new CannotArchiveWithChildrenException(
                "No se puede archivar un rancho con potreros activos"
            );
        }
        
        // Archivar mediante método de dominio
        Rancho archivedRancho = rancho.archive(userId);
        
        // Persistir
        ranchoRepository.save(archivedRancho);
        
        // Registrar auditoría
        auditRepository.logRanchoArchive(rancho, archivedRancho, userId);
        
        log.info("Rancho archivado: {}", ranchoId);
    }
}
