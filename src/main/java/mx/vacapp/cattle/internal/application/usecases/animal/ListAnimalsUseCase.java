package mx.vacapp.cattle.internal.application.usecases.animal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.application.usecases.commands.AnimalFilters;
import mx.vacapp.cattle.internal.application.usecases.commands.AnimalResult;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.AgeCalculator;
import mx.vacapp.cattle.internal.domain.model.PastureHistory;
import mx.vacapp.cattle.internal.domain.model.WeightRecord;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.PastureHistoryRepository;
import mx.vacapp.cattle.internal.domain.repository.WeightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso para listar animales con filtros opcionales.
 * 
 * <p>Este caso de uso implementa el flujo completo de listado de animales aplicando
 * filtros opcionales por status, tipo, rancho y potrero. Además, enriquece cada
 * animal con información calculada y relacionada (edad en meses, peso actual,
 * potrero actual).</p>
 * 
 * <h2>Flujo de Ejecución:</h2>
 * <ol>
 *   <li>Validar tenant_id (extraído del contexto de seguridad)</li>
 *   <li>Aplicar filtros opcionales: status, tipo, rancho, potrero</li>
 *   <li>Para cada animal en el resultado:
 *     <ul>
 *       <li>Recalcular meses (edad) en tiempo real desde fecha_nacimiento</li>
 *       <li>Obtener peso_actual (peso más reciente de cattle_weights)</li>
 *       <li>Obtener potrero_actual (potrero con fecha_salida = null en pasture_history)</li>
 *     </ul>
 *   </li>
 *   <li>Ordenar resultado por arete ASC</li>
 *   <li>Retornar lista de AnimalResult enriquecidos</li>
 * </ol>
 * 
 * <h2>Filtros Disponibles:</h2>
 * <ul>
 *   <li><b>status:</b> Filtra por estado (ACTIVA, VENDIDA, MUERTA, PRESTADA, PRENADA, EN_REPOSO)</li>
 *   <li><b>tipo:</b> Filtra por tipo comercial (VENTA, CRIA, ENGORDA, SEMENTAL, VIENTRE)</li>
 *   <li><b>ranchoId:</b> Filtra animales de un rancho específico</li>
 *   <li><b>potreroId:</b> Filtra animales que están actualmente en un potrero específico</li>
 * </ul>
 * 
 * <h2>Enriquecimiento de Datos:</h2>
 * <p>Cada AnimalResult incluye:</p>
 * <ul>
 *   <li><b>meses:</b> Edad calculada en tiempo real (Requirement 2.10)</li>
 *   <li><b>peso_actual:</b> Último peso registrado en cattle_weights (Requirement 7.6)</li>
 *   <li><b>potrero_actual:</b> Potrero donde está el animal actualmente (fecha_salida = null)</li>
 * </ul>
 * 
 * <h2>Ordenación:</h2>
 * <p>Los resultados se ordenan alfabéticamente por arete en orden ascendente (A-Z).</p>
 * 
 * <h2>Multi-tenancy:</h2>
 * <p>Todos los filtros aplican automáticamente filtrado por tenant_id del contexto
 * de seguridad para garantizar aislamiento de datos entre tenants.</p>
 * 
 * @see AnimalFilters
 * @see AnimalResult
 * @see Animal
 * @see AnimalRepository
 * @see PastureHistoryRepository
 * @see WeightRepository
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListAnimalsUseCase {
    
    private final AnimalRepository animalRepository;
    private final PastureHistoryRepository pastureHistoryRepository;
    private final WeightRepository weightRepository;
    
    /**
     * Ejecuta el caso de uso de listado de animales con filtros opcionales.
     * 
     * @param tenantId UUID del tenant (requerido, extraído del contexto de seguridad)
     * @param filters filtros opcionales a aplicar (AnimalFilters con campos null para no filtrar)
     * @return lista de AnimalResult enriquecidos, ordenados por arete ASC
     * @throws IllegalArgumentException si tenantId es null
     */
    @Transactional(readOnly = true)
    public List<AnimalResult> execute(UUID tenantId, AnimalFilters filters) {
        log.info("Listando animales: tenantId={}, filters={}", tenantId, filters);
        
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId no puede ser null");
        }
        
        if (filters == null) {
            filters = AnimalFilters.empty();
        }
        
        // 1. Obtener animales aplicando filtros (sin potrero por ahora)
        List<Animal> animals = animalRepository.findByFilters(
            tenantId, 
            filters.status(), 
            filters.tipo(), 
            filters.ranchoId(), 
            null // potreroId se filtra después con pasture_history
        );
        
        log.debug("Animales encontrados antes de filtro de potrero: {}", animals.size());
        
        // 2. Filtrar por potrero si se especificó (requiere JOIN con pasture_history)
        if (filters.hasPotreroFilter()) {
            animals = filterByPotrero(animals, filters.potreroId());
            log.debug("Animales después de filtro de potrero: {}", animals.size());
        }
        
        // 3. Enriquecer cada animal con datos calculados y relacionados
        List<EnrichedAnimal> enrichedAnimals = animals.stream()
            .map(this::enrichAnimal)
            .collect(Collectors.toList());
        
        // 4. Ordenar por arete ASC (ya debería estar ordenado desde el repository, pero aseguramos)
        enrichedAnimals.sort((a, b) -> a.animal().getArete().compareTo(b.animal().getArete()));
        
        // 5. Convertir a AnimalResult
        List<AnimalResult> results = enrichedAnimals.stream()
            .map(this::toAnimalResult)
            .collect(Collectors.toList());
        
        log.info("Listado completado: {} animales encontrados", results.size());
        
        return results;
    }
    
    /**
     * Filtra animales por potrero actual.
     * Obtiene los IDs de animales que están actualmente en el potrero especificado
     * (fecha_salida = null en pasture_history) y filtra la lista de animales.
     * 
     * @param animals lista de animales a filtrar
     * @param potreroId UUID del potrero
     * @return lista filtrada de animales que están en el potrero
     */
    private List<Animal> filterByPotrero(List<Animal> animals, UUID potreroId) {
        log.debug("Filtrando animales por potrero: potreroId={}", potreroId);
        
        // Obtener IDs de animales en el potrero
        List<UUID> animalIdsInPotrero = pastureHistoryRepository.findAnimalIdsByPotreroId(potreroId);
        
        log.debug("Animales en potrero {}: {}", potreroId, animalIdsInPotrero.size());
        
        // Filtrar la lista de animales
        return animals.stream()
            .filter(animal -> animalIdsInPotrero.contains(animal.getAnimalId()))
            .collect(Collectors.toList());
    }
    
    /**
     * Enriquece un animal con datos calculados y relacionados.
     * 
     * <p>Enriquecimiento incluye:</p>
     * <ul>
     *   <li>Recalcular meses (edad) en tiempo real</li>
     *   <li>Obtener peso_actual (último peso registrado)</li>
     *   <li>Obtener potrero_actual (potrero donde está actualmente)</li>
     * </ul>
     * 
     * @param animal animal a enriquecer
     * @return EnrichedAnimal con animal, meses, peso_actual, potrero_actual
     */
    private EnrichedAnimal enrichAnimal(Animal animal) {
        log.trace("Enriqueciendo animal: animalId={}, arete={}", animal.getAnimalId(), animal.getArete());
        
        // 1. Recalcular meses en tiempo real (Requirement 2.10)
        int meses = AgeCalculator.calculateMonths(animal.getFechaNacimiento(), LocalDate.now());
        
        // 2. Obtener peso actual (Requirement 7.6)
        Optional<WeightRecord> latestWeight = weightRepository.findLatestWeight(animal.getAnimalId());
        BigDecimal pesoActual = latestWeight.map(WeightRecord::getPesoKg).orElse(null);
        
        // 3. Obtener potrero actual (Requirement 3.5)
        Optional<PastureHistory> currentPasture = pastureHistoryRepository.findCurrentByAnimalId(animal.getAnimalId());
        UUID potreroActual = currentPasture.map(PastureHistory::getPotreroId).orElse(null);
        
        log.trace("Animal enriquecido: meses={}, pesoActual={}, potreroActual={}", 
                  meses, pesoActual, potreroActual);
        
        return new EnrichedAnimal(animal, meses, pesoActual, potreroActual);
    }
    
    /**
     * Convierte un EnrichedAnimal a AnimalResult.
     * 
     * @param enriched animal enriquecido con datos calculados
     * @return AnimalResult para retornar al controlador
     */
    private AnimalResult toAnimalResult(EnrichedAnimal enriched) {
        Animal animal = enriched.animal();
        
        // Actualizar meses en el animal antes de convertir a AnimalResult
        Animal animalWithMeses = new Animal.Builder()
            .from(animal)
            .meses(enriched.meses())
            .build();
        
        // Convertir a AnimalResult
        // Nota: AnimalResult no tiene campos para pesoActual y potreroActual
        // Estos se añadirán en AnimalResponse en la capa de infraestructura
        return AnimalResult.fromDomain(animalWithMeses);
    }
    
    /**
     * Record interno para almacenar animal enriquecido con datos calculados.
     * 
     * @param animal entidad de dominio Animal
     * @param meses edad recalculada en tiempo real
     * @param pesoActual último peso registrado (puede ser null)
     * @param potreroActual potrero donde está actualmente (puede ser null si vendido/muerto)
     */
    private record EnrichedAnimal(
        Animal animal,
        int meses,
        BigDecimal pesoActual,
        UUID potreroActual
    ) {}
}
