package mx.vacapp.cattle.internal.application.usecases.animal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;
import mx.vacapp.cattle.internal.domain.model.AgeCalculator;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.PastureHistory;
import mx.vacapp.cattle.internal.domain.model.WeightRecord;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.PastureHistoryRepository;
import mx.vacapp.cattle.internal.domain.repository.WeightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso para obtener un animal por su ID con cálculos en tiempo real.
 * 
 * <p>Este caso de uso recupera un animal del repositorio y enriquece la información
 * con datos calculados en tiempo real y relacionados de otras entidades:</p>
 * 
 * <h2>Campos Enriquecidos:</h2>
 * <ul>
 *   <li><b>meses:</b> Edad calculada en tiempo real desde fecha_nacimiento hasta LocalDate.now()
 *       usando Period.between() (Requirement 2.10)</li>
 *   <li><b>pesoActual:</b> Peso más reciente del animal obtenido de WeightRepository (Requirement 7.6)</li>
 *   <li><b>potreroActual:</b> Potrero donde está ubicado actualmente el animal,
 *       obtenido de PastureHistoryRepository WHERE fecha_salida IS NULL (Requirement 3)</li>
 *   <li><b>nombreMadre:</b> Nombre de la madre obtenido de AnimalRepository usando madreId (Requirement 2.9)</li>
 *   <li><b>nombrePadre:</b> Nombre del padre obtenido de AnimalRepository usando padreId (Requirement 2.9)</li>
 * </ul>
 * 
 * <h2>Validaciones:</h2>
 * <ul>
 *   <li>El animal debe existir en el sistema</li>
 *   <li>El animal debe pertenecer al tenant del contexto actual (multi-tenancy)</li>
 * </ul>
 * 
 * <h2>Flujo de Ejecución:</h2>
 * <ol>
 *   <li>Buscar animal por ID en AnimalRepository</li>
 *   <li>Validar que existe y pertenece al tenant (AnimalNotFoundException si falla)</li>
 *   <li>Calcular meses en tiempo real usando AgeCalculator.calculateMonths()</li>
 *   <li>Obtener peso actual (último peso) de WeightRepository</li>
 *   <li>Obtener potrero actual de PastureHistoryRepository (WHERE fecha_salida IS NULL)</li>
 *   <li>Obtener nombres de madre y padre si existen (madreId/padreId)</li>
 *   <li>Construir y retornar AnimalResult enriquecido</li>
 * </ol>
 * 
 * @see AnimalResult
 * @see Animal
 * @see AgeCalculator
 * @see AnimalRepository
 * @see WeightRepository
 * @see PastureHistoryRepository
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetAnimalUseCase {
    
    private final AnimalRepository animalRepository;
    private final WeightRepository weightRepository;
    private final PastureHistoryRepository pastureHistoryRepository;
    
    /**
     * Ejecuta el caso de uso para obtener un animal por ID con datos enriquecidos.
     * 
     * @param animalId UUID del animal a consultar
     * @return AnimalResult con datos del animal enriquecidos con cálculos en tiempo real
     * @throws AnimalNotFoundException si el animal no existe o no pertenece al tenant actual
     * @throws IllegalArgumentException si animalId es null
     */
    @Transactional(readOnly = true)
    public AnimalResult execute(UUID animalId) {
        log.info("Consultando animal: animalId={}", animalId);
        
        if (animalId == null) {
            throw new IllegalArgumentException("animalId no puede ser null");
        }
        
        // 1. Buscar animal en repositorio
        Optional<Animal> animalOpt = animalRepository.findById(animalId);
        
        if (animalOpt.isEmpty()) {
            log.warn("Animal no encontrado: animalId={}", animalId);
            throw new AnimalNotFoundException("Animal no encontrado");
        }
        
        Animal animal = animalOpt.get();
        log.debug("Animal encontrado: arete={}, tenantId={}", 
                  animal.getArete(), animal.getTenantId());
        
        // 2. Calcular meses en tiempo real (Requirement 2.10)
        int mesesActuales = AgeCalculator.calculateMonths(
            animal.getFechaNacimiento(), 
            LocalDate.now()
        );
        log.debug("Edad calculada en tiempo real: {} meses", mesesActuales);
        
        // 3. Obtener peso actual (último peso registrado) (Requirement 7.6)
        BigDecimal pesoActual = null;
        Optional<WeightRecord> latestWeight = weightRepository.findLatestWeight(animalId);
        if (latestWeight.isPresent()) {
            pesoActual = latestWeight.get().getPesoKg();
            log.debug("Peso actual: {} kg (fecha: {})", 
                      pesoActual, latestWeight.get().getFechaPesaje());
        } else {
            log.debug("Animal sin registros de peso");
        }
        
        // 4. Obtener potrero actual (WHERE fecha_salida IS NULL) (Requirement 3)
        UUID potreroActual = null;
        Optional<PastureHistory> currentLocation = pastureHistoryRepository.findCurrentByAnimalId(animalId);
        if (currentLocation.isPresent()) {
            potreroActual = currentLocation.get().getPotreroId();
            log.debug("Ubicación actual: potreroId={}", potreroActual);
        } else {
            log.debug("Animal sin ubicación actual (posiblemente vendido o muerto)");
        }
        
        // 5. Obtener nombre de madre si existe (Requirement 2.9)
        String nombreMadre = null;
        if (animal.getMadreId() != null) {
            Optional<Animal> madre = animalRepository.findById(animal.getMadreId());
            if (madre.isPresent()) {
                nombreMadre = madre.get().getArete();  // Usando arete como nombre identificador
                log.debug("Madre: {} ({})", nombreMadre, animal.getMadreId());
            }
        }
        
        // 6. Obtener nombre de padre si existe (Requirement 2.9)
        String nombrePadre = null;
        if (animal.getPadreId() != null) {
            Optional<Animal> padre = animalRepository.findById(animal.getPadreId());
            if (padre.isPresent()) {
                nombrePadre = padre.get().getArete();  // Usando arete como nombre identificador
                log.debug("Padre: {} ({})", nombrePadre, animal.getPadreId());
            }
        }
        
        // 7. Construir AnimalResult enriquecido con todos los campos calculados
        AnimalResult enrichedResult = AnimalResult.fromDomainEnriched(
            animal,
            mesesActuales,      // Edad calculada en tiempo real
            pesoActual,          // Último peso registrado
            potreroActual,       // Ubicación actual
            nombreMadre,         // Arete de la madre
            nombrePadre          // Arete del padre
        );
        
        log.info("Animal recuperado exitosamente: animalId={}, arete={}, meses={}, pesoActual={}, potreroActual={}", 
                 animal.getAnimalId(), animal.getArete(), mesesActuales, pesoActual, potreroActual);
        
        return enrichedResult;
    }
}
