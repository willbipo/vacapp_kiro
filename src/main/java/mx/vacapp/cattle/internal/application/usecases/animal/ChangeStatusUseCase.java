package mx.vacapp.cattle.internal.application.usecases.animal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;
import mx.vacapp.cattle.internal.application.usecases.commands.ChangeStatusCommand;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.CattleStatus;
import mx.vacapp.cattle.internal.domain.model.PastureHistory;
import mx.vacapp.cattle.internal.domain.model.Sex;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.model.exceptions.CattleDomainException;
import mx.vacapp.cattle.internal.domain.model.exceptions.SoldOrDeadAnimalException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.CattleAuditRepository;
import mx.vacapp.cattle.internal.domain.repository.PastureHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Caso de uso para cambiar el status de un animal en el sistema de inventario de ganado.
 * 
 * <p>Este caso de uso implementa el flujo completo de cambio de estado de un animal,
 * incluyendo validaciones específicas por status, actualización de la entidad,
 * gestión de ubicación en potrero para estados finales (Vendida/Muerta), y auditoría.</p>
 * 
 * <h2>Estados Soportados:</h2>
 * <ul>
 *   <li><b>ACTIVA</b>: Animal activo en el rancho (sin validaciones especiales)</li>
 *   <li><b>VENDIDA</b>: Animal vendido (requiere fechaVenta, precioVenta opcional)</li>
 *   <li><b>MUERTA</b>: Animal muerto (requiere fechaMuerte, motivoMuerte opcional)</li>
 *   <li><b>PRESTADA</b>: Animal prestado temporalmente (sin validaciones especiales)</li>
 *   <li><b>PRENADA</b>: Animal preñado - SOLO HEMBRAS (requiere sexo = HEMBRA)</li>
 *   <li><b>EN_REPOSO</b>: Animal en descanso reproductivo (sin validaciones especiales)</li>
 * </ul>
 * 
 * <h2>Flujo de Ejecución:</h2>
 * <ol>
 *   <li>Buscar el animal por ID y validar existencia</li>
 *   <li>Validar que el animal pertenece al tenant del comando</li>
 *   <li>Validar que el animal NO está ya Vendido o Muerto</li>
 *   <li>Ejecutar validaciones específicas según el nuevo status:
 *     <ul>
 *       <li>PRENADA → validar que sexo = HEMBRA</li>
 *       <li>VENDIDA → validar que fechaVenta sea proporcionada y precioVenta > 0</li>
 *       <li>MUERTA → validar que fechaMuerte sea proporcionada</li>
 *     </ul>
 *   </li>
 *   <li>Aplicar el cambio de status usando el método correspondiente de Animal</li>
 *   <li>Si el nuevo status es VENDIDA o MUERTA, actualizar pasture_history
 *       con fecha_salida = NOW (sacar del potrero actual)</li>
 *   <li>Persistir animal actualizado</li>
 *   <li>Registrar auditoría del cambio de status</li>
 *   <li>Retornar AnimalResult con datos actualizados</li>
 * </ol>
 * 
 * <h2>Validaciones de Negocio:</h2>
 * <ul>
 *   <li><b>No modificar vendidos/muertos:</b> Un animal con status VENDIDA o MUERTA
 *       no puede cambiar a ningún otro status (Requirement 4.4, 16.1)</li>
 *   <li><b>Preñada solo hembras:</b> El status PRENADA solo puede aplicarse a animales
 *       con sexo = HEMBRA (Requirement 4.5)</li>
 *   <li><b>Venta con fecha:</b> Cambio a VENDIDA requiere fechaVenta obligatoria.
 *       Si se proporciona precioVenta, debe ser > 0 (Requirement 4.3)</li>
 *   <li><b>Muerte con fecha:</b> Cambio a MUERTA requiere fechaMuerte obligatoria.
 *       motivoMuerte es opcional (Requirement 4.4)</li>
 *   <li><b>Salida de potrero:</b> Cuando status cambia a VENDIDA o MUERTA,
 *       se actualiza pasture_history con fecha_salida = NOW (Requirement 4.6)</li>
 * </ul>
 * 
 * <h2>Atomicidad:</h2>
 * <p>Toda la operación se ejecuta en una transacción {@code @Transactional} para garantizar
 * que si alguna parte falla (actualización animal, historial, auditoría), se hace rollback
 * completo y no se deja el sistema en estado inconsistente.</p>
 * 
 * @see ChangeStatusCommand
 * @see AnimalResult
 * @see Animal
 * @see CattleStatus
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
public class ChangeStatusUseCase {
    
    private final AnimalRepository animalRepository;
    private final PastureHistoryRepository pastureHistoryRepository;
    private final CattleAuditRepository cattleAuditRepository;
    
    /**
     * Ejecuta el caso de uso de cambio de status de animal.
     * 
     * @param command comando con datos del cambio de status
     * @return AnimalResult con los datos del animal actualizado
     * @throws AnimalNotFoundException si el animal no existe
     * @throws SoldOrDeadAnimalException si el animal ya está vendido o muerto
     * @throws CattleDomainException si las validaciones de status no se cumplen
     * @throws IllegalArgumentException si algún parámetro requerido es null o inválido
     */
    @Transactional
    public AnimalResult execute(ChangeStatusCommand command) {
        log.info("Iniciando cambio de status: animalId={}, newStatus={}, tenantId={}", 
                 command.animalId(), command.newStatus(), command.tenantId());
        
        // 1. Buscar el animal y validar existencia
        Animal animal = findAnimalById(command.animalId());
        
        // 2. Validar que el animal pertenece al tenant del comando
        validateAnimalBelongsToTenant(animal, command.tenantId());
        
        // 3. Validar que el animal NO está vendido o muerto (no se puede modificar)
        validateAnimalNotSoldOrDead(animal);
        
        // 4. Almacenar status anterior para auditoría
        CattleStatus oldStatus = animal.getStatus();
        
        // 5. Ejecutar validaciones específicas y aplicar cambio de status
        Animal updatedAnimal = applyStatusChange(animal, command);
        
        // 6. Si el nuevo status es VENDIDA o MUERTA, sacar del potrero actual
        if (command.newStatus() == CattleStatus.VENDIDA || 
            command.newStatus() == CattleStatus.MUERTA) {
            closeCurrentPastureHistory(animal.getAnimalId());
        }
        
        // 7. Persistir animal actualizado
        Animal savedAnimal = animalRepository.save(updatedAnimal);
        log.debug("Animal actualizado: animalId={}, newStatus={}", 
                  savedAnimal.getAnimalId(), savedAnimal.getStatus());
        
        // 8. Registrar auditoría del cambio de status (Requirement 4.10, 14.4)
        cattleAuditRepository.logStatusChange(
            savedAnimal.getAnimalId(),
            oldStatus.getValue(),
            savedAnimal.getStatus().getValue(),
            command.changedBy(),
            command.reason()
        );
        log.debug("Auditoría de cambio de status registrada: animalId={}, oldStatus={}, newStatus={}", 
                  savedAnimal.getAnimalId(), oldStatus, savedAnimal.getStatus());
        
        log.info("Cambio de status completado exitosamente: animalId={}, oldStatus={}, newStatus={}", 
                 savedAnimal.getAnimalId(), oldStatus, savedAnimal.getStatus());
        
        // 9. Retornar resultado
        return AnimalResult.fromDomain(savedAnimal);
    }
    
    /**
     * Busca el animal por ID y lanza excepción si no existe.
     * 
     * @param animalId UUID del animal
     * @return Animal encontrado
     * @throws AnimalNotFoundException si el animal no existe
     */
    private Animal findAnimalById(java.util.UUID animalId) {
        log.debug("Buscando animal: animalId={}", animalId);
        
        return animalRepository.findById(animalId)
            .orElseThrow(() -> {
                log.warn("Animal no encontrado: animalId={}", animalId);
                return new AnimalNotFoundException("Animal no encontrado");
            });
    }
    
    /**
     * Valida que el animal pertenece al tenant del comando.
     * Validación de seguridad multi-tenant.
     * 
     * @param animal animal a validar
     * @param tenantId tenant del comando
     * @throws IllegalArgumentException si el animal no pertenece al tenant
     */
    private void validateAnimalBelongsToTenant(Animal animal, java.util.UUID tenantId) {
        if (!animal.getTenantId().equals(tenantId)) {
            log.warn("Intento de modificar animal de otro tenant: animalId={}, animalTenant={}, commandTenant={}", 
                     animal.getAnimalId(), animal.getTenantId(), tenantId);
            throw new IllegalArgumentException("Acceso denegado");
        }
    }
    
    /**
     * Valida que el animal NO está vendido o muerto.
     * Animales vendidos o muertos no pueden cambiar de status.
     * 
     * @param animal animal a validar
     * @throws SoldOrDeadAnimalException si el animal está vendido o muerto
     */
    private void validateAnimalNotSoldOrDead(Animal animal) {
        if (animal.isSoldOrDead()) {
            log.warn("Intento de cambiar status de animal vendido o muerto: animalId={}, currentStatus={}", 
                     animal.getAnimalId(), animal.getStatus());
            throw new SoldOrDeadAnimalException("No se puede modificar un animal vendido o muerto");
        }
    }
    
    /**
     * Aplica el cambio de status con validaciones específicas según el nuevo estado.
     * 
     * @param animal animal a actualizar
     * @param command comando con datos del cambio
     * @return Animal con status actualizado
     * @throws CattleDomainException si las validaciones específicas fallan
     */
    private Animal applyStatusChange(Animal animal, ChangeStatusCommand command) {
        log.debug("Aplicando cambio de status: animalId={}, currentStatus={}, newStatus={}", 
                  animal.getAnimalId(), animal.getStatus(), command.newStatus());
        
        return switch (command.newStatus()) {
            case PRENADA -> applyPregnantStatus(animal, command);
            case VENDIDA -> applySoldStatus(animal, command);
            case MUERTA -> applyDeadStatus(animal, command);
            case ACTIVA, PRESTADA, EN_REPOSO -> applySimpleStatus(animal, command);
        };
    }
    
    /**
     * Aplica el status PRENADA con validación de sexo.
     * Solo hembras pueden estar preñadas (Requirement 4.5).
     * 
     * @param animal animal a actualizar
     * @param command comando con datos
     * @return Animal con status PRENADA
     * @throws CattleDomainException si el animal no es hembra
     */
    private Animal applyPregnantStatus(Animal animal, ChangeStatusCommand command) {
        log.debug("Aplicando status PRENADA: animalId={}, sexo={}", animal.getAnimalId(), animal.getSexo());
        
        if (animal.getSexo() != Sex.HEMBRA) {
            log.warn("Intento de marcar macho como preñado: animalId={}, sexo={}", 
                     animal.getAnimalId(), animal.getSexo());
            throw new CattleDomainException("Solo hembras pueden estar preñadas");
        }
        
        return animal.markAsPregnant(command.changedBy());
    }
    
    /**
     * Aplica el status VENDIDA con validaciones de fecha y precio.
     * Requiere fechaVenta obligatoria y precioVenta opcional > 0 (Requirement 4.3).
     * 
     * @param animal animal a actualizar
     * @param command comando con datos de venta
     * @return Animal con status VENDIDA
     * @throws IllegalArgumentException si fechaVenta es null o precioVenta <= 0
     */
    private Animal applySoldStatus(Animal animal, ChangeStatusCommand command) {
        log.debug("Aplicando status VENDIDA: animalId={}, fechaVenta={}, precioVenta={}", 
                  animal.getAnimalId(), command.fechaVenta(), command.precioVenta());
        
        // Validar fechaVenta obligatoria
        if (command.fechaVenta() == null) {
            log.warn("Intento de marcar como vendida sin fecha de venta: animalId={}", animal.getAnimalId());
            throw new IllegalArgumentException("La fecha de venta es obligatoria");
        }
        
        // Validar precioVenta si se proporciona
        if (command.precioVenta() != null && command.precioVenta().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Intento de marcar como vendida con precio inválido: animalId={}, precioVenta={}", 
                     animal.getAnimalId(), command.precioVenta());
            throw new IllegalArgumentException("El precio de venta debe ser mayor que cero");
        }
        
        return animal.markAsSold(command.fechaVenta(), command.precioVenta(), command.changedBy());
    }
    
    /**
     * Aplica el status MUERTA con validaciones de fecha y motivo.
     * Requiere fechaMuerte obligatoria y motivoMuerte opcional (Requirement 4.4).
     * 
     * @param animal animal a actualizar
     * @param command comando con datos de muerte
     * @return Animal con status MUERTA
     * @throws IllegalArgumentException si fechaMuerte es null
     */
    private Animal applyDeadStatus(Animal animal, ChangeStatusCommand command) {
        log.debug("Aplicando status MUERTA: animalId={}, fechaMuerte={}, motivoMuerte={}", 
                  animal.getAnimalId(), command.fechaMuerte(), command.motivoMuerte());
        
        // Validar fechaMuerte obligatoria
        if (command.fechaMuerte() == null) {
            log.warn("Intento de marcar como muerta sin fecha de muerte: animalId={}", animal.getAnimalId());
            throw new IllegalArgumentException("La fecha de muerte es obligatoria");
        }
        
        return animal.markAsDead(command.fechaMuerte(), command.motivoMuerte(), command.changedBy());
    }
    
    /**
     * Aplica status simple (ACTIVA, PRESTADA, EN_REPOSO) sin validaciones adicionales.
     * 
     * @param animal animal a actualizar
     * @param command comando con datos
     * @return Animal con status actualizado
     */
    private Animal applySimpleStatus(Animal animal, ChangeStatusCommand command) {
        log.debug("Aplicando status simple: animalId={}, newStatus={}", 
                  animal.getAnimalId(), command.newStatus());
        
        // Usar el builder para cambiar el status
        return new Animal.Builder()
            .from(animal)
            .status(command.newStatus())
            .updatedAt(Instant.now())
            .updatedBy(command.changedBy())
            .build();
    }
    
    /**
     * Cierra el registro actual en pasture_history cuando el animal sale del rancho.
     * Actualiza fecha_salida = NOW para el registro actual (fecha_salida = null).
     * Se ejecuta cuando status cambia a VENDIDA o MUERTA (Requirement 4.6, 3.10).
     * 
     * @param animalId UUID del animal
     */
    private void closeCurrentPastureHistory(java.util.UUID animalId) {
        log.debug("Cerrando historial de potrero actual: animalId={}", animalId);
        
        Optional<PastureHistory> currentHistory = pastureHistoryRepository.findCurrentByAnimalId(animalId);
        
        if (currentHistory.isPresent()) {
            pastureHistoryRepository.updateFechaSalida(
                currentHistory.get().getHistoryId(),
                Instant.now()
            );
            log.debug("Historial de potrero cerrado: animalId={}, historyId={}", 
                      animalId, currentHistory.get().getHistoryId());
        } else {
            log.debug("No hay historial de potrero actual para cerrar: animalId={}", animalId);
        }
    }
}
