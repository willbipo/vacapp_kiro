package mx.vacapp.cattle.internal.application.usecases.animal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;
import mx.vacapp.cattle.internal.application.usecases.commands.CreateAnimalCommand;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.PastureHistory;
import mx.vacapp.cattle.internal.domain.model.Sex;
import mx.vacapp.cattle.internal.domain.model.exceptions.DuplicateAreteException;
import mx.vacapp.cattle.internal.domain.model.exceptions.InvalidGenealogyException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.CattleAuditRepository;
import mx.vacapp.cattle.internal.domain.repository.PastureHistoryRepository;
import mx.vacapp.cattle.internal.infrastructure.integration.GeographyServiceClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Caso de uso para crear un nuevo animal en el sistema de inventario de ganado.
 * 
 * <p>Este caso de uso implementa el flujo completo de registro de un animal nuevo,
 * incluyendo todas las validaciones de negocio, creación de la entidad de dominio,
 * persistencia del animal, registro en historial de potreros, y auditoría.</p>
 * 
 * <h2>Flujo de Ejecución:</h2>
 * <ol>
 *   <li>Validar que el arete es único a nivel global (no existe en ningún tenant)</li>
 *   <li>Lanzar DuplicateAreteException si el arete ya existe</li>
 *   <li>Validar que el potrero existe y está activo (GeographyService)</li>
 *   <li>Validar genealogía si se proporcionan madre_id o padre_id:
 *     <ul>
 *       <li>Madre debe existir, ser hembra, y pertenecer al mismo rancho</li>
 *       <li>Padre debe existir, ser macho, y pertenecer al mismo rancho</li>
 *     </ul>
 *   </li>
 *   <li>Crear entidad Animal usando Animal.create()</li>
 *   <li>Persistir animal en AnimalRepository</li>
 *   <li>Insertar primer registro en pasture_history con fecha_salida = null</li>
 *   <li>Registrar auditoría de creación en CattleAuditRepository</li>
 *   <li>Retornar AnimalResult con datos del animal creado</li>
 * </ol>
 * 
 * <h2>Validaciones de Negocio:</h2>
 * <ul>
 *   <li><b>Arete único global:</b> El arete debe ser único en todo el sistema,
 *       no solo por tenant (Requirement 1.2)</li>
 *   <li><b>Potrero activo:</b> El potrero debe existir y estar activo en el módulo
 *       geographic-control (Requirement 3.1, 3.2)</li>
 *   <li><b>Genealogía válida:</b> Si se especifica madre, debe ser hembra del mismo rancho.
 *       Si se especifica padre, debe ser macho del mismo rancho (Requirement 2.5, 2.6, 2.7, 2.8)</li>
 * </ul>
 * 
 * <h2>Atomicidad:</h2>
 * <p>Toda la operación se ejecuta en una transacción {@code @Transactional} para garantizar
 * que si alguna parte falla (persistencia animal, historial, auditoría), se hace rollback
 * completo y no se deja el sistema en estado inconsistente.</p>
 * 
 * @see CreateAnimalCommand
 * @see AnimalResult
 * @see Animal
 * @see AnimalRepository
 * @see PastureHistoryRepository
 * @see CattleAuditRepository
 * @see GeographyServiceClient
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateAnimalUseCase {
    
    private final AnimalRepository animalRepository;
    private final PastureHistoryRepository pastureHistoryRepository;
    private final CattleAuditRepository cattleAuditRepository;
    private final GeographyServiceClient geographyServiceClient;
    
    /**
     * Ejecuta el caso de uso de creación de animal.
     * 
     * @param command comando con todos los datos del animal a crear
     * @return AnimalResult con los datos del animal creado
     * @throws DuplicateAreteException si el arete ya existe en el sistema
     * @throws InvalidGenealogyException si madre o padre no cumplen validaciones
     * @throws mx.vacapp.cattle.internal.domain.model.exceptions.InvalidPastureException 
     *         si el potrero no existe o está inactivo
     * @throws IllegalArgumentException si algún parámetro requerido es null o inválido
     */
    @Transactional
    public AnimalResult execute(CreateAnimalCommand command) {
        log.info("Iniciando creación de animal: arete={}, ranchoId={}, tenantId={}", 
                 command.arete(), command.ranchoId(), command.tenantId());
        
        // 1. Validar que arete es único a nivel global (Requirement 1.2)
        validateAreteUnique(command.arete());
        
        // 2. Validar que potrero existe y está activo (Requirement 3.1, 3.2)
        validatePotreroActive(command.potreroId());
        
        // 3. Validar genealogía si se proporcionan madre/padre (Requirement 2.5-2.8)
        validateGenealogy(command.madreId(), command.padreId(), command.ranchoId());
        
        // 4. Crear entidad de dominio Animal (Requirement 1.5)
        Animal animal = Animal.create(
            command.arete(),
            command.sexo(),
            command.raza(),
            command.fechaNacimiento(),
            command.tipo(),
            command.ranchoId(),
            command.tenantId(),
            command.createdBy()
        );
        
        // Aplicar campos opcionales usando el Builder
        animal = new Animal.Builder()
            .from(animal)
            .areteAnterior(command.areteAnterior())
            .fechaAretado(command.fechaAretado())
            .folioReemo(command.folioReemo())
            .nota(command.nota())
            .madreId(command.madreId())
            .padreId(command.padreId())
            .build();
        
        // 5. Persistir animal en repositorio (Requirement 1.5)
        Animal savedAnimal = animalRepository.save(animal);
        log.debug("Animal persistido: animalId={}, arete={}", 
                  savedAnimal.getAnimalId(), savedAnimal.getArete());
        
        // 6. Insertar primer registro en pasture_history (Requirement 3.3)
        PastureHistory initialHistory = PastureHistory.create(
            savedAnimal.getAnimalId(),
            command.potreroId(),
            command.createdBy()
        );
        pastureHistoryRepository.insert(initialHistory);
        log.debug("Historial de potrero creado: animalId={}, potreroId={}", 
                  savedAnimal.getAnimalId(), command.potreroId());
        
        // 7. Registrar auditoría de creación (Requirement 1.9, 14.2)
        cattleAuditRepository.logAnimalCreation(
            savedAnimal.getAnimalId(),
            savedAnimal,
            command.createdBy()
        );
        log.debug("Auditoría de creación registrada: animalId={}", savedAnimal.getAnimalId());
        
        log.info("Animal creado exitosamente: animalId={}, arete={}", 
                 savedAnimal.getAnimalId(), savedAnimal.getArete());
        
        // 8. Retornar resultado
        return AnimalResult.fromDomain(savedAnimal);
    }
    
    /**
     * Valida que el arete no exista en el sistema.
     * La validación es a nivel GLOBAL (no por tenant) según Requirement 1.2.
     * 
     * @param arete número de arete a validar
     * @throws DuplicateAreteException si el arete ya existe
     */
    private void validateAreteUnique(String arete) {
        log.debug("Validando unicidad de arete: {}", arete);
        
        if (animalRepository.existsByArete(arete)) {
            log.warn("Intento de crear animal con arete duplicado: {}", arete);
            throw new DuplicateAreteException("Arete ya registrado en el sistema");
        }
    }
    
    /**
     * Valida que el potrero existe y está activo en el módulo geographic-control.
     * 
     * @param potreroId UUID del potrero a validar
     * @throws mx.vacapp.cattle.internal.domain.model.exceptions.InvalidPastureException 
     *         si el potrero no existe o está inactivo
     */
    private void validatePotreroActive(java.util.UUID potreroId) {
        log.debug("Validando potrero activo: potreroId={}", potreroId);
        
        // GeographyServiceClient.validatePotreroActive() lanza InvalidPastureException si no está activo
        geographyServiceClient.validatePotreroActive(potreroId);
    }
    
    /**
     * Valida la genealogía del animal si se proporcionan madre o padre.
     * 
     * <p>Validaciones:</p>
     * <ul>
     *   <li>Si se especifica madre_id: debe existir, ser hembra, y pertenecer al mismo rancho</li>
     *   <li>Si se especifica padre_id: debe existir, ser macho, y pertenecer al mismo rancho</li>
     * </ul>
     * 
     * @param madreId UUID de la madre (puede ser null)
     * @param padreId UUID del padre (puede ser null)
     * @param ranchoId UUID del rancho del animal
     * @throws InvalidGenealogyException si madre o padre no cumplen validaciones
     */
    private void validateGenealogy(java.util.UUID madreId, java.util.UUID padreId, java.util.UUID ranchoId) {
        // Validar madre si se proporciona (Requirement 2.5, 2.7)
        if (madreId != null) {
            log.debug("Validando madre: madreId={}", madreId);
            
            Optional<Animal> madre = animalRepository.findById(madreId);
            
            if (madre.isEmpty()) {
                log.warn("Madre no encontrada: madreId={}", madreId);
                throw new InvalidGenealogyException("Madre inválida");
            }
            
            if (madre.get().getSexo() != Sex.HEMBRA) {
                log.warn("Madre no es hembra: madreId={}, sexo={}", madreId, madre.get().getSexo());
                throw new InvalidGenealogyException("Madre inválida");
            }
            
            if (!madre.get().getRanchoId().equals(ranchoId)) {
                log.warn("Madre pertenece a otro rancho: madreId={}, ranchoMadre={}, ranchoAnimal={}", 
                         madreId, madre.get().getRanchoId(), ranchoId);
                throw new InvalidGenealogyException("Madre inválida");
            }
        }
        
        // Validar padre si se proporciona (Requirement 2.6, 2.8)
        if (padreId != null) {
            log.debug("Validando padre: padreId={}", padreId);
            
            Optional<Animal> padre = animalRepository.findById(padreId);
            
            if (padre.isEmpty()) {
                log.warn("Padre no encontrado: padreId={}", padreId);
                throw new InvalidGenealogyException("Padre inválido");
            }
            
            if (padre.get().getSexo() != Sex.MACHO) {
                log.warn("Padre no es macho: padreId={}, sexo={}", padreId, padre.get().getSexo());
                throw new InvalidGenealogyException("Padre inválido");
            }
            
            if (!padre.get().getRanchoId().equals(ranchoId)) {
                log.warn("Padre pertenece a otro rancho: padreId={}, ranchoPadre={}, ranchoAnimal={}", 
                         padreId, padre.get().getRanchoId(), ranchoId);
                throw new InvalidGenealogyException("Padre inválido");
            }
        }
    }
}
