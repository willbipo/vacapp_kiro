package mx.vacapp.cattle.internal.application.usecases.movement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;
import mx.vacapp.cattle.internal.domain.model.AgeCalculator;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.PastureHistoryRepository;
import mx.vacapp.cattle.internal.domain.repository.WeightRepository;
import mx.vacapp.cattle.internal.infrastructure.integration.GeographyServiceClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso para listar todos los animales actualmente ubicados en un potrero específico.
 * 
 * <p>Este caso de uso implementa el flujo completo para obtener la lista de animales
 * que se encuentran físicamente en un potrero dado, incluyendo el cálculo en tiempo real
 * de edad (meses) y opcionalmente enriqueciendo los resultados con el peso actual de cada animal.</p>
 * 
 * <h2>Flujo de Ejecución:</h2>
 * <ol>
 *   <li>Validar que el potrero existe y está activo usando GeographyServiceClient</li>
 *   <li>Obtener todos los IDs de animales en el potrero desde PastureHistoryRepository
 *       (WHERE potrero_id = ? AND fecha_salida IS NULL)</li>
 *   <li>Para cada animal ID, obtener el Animal completo desde AnimalRepository</li>
 *   <li>Calcular meses (edad) en tiempo real usando AgeCalculator</li>
 *   <li>Opcionalmente obtener peso_actual desde WeightRepository</li>
 *   <li>Ordenar resultados por arete ASC</li>
 *   <li>Retornar List&lt;AnimalResult&gt; con datos enriquecidos</li>
 * </ol>
 * 
 * <h2>Validaciones de Negocio:</h2>
 * <ul>
 *   <li><b>Potrero activo:</b> El potrero debe existir y estar activo en el módulo
 *       geographic-control (Requirement 3.1, 3.2)</li>
 *   <li><b>Multi-tenancy:</b> Solo retorna animales del tenant del contexto de seguridad</li>
 *   <li><b>Ubicación actual:</b> Solo considera animales con fecha_salida = null en
 *       pasture_history (animales actualmente en el potrero)</li>
 * </ul>
 * 
 * <h2>Nota sobre Performance:</h2>
 * <p>Este caso de uso realiza múltiples consultas a repositorios (N+1 pattern) para
 * obtener animales individuales. En escenarios con potreros que contienen cientos
 * de animales, considerar optimización con queries batch o proyecciones.</p>
 * 
 * @see AnimalResult
 * @see Animal
 * @see AnimalRepository
 * @see PastureHistoryRepository
 * @see WeightRepository
 * @see GeographyServiceClient
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListAnimalsInPastureUseCase {
    
    private final GeographyServiceClient geographyServiceClient;
    private final PastureHistoryRepository pastureHistoryRepository;
    private final AnimalRepository animalRepository;
    private final WeightRepository weightRepository;
    
    /**
     * Ejecuta el caso de uso para listar animales en un potrero específico.
     * 
     * <p>Retorna una lista ordenada alfabéticamente por arete (ASC) de todos
     * los animales actualmente ubicados en el potrero. Cada AnimalResult incluye:</p>
     * <ul>
     *   <li>Todos los campos básicos del animal (arete, sexo, raza, etc.)</li>
     *   <li>meses calculado en tiempo real desde fecha_nacimiento</li>
     *   <li>pesoActual obtenido del registro de peso más reciente (o null si no tiene pesos)</li>
     *   <li>potreroActual establecido al UUID del potrero consultado</li>
     * </ul>
     * 
     * @param potreroId UUID del potrero a consultar
     * @return lista de AnimalResult ordenada por arete ASC, puede estar vacía si el
     *         potrero no tiene animales actualmente
     * @throws mx.vacapp.cattle.internal.domain.model.exceptions.InvalidPastureException 
     *         si el potrero no existe o está inactivo
     * @throws IllegalArgumentException si potreroId es null
     */
    @Transactional(readOnly = true)
    public List<AnimalResult> execute(UUID potreroId) {
        if (potreroId == null) {
            throw new IllegalArgumentException("potreroId no puede ser null");
        }
        
        log.info("Iniciando listado de animales en potrero: potreroId={}", potreroId);
        
        // 1. Validar que potrero existe y está activo (Requirement 3.1, 3.2)
        geographyServiceClient.validatePotreroActive(potreroId);
        log.debug("Potrero validado exitosamente: potreroId={}", potreroId);
        
        // 2. Obtener todos los IDs de animales actualmente en el potrero
        // (WHERE potrero_id = ? AND fecha_salida IS NULL)
        List<UUID> animalIds = pastureHistoryRepository.findAnimalIdsByPotreroId(potreroId);
        log.debug("Encontrados {} animales en potrero: potreroId={}", animalIds.size(), potreroId);
        
        if (animalIds.isEmpty()) {
            log.info("No hay animales en el potrero: potreroId={}", potreroId);
            return List.of();
        }
        
        // 3. Para cada animalId, obtener Animal completo y enriquecer con datos calculados
        List<AnimalResult> results = animalIds.stream()
            .map(animalId -> buildEnrichedAnimalResult(animalId, potreroId))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .sorted(Comparator.comparing(AnimalResult::arete))  // 4. Ordenar por arete ASC
            .toList();
        
        log.info("Retornando {} animales del potrero: potreroId={}", results.size(), potreroId);
        return results;
    }
    
    /**
     * Construye un AnimalResult enriquecido con datos calculados en tiempo real.
     * 
     * <p>Este método obtiene el animal del repositorio y enriquece el resultado con:</p>
     * <ul>
     *   <li>meses (edad) calculada en tiempo real</li>
     *   <li>pesoActual del registro más reciente (si existe)</li>
     *   <li>potreroActual (el UUID del potrero donde está ubicado)</li>
     * </ul>
     * 
     * <p>Si el animal no existe (por inconsistencia de datos), retorna Optional.empty()
     * y registra un warning en logs. Esto evita que un animal faltante rompa toda la lista.</p>
     * 
     * @param animalId UUID del animal a enriquecer
     * @param potreroId UUID del potrero actual del animal
     * @return Optional con AnimalResult enriquecido, o empty si el animal no existe
     */
    private Optional<AnimalResult> buildEnrichedAnimalResult(UUID animalId, UUID potreroId) {
        log.debug("Obteniendo datos de animal: animalId={}", animalId);
        
        // Obtener animal del repositorio (filtra automáticamente por tenant_id)
        Optional<Animal> animalOpt = animalRepository.findById(animalId);
        
        if (animalOpt.isEmpty()) {
            log.warn("Animal no encontrado en repositorio pero existe en pasture_history. " +
                     "Posible inconsistencia de datos: animalId={}", animalId);
            return Optional.empty();
        }
        
        Animal animal = animalOpt.get();
        
        // Calcular meses (edad) en tiempo real (Requirement 2.10)
        int mesesActuales = AgeCalculator.calculateMonths(
            animal.getFechaNacimiento(), 
            LocalDate.now()
        );
        log.debug("Edad calculada para animal: animalId={}, arete={}, meses={}", 
                  animalId, animal.getArete(), mesesActuales);
        
        // Obtener peso actual (opcional) - último peso registrado (Requirement 7.6)
        BigDecimal pesoActual = weightRepository.findLatestWeight(animalId)
            .map(weight -> weight.getPesoKg())
            .orElse(null);
        
        if (pesoActual != null) {
            log.debug("Peso actual encontrado: animalId={}, arete={}, pesoActual={} kg", 
                      animalId, animal.getArete(), pesoActual);
        } else {
            log.debug("Animal sin registros de peso: animalId={}, arete={}", 
                      animalId, animal.getArete());
        }
        
        // Construir AnimalResult enriquecido
        AnimalResult result = AnimalResult.fromDomainEnriched(
            animal,
            mesesActuales,
            pesoActual,
            potreroId,  // potreroActual
            null,       // nombreMadre (no requerido para este caso de uso)
            null        // nombrePadre (no requerido para este caso de uso)
        );
        
        return Optional.of(result);
    }
}
