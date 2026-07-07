package mx.vacapp.geography.internal.application.usecases.potrero;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.PotreroResult;
import mx.vacapp.geography.internal.domain.model.Potrero;
import mx.vacapp.geography.internal.domain.model.exceptions.EntityNotFoundException;
import mx.vacapp.geography.internal.domain.repository.PotreroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso para obtener un potrero por ID.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetPotreroUseCase {
    
    private final PotreroRepository potreroRepository;
    
    /**
     * Ejecuta la búsqueda de un potrero por ID.
     * 
     * @param potreroId UUID del potrero
     * @return resultado con información del potrero
     * @throws EntityNotFoundException si el potrero no existe
     */
    @Transactional(readOnly = true)
    public PotreroResult execute(UUID potreroId) {
        log.debug("Obteniendo potrero: {}", potreroId);
        
        // Buscar potrero
        Potrero potrero = potreroRepository.findById(potreroId)
            .orElseThrow(() -> new EntityNotFoundException("Potrero no encontrado con ID: " + potreroId));
        
        return new PotreroResult(
            potrero.getPotreroId(),
            potrero.getNombre(),
            potrero.getSuperficie(),
            potrero.getRanchoId(),
            potrero.getSeccionId(),
            potrero.getCattleCount(),
            potrero.getDescripcion(),
            potrero.getStatus(),
            potrero.getTenantId(),
            potrero.getCreatedAt(),
            potrero.getUpdatedAt()
        );
    }
}
