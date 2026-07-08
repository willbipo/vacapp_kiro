package mx.vacapp.cattle.internal.application.usecases.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.application.usecases.commands.HealthEventResult;
import mx.vacapp.cattle.internal.application.usecases.commands.RecordHealthEventCommand;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.HealthEvent;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.HealthEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Caso de uso para registrar un evento de salud genérico de un animal.
 * 
 * <p>Este caso de uso implementa el flujo completo de registro de eventos de salud
 * para todos los tipos soportados: Vacunacion, Desparasitacion, Tratamiento,
 * Diagnostico, Cirugia, y Revision. Incluye todas las validaciones de negocio,
 * creación de la entidad de dominio, y persistencia del evento.</p>
 * 
 * <h2>Flujo de Ejecución:</h2>
 * <ol>
 *   <li>Validar que el animal existe y pertenece al tenant del usuario</li>
 *   <li>Lanzar AnimalNotFoundException si el animal no existe o no pertenece al tenant</li>
 *   <li>Validar que la fecha del evento no sea futura</li>
 *   <li>Validar que el costo sea >= 0 si está presente</li>
 *   <li>Crear entidad HealthEvent usando HealthEvent.create()</li>
 *   <li>Aplicar campos opcionales usando el Builder (medicamento, dosis, veterinario, etc.)</li>
 *   <li>Persistir evento en HealthEventRepository</li>
 *   <li>Retornar HealthEventResult con datos del evento creado</li>
 * </ol>
 * 
 * <h2>Validaciones de Negocio:</h2>
 * <ul>
 *   <li><b>Animal existe:</b> El animal debe existir y pertenecer al tenant del usuario (Requirement 8.8)</li>
 *   <li><b>Fecha no futura:</b> La fecha del evento no puede ser posterior a la fecha actual (validación en HealthEvent.create())</li>
 *   <li><b>Costo no negativo:</b> Si se proporciona costo, debe ser >= 0 (Requirement 8.1)</li>
 * </ul>
 * 
 * <h2>Tipos de Eventos Soportados:</h2>
 * <ul>
 *   <li><b>Vacunacion:</b> Vacunación del animal (preventivo)</li>
 *   <li><b>Desparasitacion:</b> Desparasitación del animal</li>
 *   <li><b>Tratamiento:</b> Tratamiento médico (curativo)</li>
 *   <li><b>Diagnostico:</b> Diagnóstico veterinario (evaluación de salud)</li>
 *   <li><b>Cirugia:</b> Intervención quirúrgica</li>
 *   <li><b>Revision:</b> Revisión general de salud</li>
 * </ul>
 * 
 * <h2>Atomicidad:</h2>
 * <p>Toda la operación se ejecuta en una transacción {@code @Transactional} para garantizar
 * que si alguna parte falla, se hace rollback completo y no se deja el sistema en estado
 * inconsistente.</p>
 * 
 * <h2>Campos Opcionales:</h2>
 * <p>Los siguientes campos son opcionales y pueden ser null:</p>
 * <ul>
 *   <li>medicamento: Nombre del medicamento utilizado</li>
 *   <li>dosis: Dosis del medicamento aplicado</li>
 *   <li>veterinario: Nombre del veterinario que atendió el evento</li>
 *   <li>proximaFecha: Fecha programada para próximo evento relacionado (seguimiento, refuerzo)</li>
 *   <li>costo: Costo económico del evento</li>
 *   <li>observaciones: Observaciones adicionales sobre el evento</li>
 * </ul>
 * 
 * @see RecordHealthEventCommand
 * @see HealthEventResult
 * @see HealthEvent
 * @see HealthEventRepository
 * @see AnimalRepository
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordHealthEventUseCase {
    
    private final AnimalRepository animalRepository;
    private final HealthEventRepository healthEventRepository;
    
    /**
     * Ejecuta el caso de uso de registro de evento de salud.
     * 
     * @param command comando con todos los datos del evento de salud a registrar
     * @return HealthEventResult con los datos del evento creado
     * @throws AnimalNotFoundException si el animal no existe o no pertenece al tenant del usuario
     * @throws IllegalArgumentException si la fecha es futura o el costo es negativo
     */
    @Transactional
    public HealthEventResult execute(RecordHealthEventCommand command) {
        log.info("Iniciando registro de evento de salud: animalId={}, tipoEvento={}, fecha={}", 
                 command.animalId(), command.tipoEvento(), command.fecha());
        
        // 1. Validar que el animal existe y pertenece al tenant (Requirement 8.8)
        validateAnimalExists(command.animalId());
        
        // 2. Validar que la fecha no sea futura (se valida en HealthEvent.create())
        // 3. Validar que el costo sea >= 0 si está presente
        validateCosto(command.costo());
        
        // 4. Crear entidad de dominio HealthEvent
        // HealthEvent.create() valida que fecha <= LocalDate.now()
        HealthEvent healthEvent = HealthEvent.create(
            command.animalId(),
            command.tipoEvento(),
            command.fecha(),
            command.descripcion(),
            command.recordedBy()
        );
        
        // 5. Aplicar campos opcionales usando el Builder
        healthEvent = new HealthEvent.Builder()
            .eventId(healthEvent.getEventId())
            .animalId(healthEvent.getAnimalId())
            .tipoEvento(healthEvent.getTipoEvento())
            .fecha(healthEvent.getFecha())
            .descripcion(healthEvent.getDescripcion())
            .medicamento(command.medicamento())
            .dosis(command.dosis())
            .veterinario(command.veterinario())
            .proximaFecha(command.proximaFecha())
            .costo(command.costo())
            .observaciones(command.observaciones())
            .recordedAt(healthEvent.getRecordedAt())
            .recordedBy(healthEvent.getRecordedBy())
            .build();
        
        // 6. Persistir evento en repositorio (Requirement 8.1)
        HealthEvent savedEvent = healthEventRepository.save(healthEvent);
        log.debug("Evento de salud persistido: eventId={}, animalId={}, tipoEvento={}", 
                  savedEvent.getEventId(), savedEvent.getAnimalId(), savedEvent.getTipoEvento());
        
        log.info("Evento de salud registrado exitosamente: eventId={}, animalId={}, tipoEvento={}", 
                 savedEvent.getEventId(), savedEvent.getAnimalId(), savedEvent.getTipoEvento());
        
        // 7. Retornar resultado
        return HealthEventResult.fromDomain(savedEvent);
    }
    
    /**
     * Valida que el animal existe y pertenece al tenant del usuario actual.
     * 
     * <p>La validación de tenant se realiza automáticamente en el repositorio
     * mediante filtrado por tenant_id del contexto de seguridad.</p>
     * 
     * @param animalId UUID del animal a validar
     * @return el animal si existe y pertenece al tenant
     * @throws AnimalNotFoundException si el animal no existe o no pertenece al tenant
     */
    private Animal validateAnimalExists(java.util.UUID animalId) {
        log.debug("Validando existencia de animal: animalId={}", animalId);
        
        return animalRepository.findById(animalId)
            .orElseThrow(() -> {
                log.warn("Animal no encontrado o no pertenece al tenant: animalId={}", animalId);
                return new AnimalNotFoundException("Animal no encontrado");
            });
    }
    
    /**
     * Valida que el costo sea mayor o igual a cero si está presente.
     * 
     * @param costo costo del evento (puede ser null)
     * @throws IllegalArgumentException si el costo es negativo
     */
    private void validateCosto(BigDecimal costo) {
        if (costo != null && costo.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Intento de registrar evento con costo negativo: costo={}", costo);
            throw new IllegalArgumentException("El costo debe ser mayor o igual a cero");
        }
    }
}
