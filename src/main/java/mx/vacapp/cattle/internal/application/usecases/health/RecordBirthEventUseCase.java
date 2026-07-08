package mx.vacapp.cattle.internal.application.usecases.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.application.usecases.animal.ChangeStatusUseCase;
import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;
import mx.vacapp.cattle.internal.application.usecases.commands.BirthEventResult;
import mx.vacapp.cattle.internal.application.usecases.commands.ChangeStatusCommand;
import mx.vacapp.cattle.internal.application.usecases.commands.HealthEventResult;
import mx.vacapp.cattle.internal.application.usecases.commands.RecordBirthCommand;
import mx.vacapp.cattle.internal.domain.model.AgeCalculator;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.CattleStatus;
import mx.vacapp.cattle.internal.domain.model.CattleType;
import mx.vacapp.cattle.internal.domain.model.HealthEvent;
import mx.vacapp.cattle.internal.domain.model.HealthEventType;
import mx.vacapp.cattle.internal.domain.model.PastureHistory;
import mx.vacapp.cattle.internal.domain.model.Sex;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.model.exceptions.DuplicateAreteException;
import mx.vacapp.cattle.internal.domain.model.exceptions.InvalidGenealogyException;
import mx.vacapp.cattle.internal.domain.model.exceptions.SoldOrDeadAnimalException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.HealthEventRepository;
import mx.vacapp.cattle.internal.domain.repository.PastureHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso especial para registrar un evento de nacimiento (parto).
 * 
 * <p>Este caso de uso es único porque realiza múltiples operaciones en una transacción:</p>
 * <ol>
 *   <li>Valida que la madre existe y es HEMBRA</li>
 *   <li>Valida que la madre está activa (no Vendida/Muerta)</li>
 *   <li>Valida que el arete de la cría es único</li>
 *   <li>Crea automáticamente un nuevo animal (la cría)</li>
 *   <li>Asigna la cría al mismo potrero que la madre</li>
 *   <li>Si la madre está PRENADA, cambia su status a ACTIVA</li>
 *   <li>Registra el evento de salud BIRTH en la madre</li>
 * </ol>
 * 
 * <h2>Creación Automática de la Cría:</h2>
 * <p>La cría se crea con los siguientes datos:</p>
 * <ul>
 *   <li><b>arete</b>: areteHijo (del comando)</li>
 *   <li><b>sexo</b>: sexo (del comando)</li>
 *   <li><b>raza</b>: heredada de la madre</li>
 *   <li><b>fechaNacimiento</b>: fechaNacimiento (del comando)</li>
 *   <li><b>tipo</b>: CRIA</li>
 *   <li><b>status</b>: ACTIVA</li>
 *   <li><b>madreId</b>: madreId (del comando)</li>
 *   <li><b>padreId</b>: padreId (opcional del comando)</li>
 *   <li><b>ranchoId</b>: heredado de la madre</li>
 *   <li><b>tenantId</b>: heredado de la madre</li>
 *   <li><b>fechaAretado</b>: fechaNacimiento (asumiendo que se areta al nacer)</li>
 *   <li><b>potreroActual</b>: mismo potrero que la madre (si tiene)</li>
 * </ul>
 * 
 * <h2>Validaciones de Negocio:</h2>
 * <ul>
 *   <li><b>Madre debe ser hembra:</b> Solo animales con sexo = HEMBRA pueden parir (Requirement 8.4)</li>
 *   <li><b>Madre no vendida/muerta:</b> La madre debe estar activa (Requirement 16.1)</li>
 *   <li><b>Fecha no futura:</b> fechaNacimiento no puede ser posterior a HOY (Requirement 3)</li>
 *   <li><b>Arete único:</b> areteHijo debe ser único a nivel global (Requirement 1.2)</li>
 *   <li><b>Cambio de status madre:</b> Si madre.status = PRENADA, se cambia a ACTIVA (Requirement 8.4)</li>
 * </ul>
 * 
 * <h2>Atomicidad:</h2>
 * <p>Toda la operación se ejecuta en una transacción {@code @Transactional}. Si cualquier parte
 * falla (validación, creación de cría, registro de evento, cambio de status), se hace rollback
 * completo para evitar estados inconsistentes.</p>
 * 
 * @see RecordBirthCommand
 * @see BirthEventResult
 * @see HealthEvent
 * @see Animal
 * @see AnimalRepository
 * @see HealthEventRepository
 * @see PastureHistoryRepository
 * @see ChangeStatusUseCase
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordBirthEventUseCase {
    
    private final AnimalRepository animalRepository;
    private final HealthEventRepository healthEventRepository;
    private final PastureHistoryRepository pastureHistoryRepository;
    private final ChangeStatusUseCase changeStatusUseCase;
    
    /**
     * Ejecuta el caso de uso de registro de nacimiento.
     * 
     * @param command comando con datos del nacimiento
     * @return BirthEventResult con el evento de salud y los datos de la cría creada
     * @throws AnimalNotFoundException si la madre no existe
     * @throws InvalidGenealogyException si la madre no es HEMBRA
     * @throws SoldOrDeadAnimalException si la madre está Vendida/Muerta
     * @throws DuplicateAreteException si el areteHijo ya existe
     * @throws IllegalArgumentException si fechaNacimiento es futura o algún parámetro es inválido
     */
    @Transactional
    public BirthEventResult execute(RecordBirthCommand command) {
        log.info("Iniciando registro de nacimiento: madreId={}, areteHijo={}, fechaNacimiento={}", 
                 command.madreId(), command.areteHijo(), command.fechaNacimiento());
        
        // 1. Validar que madre existe y es HEMBRA
        Animal madre = findAndValidateMother(command.madreId());
        
        // 2. Validar que madre está activa (no Vendida/Muerta)
        validateMotherIsActive(madre);
        
        // 3. Validar que fechaNacimiento no es futura
        validateBirthDateNotFuture(command.fechaNacimiento());
        
        // 4. Validar que areteHijo es único
        validateAreteIsUnique(command.areteHijo());
        
        // 5. Obtener potrero actual de la madre (si tiene)
        UUID potreroActual = getCurrentPastureOfMother(command.madreId());
        
        // 6. Crear nueva cría con datos proporcionados + heredados de la madre
        Animal offspring = createOffspring(command, madre, potreroActual);
        
        // 7. Persistir la cría
        Animal savedOffspring = animalRepository.save(offspring);
        log.debug("Cría creada exitosamente: animalId={}, arete={}", 
                  savedOffspring.getAnimalId(), savedOffspring.getArete());
        
        // 8. Si madre tiene potrero actual, crear PastureHistory para la cría en el mismo potrero
        if (potreroActual != null) {
            createPastureHistoryForOffspring(savedOffspring.getAnimalId(), potreroActual, 
                                             command.fechaNacimiento(), command.recordedBy());
        }
        
        // 9. Si madre.status = PRENADA, cambiar a ACTIVA
        if (madre.getStatus() == CattleStatus.PRENADA) {
            changeMotherStatusToActive(madre, command.recordedBy());
        }
        
        // 10. Registrar evento de salud BIRTH para la madre
        HealthEvent birthEvent = createBirthEvent(command, savedOffspring.getAnimalId());
        HealthEvent savedBirthEvent = healthEventRepository.save(birthEvent);
        log.debug("Evento de nacimiento registrado: eventId={}, madreId={}, criaId={}", 
                  savedBirthEvent.getEventId(), command.madreId(), savedOffspring.getAnimalId());
        
        log.info("Nacimiento registrado exitosamente: madreId={}, criaId={}, arete={}", 
                 command.madreId(), savedOffspring.getAnimalId(), savedOffspring.getArete());
        
        // 11. Retornar resultado con evento de salud y datos de la cría
        return new BirthEventResult(
            HealthEventResult.fromDomain(savedBirthEvent),
            AnimalResult.fromDomain(savedOffspring)
        );
    }
    
    /**
     * Busca la madre por ID y valida que sea HEMBRA.
     * 
     * @param madreId UUID de la madre
     * @return Animal madre validada
     * @throws AnimalNotFoundException si la madre no existe
     * @throws InvalidGenealogyException si la madre no es HEMBRA
     */
    private Animal findAndValidateMother(UUID madreId) {
        log.debug("Buscando y validando madre: madreId={}", madreId);
        
        Animal madre = animalRepository.findById(madreId)
            .orElseThrow(() -> {
                log.warn("Madre no encontrada: madreId={}", madreId);
                return new AnimalNotFoundException("Madre no encontrada");
            });
        
        // Validar que es HEMBRA
        if (madre.getSexo() != Sex.HEMBRA) {
            log.warn("Intento de registrar parto en animal no hembra: madreId={}, sexo={}", 
                     madreId, madre.getSexo());
            throw new InvalidGenealogyException("Solo hembras pueden parir");
        }
        
        log.debug("Madre validada: madreId={}, arete={}, sexo={}", 
                  madre.getAnimalId(), madre.getArete(), madre.getSexo());
        return madre;
    }
    
    /**
     * Valida que la madre está activa (no Vendida/Muerta).
     * 
     * @param madre animal madre a validar
     * @throws SoldOrDeadAnimalException si la madre está Vendida/Muerta
     */
    private void validateMotherIsActive(Animal madre) {
        if (madre.isSoldOrDead()) {
            log.warn("Intento de registrar parto en animal vendida o muerta: madreId={}, status={}", 
                     madre.getAnimalId(), madre.getStatus());
            throw new SoldOrDeadAnimalException(
                "No se puede registrar parto en animal vendida o muerta"
            );
        }
    }
    
    /**
     * Valida que la fecha de nacimiento no es futura.
     * 
     * @param fechaNacimiento fecha de nacimiento a validar
     * @throws IllegalArgumentException si la fecha es futura
     */
    private void validateBirthDateNotFuture(LocalDate fechaNacimiento) {
        if (fechaNacimiento.isAfter(LocalDate.now())) {
            log.warn("Intento de registrar nacimiento con fecha futura: fechaNacimiento={}", 
                     fechaNacimiento);
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
    }
    
    /**
     * Valida que el arete de la cría es único a nivel global.
     * 
     * @param areteHijo arete de la cría
     * @throws DuplicateAreteException si el arete ya existe
     */
    private void validateAreteIsUnique(String areteHijo) {
        log.debug("Validando unicidad de arete: areteHijo={}", areteHijo);
        
        if (animalRepository.existsByArete(areteHijo)) {
            log.warn("Intento de crear cría con arete duplicado: areteHijo={}", areteHijo);
            throw new DuplicateAreteException("El arete ya está registrado en el sistema");
        }
    }
    
    /**
     * Obtiene el potrero actual de la madre (si tiene).
     * 
     * @param madreId UUID de la madre
     * @return UUID del potrero actual o null si no tiene ubicación
     */
    private UUID getCurrentPastureOfMother(UUID madreId) {
        log.debug("Obteniendo potrero actual de la madre: madreId={}", madreId);
        
        Optional<PastureHistory> currentPasture = pastureHistoryRepository.findCurrentByAnimalId(madreId);
        
        if (currentPasture.isPresent()) {
            UUID potreroId = currentPasture.get().getPotreroId();
            log.debug("Madre tiene potrero actual: madreId={}, potreroId={}", madreId, potreroId);
            return potreroId;
        } else {
            log.debug("Madre no tiene potrero actual: madreId={}", madreId);
            return null;
        }
    }
    
    /**
     * Crea el animal cría con datos del comando y heredados de la madre.
     * 
     * @param command comando con datos de la cría
     * @param madre animal madre
     * @param potreroActual potrero actual de la madre (puede ser null)
     * @return Animal cría creado
     */
    private Animal createOffspring(RecordBirthCommand command, Animal madre, UUID potreroActual) {
        log.debug("Creando cría: areteHijo={}, sexo={}, madre={}", 
                  command.areteHijo(), command.sexo(), madre.getArete());
        
        Instant now = Instant.now();
        int meses = AgeCalculator.calculateMonths(command.fechaNacimiento(), LocalDate.now());
        
        // Crear cría usando el Builder para tener control completo
        Animal offspring = new Animal.Builder()
            .animalId(UUID.randomUUID())
            .arete(command.areteHijo().toUpperCase())  // Normalizar a mayúsculas
            .sexo(command.sexo())
            .raza(madre.getRaza())  // Heredar raza de la madre
            .fechaNacimiento(command.fechaNacimiento())
            .meses(meses)
            .fechaAretado(command.fechaNacimiento())  // Asumiendo que se areta al nacer
            .tipo(CattleType.CRIA)  // Tipo fijo para crías
            .status(CattleStatus.ACTIVA)  // Status inicial
            .madreId(command.madreId())
            .padreId(command.padreId())  // Opcional
            .ranchoId(madre.getRanchoId())  // Heredar rancho de la madre
            .tenantId(madre.getTenantId())  // Heredar tenant de la madre
            .createdAt(now)
            .updatedAt(now)
            .createdBy(command.recordedBy())
            .updatedBy(command.recordedBy())
            .build();
        
        log.debug("Cría creada con datos heredados: arete={}, raza={}, ranchoId={}, tenantId={}", 
                  offspring.getArete(), offspring.getRaza(), offspring.getRanchoId(), offspring.getTenantId());
        
        return offspring;
    }
    
    /**
     * Crea registro de historial de potrero para la cría.
     * 
     * @param offspringId UUID de la cría
     * @param potreroId UUID del potrero
     * @param fechaNacimiento fecha de nacimiento (entrada al potrero)
     * @param createdBy usuario que registra
     */
    private void createPastureHistoryForOffspring(UUID offspringId, UUID potreroId, 
                                                   LocalDate fechaNacimiento, UUID createdBy) {
        log.debug("Creando historial de potrero para cría: offspringId={}, potreroId={}", 
                  offspringId, potreroId);
        
        // Convertir LocalDate a Instant (inicio del día)
        Instant fechaEntrada = fechaNacimiento.atStartOfDay(
            java.time.ZoneId.systemDefault()
        ).toInstant();
        
        PastureHistory history = new PastureHistory.Builder()
            .historyId(UUID.randomUUID())
            .animalId(offspringId)
            .potreroId(potreroId)
            .fechaEntrada(fechaEntrada)
            .fechaSalida(null)  // Aún está en el potrero
            .createdBy(createdBy)
            .build();
        
        pastureHistoryRepository.insert(history);
        log.debug("Historial de potrero creado para cría: historyId={}", history.getHistoryId());
    }
    
    /**
     * Cambia el status de la madre de PRENADA a ACTIVA.
     * 
     * @param madre animal madre
     * @param recordedBy usuario que registra el cambio
     */
    private void changeMotherStatusToActive(Animal madre, UUID recordedBy) {
        log.debug("Cambiando status de madre de PRENADA a ACTIVA: madreId={}", madre.getAnimalId());
        
        ChangeStatusCommand statusCommand = new ChangeStatusCommand(
            madre.getAnimalId(),
            CattleStatus.ACTIVA,
            null,  // fechaVenta
            null,  // precioVenta
            null,  // fechaMuerte
            null,  // motivoMuerte
            "Cambio automático tras registrar parto",  // reason
            recordedBy,
            madre.getTenantId()
        );
        
        changeStatusUseCase.execute(statusCommand);
        log.debug("Status de madre actualizado a ACTIVA: madreId={}", madre.getAnimalId());
    }
    
    /**
     * Crea el evento de salud BIRTH para la madre.
     * 
     * @param command comando con datos del nacimiento
     * @param offspringId UUID de la cría creada
     * @return HealthEvent de tipo BIRTH
     */
    private HealthEvent createBirthEvent(RecordBirthCommand command, UUID offspringId) {
        log.debug("Creando evento de salud BIRTH: madreId={}, criaId={}", 
                  command.madreId(), offspringId);
        
        // Construir descripción del evento incluyendo datos de la cría
        String descripcion = String.format(
            "Nacimiento de cría. Arete: %s, Sexo: %s, Cría ID: %s",
            command.areteHijo(),
            command.sexo().getValue(),
            offspringId
        );
        
        // Si hay observaciones, añadirlas a la descripción
        if (command.observaciones() != null && !command.observaciones().trim().isEmpty()) {
            descripcion = descripcion + ". Observaciones: " + command.observaciones();
        }
        
        // Crear evento usando el Builder para incluir todos los campos opcionales
        return new HealthEvent.Builder()
            .eventId(UUID.randomUUID())
            .animalId(command.madreId())
            .tipoEvento(HealthEventType.Birth)
            .fecha(command.fechaNacimiento())
            .descripcion(descripcion)
            .observaciones(command.observaciones())
            .recordedAt(java.time.LocalDateTime.now())
            .recordedBy(command.recordedBy())
            .build();
    }
}
