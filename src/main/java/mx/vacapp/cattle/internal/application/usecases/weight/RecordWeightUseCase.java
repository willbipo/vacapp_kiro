package mx.vacapp.cattle.internal.application.usecases.weight;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.application.usecases.commands.RecordWeightCommand;
import mx.vacapp.cattle.internal.application.usecases.commands.WeightResult;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.WeightRecord;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.model.exceptions.SoldOrDeadAnimalException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.WeightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Caso de uso para registrar un nuevo peso de un animal en el sistema de inventario de ganado.
 * 
 * <p>Este caso de uso implementa el flujo completo de registro de peso de un animal,
 * incluyendo todas las validaciones de negocio, creación del registro de peso,
 * persistencia, y cálculo de ganancia diaria comparando con el peso anterior si existe.</p>
 * 
 * <h2>Flujo de Ejecución:</h2>
 * <ol>
 *   <li>Validar que el animal existe y pertenece al tenant del usuario</li>
 *   <li>Validar que el animal está activo (no Vendida/Muerta)</li>
 *   <li>Validar que pesoKg es mayor que 0</li>
 *   <li>Validar que fechaPesaje no es futura (debe ser <= HOY)</li>
 *   <li>Crear entidad WeightRecord usando WeightRecord.create()</li>
 *   <li>Persistir registro de peso en WeightRepository</li>
 *   <li>Buscar peso anterior (el peso inmediatamente anterior a fechaPesaje)</li>
 *   <li>Si existe peso anterior:
 *     <ul>
 *       <li>Calcular ganancia_diaria = (peso_actual - peso_anterior) / días_entre_pesajes</li>
 *       <li>Calcular días transcurridos entre pesajes</li>
 *     </ul>
 *   </li>
 *   <li>Retornar WeightResult con ganancia_diaria calculada (null si es el primer peso)</li>
 * </ol>
 * 
 * <h2>Validaciones de Negocio:</h2>
 * <ul>
 *   <li><b>Animal existe:</b> El animal debe existir en el sistema y pertenecer al tenant</li>
 *   <li><b>Animal activo:</b> No se puede registrar peso de animales Vendidos o Muertos
 *       (Requirement 7.7, 16.1)</li>
 *   <li><b>Peso positivo:</b> El peso debe ser estrictamente mayor que cero
 *       (Requirement 7.3)</li>
 *   <li><b>Fecha no futura:</b> La fecha de pesaje no puede ser posterior a la fecha actual
 *       (Requirement 7.2)</li>
 * </ul>
 * 
 * <h2>Cálculo de Ganancia Diaria:</h2>
 * <p>La ganancia diaria se calcula comparando el peso actual con el peso inmediatamente anterior
 * registrado para el mismo animal. La fórmula es:</p>
 * <pre>
 * ganancia_diaria = (peso_actual - peso_anterior) / días_entre_pesajes
 * </pre>
 * <p>
 * Si no existe un peso anterior (es el primer peso del animal), ganancia_diaria y
 * diasDesdeUltimoPesaje serán null en el resultado.
 * </p>
 * 
 * <h2>Atomicidad:</h2>
 * <p>Toda la operación se ejecuta en una transacción {@code @Transactional} para garantizar
 * que si la persistencia falla, no se deja el sistema en estado inconsistente.</p>
 * 
 * @see RecordWeightCommand
 * @see WeightResult
 * @see WeightRecord
 * @see AnimalRepository
 * @see WeightRepository
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordWeightUseCase {
    
    private final AnimalRepository animalRepository;
    private final WeightRepository weightRepository;
    
    /**
     * Ejecuta el caso de uso de registro de peso.
     * 
     * @param command comando con todos los datos del peso a registrar
     * @return WeightResult con los datos del peso registrado y ganancia_diaria calculada
     * @throws AnimalNotFoundException si el animal no existe o no pertenece al tenant
     * @throws SoldOrDeadAnimalException si el animal está vendido o muerto
     * @throws IllegalArgumentException si el peso es <= 0 o la fecha es futura
     */
    @Transactional
    public WeightResult execute(RecordWeightCommand command) {
        log.info("Iniciando registro de peso: animalId={}, pesoKg={}, fechaPesaje={}, tenantId={}", 
                 command.animalId(), command.pesoKg(), command.fechaPesaje(), command.tenantId());
        
        // 1. Validar que animal existe y pertenece al tenant (Requirement 7.7)
        Animal animal = findAnimalById(command.animalId());
        validateAnimalBelongsToTenant(animal, command.tenantId());
        
        // 2. Validar que animal está activo (no vendido/muerto) (Requirement 16.1)
        validateAnimalNotSoldOrDead(animal);
        
        // 3. Validar que peso > 0 (Requirement 7.3)
        validatePesoPositivo(command.pesoKg());
        
        // 4. Validar que fecha no es futura (Requirement 7.2)
        validateFechaPesajeNotFuture(command.fechaPesaje());
        
        // 5. Crear entidad de dominio WeightRecord
        WeightRecord weightRecord = WeightRecord.create(
            command.animalId(),
            command.pesoKg(),
            command.fechaPesaje(),
            command.notas(),
            command.recordedBy()
        );
        
        // 6. Persistir registro de peso
        WeightRecord savedWeight = weightRepository.save(weightRecord);
        log.debug("Peso persistido: weightId={}, animalId={}, pesoKg={}", 
                  savedWeight.getWeightId(), savedWeight.getAnimalId(), savedWeight.getPesoKg());
        
        // 7. Buscar peso anterior para calcular ganancia diaria (Requirement 7.4, 7.5)
        Optional<WeightRecord> previousWeightOpt = weightRepository.findPreviousWeight(
            command.animalId(), 
            command.fechaPesaje()
        );
        
        BigDecimal gananciaDiaria = null;
        Integer diasDesdeUltimoPesaje = null;
        
        // 8. Calcular ganancia diaria si existe peso anterior
        if (previousWeightOpt.isPresent()) {
            WeightRecord previousWeight = previousWeightOpt.get();
            
            // Calcular días entre pesajes
            long dias = ChronoUnit.DAYS.between(previousWeight.getFechaPesaje(), command.fechaPesaje());
            diasDesdeUltimoPesaje = (int) dias;
            
            // Calcular ganancia diaria: (peso_actual - peso_anterior) / días
            if (dias > 0) {
                BigDecimal diferenciaPeso = command.pesoKg().subtract(previousWeight.getPesoKg());
                gananciaDiaria = diferenciaPeso.divide(
                    BigDecimal.valueOf(dias), 
                    3,  // 3 decimales de precisión
                    RoundingMode.HALF_UP
                );
                
                log.debug("Ganancia diaria calculada: animalId={}, pesoAnterior={}, pesoActual={}, " +
                          "dias={}, gananciaDiaria={} kg/día",
                          command.animalId(), previousWeight.getPesoKg(), command.pesoKg(), 
                          dias, gananciaDiaria);
            } else {
                log.warn("Días entre pesajes es 0, no se puede calcular ganancia diaria: animalId={}", 
                         command.animalId());
            }
        } else {
            log.debug("No existe peso anterior para el animal, ganancia_diaria = null: animalId={}", 
                      command.animalId());
        }
        
        log.info("Peso registrado exitosamente: weightId={}, animalId={}, pesoKg={}, gananciaDiaria={}", 
                 savedWeight.getWeightId(), savedWeight.getAnimalId(), 
                 savedWeight.getPesoKg(), gananciaDiaria);
        
        // 9. Retornar resultado con ganancia diaria calculada
        return WeightResult.fromDomain(savedWeight, gananciaDiaria, diasDesdeUltimoPesaje);
    }
    
    /**
     * Busca un animal por su ID.
     * 
     * @param animalId UUID del animal
     * @return el animal encontrado
     * @throws AnimalNotFoundException si el animal no existe
     */
    private Animal findAnimalById(java.util.UUID animalId) {
        log.debug("Buscando animal por ID: animalId={}", animalId);
        
        return animalRepository.findById(animalId)
            .orElseThrow(() -> {
                log.warn("Animal no encontrado: animalId={}", animalId);
                return new AnimalNotFoundException("Animal no encontrado");
            });
    }
    
    /**
     * Valida que el animal pertenece al tenant del usuario autenticado.
     * 
     * @param animal el animal a validar
     * @param tenantId el tenant ID del usuario autenticado
     * @throws AnimalNotFoundException si el animal no pertenece al tenant
     */
    private void validateAnimalBelongsToTenant(Animal animal, java.util.UUID tenantId) {
        if (!animal.getTenantId().equals(tenantId)) {
            log.warn("Animal no pertenece al tenant: animalId={}, animalTenantId={}, requestTenantId={}", 
                     animal.getAnimalId(), animal.getTenantId(), tenantId);
            throw new AnimalNotFoundException("Animal no encontrado");
        }
    }
    
    /**
     * Valida que el animal no está vendido o muerto.
     * No se pueden registrar pesos de animales vendidos o muertos (Requirement 16.1).
     * 
     * @param animal el animal a validar
     * @throws SoldOrDeadAnimalException si el animal está vendido o muerto
     */
    private void validateAnimalNotSoldOrDead(Animal animal) {
        if (animal.isSoldOrDead()) {
            log.warn("Intento de registrar peso de animal vendido o muerto: animalId={}, status={}", 
                     animal.getAnimalId(), animal.getStatus());
            throw new SoldOrDeadAnimalException("No se puede modificar un animal vendido o muerto");
        }
    }
    
    /**
     * Valida que el peso es estrictamente mayor que cero.
     * 
     * @param pesoKg el peso a validar
     * @throws IllegalArgumentException si el peso es <= 0
     */
    private void validatePesoPositivo(BigDecimal pesoKg) {
        if (pesoKg == null || pesoKg.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Intento de registrar peso inválido: pesoKg={}", pesoKg);
            throw new IllegalArgumentException("El peso debe ser mayor que cero");
        }
    }
    
    /**
     * Valida que la fecha de pesaje no es futura.
     * 
     * @param fechaPesaje la fecha a validar
     * @throws IllegalArgumentException si la fecha es posterior a hoy
     */
    private void validateFechaPesajeNotFuture(LocalDate fechaPesaje) {
        if (fechaPesaje.isAfter(LocalDate.now())) {
            log.warn("Intento de registrar peso con fecha futura: fechaPesaje={}", fechaPesaje);
            throw new IllegalArgumentException("La fecha de pesaje no puede ser futura");
        }
    }
}
