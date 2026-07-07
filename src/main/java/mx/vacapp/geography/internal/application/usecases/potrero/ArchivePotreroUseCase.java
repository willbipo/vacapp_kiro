package mx.vacapp.geography.internal.application.usecases.potrero;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.domain.model.Potrero;
import mx.vacapp.geography.internal.domain.model.exceptions.CattleAssignedException;
import mx.vacapp.geography.internal.domain.model.exceptions.EntityNotFoundException;
import mx.vacapp.geography.internal.domain.repository.GeographyAuditRepository;
import mx.vacapp.geography.internal.domain.repository.PotreroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso para archivar un potrero.
 * 
 * Validaciones:
 * - Potrero existe
 * - No tiene ganado asignado (cattle_count == 0)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArchivePotreroUseCase {
    
    private final PotreroRepository potreroRepository;
    private final GeographyAuditRepository auditRepository;
    
    /**
     * Ejecuta el archivado de un potrero.
     * 
     * @param potreroId UUID del potrero a archivar
     * @param userId UUID del usuario que realiza la operación
     * @throws EntityNotFoundException si el potrero no existe
     * @throws CattleAssignedException si el potrero tiene ganado asignado
     */
    @Transactional
    public void execute(UUID potreroId, UUID userId) {
        log.debug("Archivando potrero: {}", potreroId);
        
        // Buscar potrero existente
        Potrero potrero = potreroRepository.findById(potreroId)
            .orElseThrow(() -> new EntityNotFoundException("Potrero no encontrado con ID: " + potreroId));
        
        // Validar que no tenga ganado asignado
        if (potrero.hasCattle()) {
            throw new CattleAssignedException(
                "No se puede archivar un potrero con ganado asignado. Traslade el ganado primero"
            );
        }
        
        // Archivar mediante método de dominio
        Potrero archivedPotrero = potrero.archive(userId);
        
        // Persistir
        potreroRepository.save(archivedPotrero);
        
        // Registrar auditoría
        auditRepository.logPotreroArchive(potrero, archivedPotrero, userId);
        
        log.info("Potrero archivado: {}", potreroId);
    }
}
