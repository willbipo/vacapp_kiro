package mx.vacapp.geography.internal.application.usecases.seccion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.domain.model.Seccion;
import mx.vacapp.geography.internal.domain.model.exceptions.CannotArchiveWithChildrenException;
import mx.vacapp.geography.internal.domain.model.exceptions.EntityNotFoundException;
import mx.vacapp.geography.internal.domain.repository.GeographyAuditRepository;
import mx.vacapp.geography.internal.domain.repository.SeccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso para archivar una sección.
 * 
 * Validaciones:
 * - Sección existe
 * - No tiene potreros activos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveSeccionUseCase {
    
    private final SeccionRepository seccionRepository;
    private final GeographyAuditRepository auditRepository;
    
    /**
     * Ejecuta el archivado de una sección.
     * 
     * @param seccionId UUID de la sección a archivar
     * @param userId UUID del usuario que realiza la operación
     * @throws EntityNotFoundException si la sección no existe
     * @throws CannotArchiveWithChildrenException si tiene potreros activos
     */
    @Transactional
    public void execute(UUID seccionId, UUID userId) {
        log.debug("Archivando sección: {}", seccionId);
        
        // Buscar sección existente
        Seccion seccion = seccionRepository.findById(seccionId)
            .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada con ID: " + seccionId));
        
        // Validar que no tenga potreros activos
        if (seccionRepository.hasActivePotreros(seccionId)) {
            throw new CannotArchiveWithChildrenException(
                "No se puede archivar una sección con potreros activos"
            );
        }
        
        // Archivar mediante método de dominio
        Seccion archivedSeccion = seccion.archive(userId);
        
        // Persistir
        seccionRepository.save(archivedSeccion);
        
        // Registrar auditoría
        auditRepository.logSeccionArchive(seccion, archivedSeccion, userId);
        
        log.info("Sección archivada: {}", seccionId);
    }
}
