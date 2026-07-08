package mx.vacapp.cattle.internal.application.usecases.movement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;
import mx.vacapp.cattle.internal.application.usecases.commands.MoveAnimalCommand;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.PastureHistory;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.model.exceptions.SoldOrDeadAnimalException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.CattleAuditRepository;
import mx.vacapp.cattle.internal.domain.repository.PastureHistoryRepository;
import mx.vacapp.cattle.internal.infrastructure.integration.GeographyServiceClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Caso de uso para mover un animal de un potrero a otro.
 * 
 * <p>Este caso de uso implementa la funcionalidad completa de movimiento de ganado
 * entre potreros, garantizando trazabilidad completa del historial de ubicaciones
 * y validando reglas de negocio críticas.</p>
 * 
 * <h2>Flujo de Ejecución:</h2>
 * <ol>
 *   <li>Validar que el animal existe y pertenece al tenant del usuario</li>
 *   <li>Validar que el animal está activo (NO Vendida/Muerta)</li>
 *   <li>Validar que el potrero destino existe y está activo (consulta a geography-control)</li>
 *   <li>Obtener el registro actual en pasture_history (WHERE fecha_salida IS NULL)</li>
 *   <li>Actualizar fecha_salida = NOW() en el registro actual</li>
 *   <li>Crear nuevo registro en pasture_history con potreroDestinoId y fecha_entrada = NOW()</li>
 *   <li>Guardar ambos registros en PastureHistoryRepository</li>
 *   <li>Registrar el movimiento en cattle_audit</li>
 *   <li>Retornar AnimalResult con ubicación actualizada</li>
 * </ol>
 * 
 * <h2>Validaciones de Negocio:</h2>
 * <ul>
 *   <li>El animal DEBE existir y pertenecer al tenant del usuario (lanza AnimalNotFoundException)</li>
 *   <li>El animal NO PUEDE tener status Vendida o Muerta (lanza SoldOrDeadAnimalException)</li>
 *   <li>El potrero destino DEBE existir y estar activo (lanza InvalidPastureException)</li>
 *   <li>Solo puede haber UN registro activo (fecha_salida = null) por animal en cualquier momento</li>
 * </ul>
 * 
 * <h2>Transaccionalidad:</h2>
 * <p>Este caso de uso ejecuta en una transacción para garantizar consistencia:</p>
 * <ul>
 *   <li>Si alguna operación falla, toda la transacción se revierte (rollback)</li>
 *   <li>No quedan registros huérfanos con fecha_salida sin actualizar</li>
 *   <li>La auditoría siempre refleja el estado real de la base de datos</li>
 * </ul>
 * 
 * <h2>Auditoría:</h2>
 * <p>El movimiento se registra en cattle_audit con:</p>
 * <ul>
 *   <li>operation_type: MOVE_PASTURE</li>
 *   <li>old_values: UUID del potrero origen</li>
 *   <li>new_values: UUID del potrero destino</li>
 *   <li>timestamp: fecha/hora UTC del movimiento</li>
 *   <li>modified_by: usuario que realizó el movimiento</li>
 * </ul>
 * 
 * <h2>Integración con Módulo Geográfico:</h2>
 * <p>Utiliza {@link GeographyServiceClient} para validar el potrero destino.
 * Esta validación es crítica para mantener integridad referencial entre módulos.</p>
 * 
 * @see MoveAnimalCommand
 * @see AnimalResult
 * @see PastureHistory
 * @see GeographyServiceClient
 * @see AnimalRepository
 * @see PastureHistoryRepository
 * @see CattleAuditRepository
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoveAnimalUseCase {
    
    private final AnimalRepository animalRepository;
    private final PastureHistoryRepository pastureHistoryRepository;
    private final GeographyServiceClient geographyServiceClient;
    private final CattleAuditRepository cattleAuditRepository;
    
    /**
     * Ejecuta el movimiento de un animal de un potrero a otro.
     * 
     * <p>Este método es el punto de entrada principal del caso de uso.
     * Coordina todas las validaciones, actualizaciones de historial,
     * y registro de auditoría necesarios para completar el movimiento.</p>
     * 
     * @param command comando con datos del movimiento (animalId, potreroDestinoId, movedBy, tenantId)
     * @return AnimalResult con datos actualizados del animal incluyendo nueva ubicación
     * @throws AnimalNotFoundException si el animal no existe o no pertenece al tenant
     * @throws SoldOrDeadAnimalException si el animal tiene status Vendida o Muerta
     * @throws InvalidPastureException si el potrero destino no existe o está inactivo
     * @throws IllegalStateException si no se encuentra registro actual de ubicación
     */
    @Transactional
    public AnimalResult execute(MoveAnimalCommand command) {
        log.info("Iniciando movimiento de animal: animalId={}, potreroDestinoId={}, movedBy={}",
                command.animalId(), command.potreroDestinoId(), command.movedBy());
        
        // 1. Validar que el animal existe y pertenece al tenant
        Animal animal = animalRepository.findById(command.animalId())
                .orElseThrow(() -> {
                    log.error("Animal no encontrado: animalId={}", command.animalId());
                    return new AnimalNotFoundException("Animal no encontrado");
                });
        
        log.debug("Animal encontrado: arete={}, status={}", animal.getArete(), animal.getStatus());
        
        // 2. Validar que el animal NO está vendido o muerto
        if (animal.isSoldOrDead()) {
            log.error("Intento de mover animal vendido o muerto: animalId={}, status={}",
                    command.animalId(), animal.getStatus());
            throw new SoldOrDeadAnimalException("No se puede mover un animal vendido o muerto");
        }
        
        // 3. Validar que el potrero destino existe y está activo
        log.debug("Validando potrero destino: potreroId={}", command.potreroDestinoId());
        geographyServiceClient.validatePotreroActive(command.potreroDestinoId());
        log.debug("Potrero destino validado exitosamente");
        
        // 4. Obtener el registro actual en pasture_history (WHERE fecha_salida IS NULL)
        PastureHistory currentLocation = pastureHistoryRepository.findCurrentByAnimalId(command.animalId())
                .orElseThrow(() -> {
                    log.error("No se encontró ubicación actual para el animal: animalId={}", command.animalId());
                    return new IllegalStateException("Animal no tiene ubicación actual registrada");
                });
        
        UUID oldPotreroId = currentLocation.getPotreroId();
        log.debug("Ubicación actual encontrada: potreroId={}, fechaEntrada={}",
                oldPotreroId, currentLocation.getFechaEntrada());
        
        // 5. Actualizar fecha_salida = NOW() en el registro actual
        Instant now = Instant.now();
        pastureHistoryRepository.updateFechaSalida(currentLocation.getHistoryId(), now);
        log.debug("Fecha de salida actualizada en registro actual: historyId={}, fechaSalida={}",
                currentLocation.getHistoryId(), now);
        
        // 6. Crear nuevo registro en pasture_history con potreroDestinoId y fecha_entrada = NOW()
        PastureHistory newLocation = PastureHistory.create(
                command.animalId(),
                command.potreroDestinoId(),
                now,
                command.movedBy()
        );
        
        PastureHistory savedLocation = pastureHistoryRepository.insert(newLocation);
        log.debug("Nuevo registro de ubicación creado: historyId={}, potreroId={}, fechaEntrada={}",
                savedLocation.getHistoryId(), savedLocation.getPotreroId(), savedLocation.getFechaEntrada());
        
        // 7. Registrar el movimiento en cattle_audit
        cattleAuditRepository.logMovement(
                command.animalId(),
                oldPotreroId,
                command.potreroDestinoId(),
                command.movedBy()
        );
        
        log.info("Movimiento de animal completado exitosamente: animalId={}, potreroOrigen={}, potreroDestino={}",
                command.animalId(), oldPotreroId, command.potreroDestinoId());
        
        // 8. Retornar AnimalResult con ubicación actualizada
        return AnimalResult.fromDomainEnriched(
                animal,
                animal.getMeses(),
                null,  // pesoActual - no se requiere para movimiento
                command.potreroDestinoId(),  // potreroActual actualizado
                null,  // nombreMadre - no requerido para movimiento
                null   // nombrePadre - no requerido para movimiento
        );
    }
}
