package mx.vacapp.cattle.internal.application.usecases.animal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;
import mx.vacapp.cattle.internal.application.usecases.commands.UpdateAnimalCommand;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.AgeCalculator;
import mx.vacapp.cattle.internal.domain.model.Sex;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.model.exceptions.DuplicateAreteException;
import mx.vacapp.cattle.internal.domain.model.exceptions.InvalidGenealogyException;
import mx.vacapp.cattle.internal.domain.model.exceptions.SoldOrDeadAnimalException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.CattleAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Caso de uso para actualizar información de un animal existente en el inventario.
 * 
 * <p>Este caso de uso implementa el flujo completo de actualización de un animal,
 * incluyendo todas las validaciones de negocio, restricciones de estado (no vendido/muerto),
 * validación de genealogía, y auditoría de cambios.</p>
 * 
 * <h2>Flujo de Ejecución:</h2>
 * <ol>
 *   <li>Validar que el animal existe y pertenece al tenant del usuario</li>
 *   <li>Validar que el animal NO está en status Vendida o Muerta (Requirement 16.1)</li>
 *   <li>Lanzar SoldOrDeadAnimalException si el animal está vendido o muerto</li>
 *   <li>Si se actualiza arete, validar que el nuevo arete es único a nivel global</li>
 *   <li>Si se actualizan madre_id o padre_id, validar genealogía:
 *     <ul>
 *       <li>Madre debe existir, ser hembra, y pertenecer al mismo rancho</li>
 *       <li>Padre debe existir, ser macho, y pertenecer al mismo rancho</li>
 *     </ul>
 *   </li>
 *   <li>Actualizar campos modificables usando Animal.Builder (no sexo, raza, fechaNacimiento)</li>
 *   <li>Recalcular meses si es necesario (campo calculado)</li>
 *   <li>Persistir animal actualizado en AnimalRepository</li>
 *   <li>Registrar auditoría de actualización en CattleAuditRepository</li>
 *   <li>Retornar AnimalResult con datos actualizados</li>
 * </ol>
 * 
 * <h2>Validaciones de Negocio:</h2>
 * <ul>
 *   <li><b>Estado activo:</b> No se puede modificar animales con status Vendida o Muerta (Requirement 16.1)</li>
 *   <li><b>Arete único:</b> Si se actualiza el arete, debe ser único a nivel global (Requirement 1.2)</li>
 *   <li><b>Genealogía válida:</b> Si se actualizan madre/padre, deben cumplir las mismas 
 *       validaciones que en creación (Requirement 2.5-2.8)</li>
 *   <li><b>Campos inmutables:</b> No se pueden modificar sexo, raza, fechaNacimiento después de la creación</li>
 * </ul>
 * 
 * <h2>Campos Modificables:</h2>
 * <ul>
 *   <li>arete (con validación de unicidad global)</li>
 *   <li>areteAnterior</li>
 *   <li>fechaAretado</li>
 *   <li>tipo (CattleType)</li>
 *   <li>folioReemo</li>
 *   <li>nota</li>
 *   <li>madreId (con validación de genealogía)</li>
 *   <li>padreId (con validación de genealogía)</li>
 * </ul>
 * 
 * <h2>Campos NO Modificables:</h2>
 * <ul>
 *   <li>sexo - Inmutable (definido en creación)</li>
 *   <li>raza - Inmutable (definido en creación)</li>
 *   <li>fechaNacimiento - Inmutable (definido en creación)</li>
 *   <li>status - Requiere caso de uso dedicado (ChangeStatusUseCase)</li>
 *   <li>potrero - Requiere caso de uso dedicado (MoveAnimalUseCase)</li>
 * </ul>
 * 
 * <h2>Atomicidad:</h2>
 * <p>Toda la operación se ejecuta en una transacción {@code @Transactional} para garantizar
 * que si alguna parte falla (validación, persistencia, auditoría), se hace rollback
 * completo y no se deja el sistema en estado inconsistente.</p>
 * 
 * @see UpdateAnimalCommand
 * @see AnimalResult
 * @see Animal
 * @see AnimalRepository
 * @see CattleAuditRepository
 * @see SoldOrDeadAnimalException
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateAnimalUseCase {
    
    private final AnimalRepository animalRepository;
    private final CattleAuditRepository cattleAuditRepository;
    
    /**
     * Ejecuta el caso de uso de actualización de animal.
     * 
     * @param command comando con los datos del animal a actualizar
     * @return AnimalResult con los datos del animal actualizado
     * @throws AnimalNotFoundException si el animal no existe o no pertenece al tenant
     * @throws SoldOrDeadAnimalException si el animal está vendido o muerto (Requirement 16.1)
     * @throws DuplicateAreteException si se actualiza el arete y ya existe en el sistema
     * @throws InvalidGenealogyException si madre o padre no cumplen validaciones
     * @throws IllegalArgumentException si algún parámetro requerido es null o inválido
     */
    @Transactional
    public AnimalResult execute(UpdateAnimalCommand command) {
        log.info("Iniciando actualización de animal: animalId={}, tenantId={}", 
                 command.animalId(), command.tenantId());
        
        // 1. Validar que animal existe y pertenece al tenant (Requirement 16.1)
        Animal existingAnimal = findAnimalOrThrow(command.animalId(), command.tenantId());
        
        // 2. Validar que animal NO está vendido o muerto (Requirement 16.1)
        validateAnimalNotSoldOrDead(existingAnimal);
        
        // 3. Si se actualiza arete, validar unicidad global (Requirement 1.2)
        if (command.arete() != null && !command.arete().equals(existingAnimal.getArete())) {
            validateAreteUnique(command.arete());
        }
        
        // 4. Validar genealogía si se actualizan madre/padre (Requirement 2.5-2.8)
        validateGenealogy(command.madreId(), command.padreId(), existingAnimal.getRanchoId());
        
        // 5. Recalcular meses (campo calculado en tiempo real)
        int mesesActualizados = AgeCalculator.calculateMonths(
            existingAnimal.getFechaNacimiento(), 
            LocalDate.now()
        );
        
        // 6. Construir animal actualizado usando Builder (solo campos modificables)
        Animal updatedAnimal = new Animal.Builder()
            .from(existingAnimal)
            .arete(command.arete() != null ? command.arete() : existingAnimal.getArete())
            .areteAnterior(command.areteAnterior() != null ? command.areteAnterior() : existingAnimal.getAreteAnterior())
            .fechaAretado(command.fechaAretado() != null ? command.fechaAretado() : existingAnimal.getFechaAretado())
            .tipo(command.tipo() != null ? command.tipo() : existingAnimal.getTipo())
            .folioReemo(command.folioReemo() != null ? command.folioReemo() : existingAnimal.getFolioReemo())
            .nota(command.nota() != null ? command.nota() : existingAnimal.getNota())
            .madreId(command.madreId() != null ? command.madreId() : existingAnimal.getMadreId())
            .padreId(command.padreId() != null ? command.padreId() : existingAnimal.getPadreId())
            .meses(mesesActualizados)
            .updatedAt(Instant.now())
            .updatedBy(command.updatedBy())
            .build();
        
        // 7. Persistir animal actualizado (Requirement 16.1)
        Animal savedAnimal = animalRepository.save(updatedAnimal);
        log.debug("Animal actualizado: animalId={}, arete={}", 
                  savedAnimal.getAnimalId(), savedAnimal.getArete());
        
        // 8. Registrar auditoría de actualización (Requirement 14.3)
        java.util.Map<String, Object> oldValues = buildChangedFields(existingAnimal, savedAnimal);
        java.util.Map<String, Object> newValues = buildChangedFields(savedAnimal, existingAnimal);
        
        if (!oldValues.isEmpty()) {
            cattleAuditRepository.logAnimalUpdate(
                savedAnimal.getAnimalId(),
                oldValues,
                newValues,
                command.updatedBy(),
                command.tenantId()
            );
            log.debug("Auditoría de actualización registrada: animalId={}, cambios={}", 
                      savedAnimal.getAnimalId(), oldValues.keySet());
        } else {
            log.debug("Sin cambios para auditar: animalId={}", savedAnimal.getAnimalId());
        }
        
        log.info("Animal actualizado exitosamente: animalId={}, arete={}", 
                 savedAnimal.getAnimalId(), savedAnimal.getArete());
        
        // 9. Retornar resultado
        return AnimalResult.fromDomain(savedAnimal);
    }
    
    /**
     * Busca un animal por ID y valida que pertenece al tenant.
     * 
     * @param animalId UUID del animal
     * @param tenantId UUID del tenant
     * @return Animal encontrado
     * @throws AnimalNotFoundException si no existe o no pertenece al tenant
     */
    private Animal findAnimalOrThrow(java.util.UUID animalId, java.util.UUID tenantId) {
        log.debug("Buscando animal: animalId={}, tenantId={}", animalId, tenantId);
        
        Optional<Animal> animal = animalRepository.findById(animalId);
        
        if (animal.isEmpty()) {
            log.warn("Animal no encontrado: animalId={}", animalId);
            throw new AnimalNotFoundException("Animal no encontrado");
        }
        
        if (!animal.get().getTenantId().equals(tenantId)) {
            log.warn("Animal no pertenece al tenant: animalId={}, tenantId={}, animalTenantId={}", 
                     animalId, tenantId, animal.get().getTenantId());
            throw new AnimalNotFoundException("Animal no encontrado");
        }
        
        return animal.get();
    }
    
    /**
     * Valida que el animal NO está en status Vendida o Muerta.
     * Según Requirement 16.1, no se puede modificar animales vendidos o muertos.
     * 
     * @param animal animal a validar
     * @throws SoldOrDeadAnimalException si el animal está vendido o muerto
     */
    private void validateAnimalNotSoldOrDead(Animal animal) {
        log.debug("Validando que animal no está vendido o muerto: animalId={}, status={}", 
                  animal.getAnimalId(), animal.getStatus());
        
        if (animal.isSoldOrDead()) {
            log.warn("Intento de modificar animal vendido o muerto: animalId={}, status={}", 
                     animal.getAnimalId(), animal.getStatus());
            throw new SoldOrDeadAnimalException("No se puede modificar un animal vendido o muerto");
        }
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
            log.warn("Intento de actualizar con arete duplicado: {}", arete);
            throw new DuplicateAreteException("Arete ya registrado en el sistema");
        }
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
    
    /**
     * Construye un mapa con los campos modificados del animal para auditoría.
     * Retorna los valores del animal "from" para los campos que difieren del animal "to".
     * 
     * @param from animal de referencia (cuyos valores se retornarán)
     * @param to animal de comparación
     * @return mapa con los campos de "from" que difieren de "to"
     */
    private java.util.Map<String, Object> buildChangedFields(Animal from, Animal to) {
        java.util.Map<String, Object> values = new java.util.HashMap<>();
        
        // Comparar arete
        if (!from.getArete().equals(to.getArete())) {
            values.put("arete", from.getArete());
        }
        
        // Comparar areteAnterior
        if (!java.util.Objects.equals(from.getAreteAnterior(), to.getAreteAnterior())) {
            values.put("areteAnterior", from.getAreteAnterior());
        }
        
        // Comparar fechaAretado
        if (!java.util.Objects.equals(from.getFechaAretado(), to.getFechaAretado())) {
            values.put("fechaAretado", from.getFechaAretado() != null ? from.getFechaAretado().toString() : null);
        }
        
        // Comparar tipo
        if (!from.getTipo().equals(to.getTipo())) {
            values.put("tipo", from.getTipo().getValue());
        }
        
        // Comparar folioReemo
        if (!java.util.Objects.equals(from.getFolioReemo(), to.getFolioReemo())) {
            values.put("folioReemo", from.getFolioReemo());
        }
        
        // Comparar nota
        if (!java.util.Objects.equals(from.getNota(), to.getNota())) {
            values.put("nota", from.getNota());
        }
        
        // Comparar madreId
        if (!java.util.Objects.equals(from.getMadreId(), to.getMadreId())) {
            values.put("madreId", from.getMadreId() != null ? from.getMadreId().toString() : null);
        }
        
        // Comparar padreId
        if (!java.util.Objects.equals(from.getPadreId(), to.getPadreId())) {
            values.put("padreId", from.getPadreId() != null ? from.getPadreId().toString() : null);
        }
        
        return values;
    }
}
