package mx.vacapp.cattle.internal.application.usecases.movement;

import mx.vacapp.cattle.internal.application.usecases.commands.PastureHistoryResult;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.PastureHistory;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.PastureHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso para obtener el historial completo de movimientos de un animal entre potreros.
 * <p>
 * Este caso de uso implementa la funcionalidad de consulta de trazabilidad de ubicaciones
 * de un animal, retornando todos los registros históricos ordenados cronológicamente
 * con el cálculo de días de permanencia en cada potrero.
 * </p>
 * 
 * <h2>Flujo de Ejecución:</h2>
 * <ol>
 *   <li>Valida que el animal existe en el sistema</li>
 *   <li>Obtiene todos los registros de PastureHistory para el animal</li>
 *   <li>Para cada registro, calcula dias_permanencia:
 *     <ul>
 *       <li>Si fecha_salida IS NOT NULL: dias = fecha_salida - fecha_entrada</li>
 *       <li>Si fecha_salida IS NULL (ubicación actual): dias = NOW - fecha_entrada</li>
 *     </ul>
 *   </li>
 *   <li>Ordena resultados por fecha_entrada DESC (más reciente primero)</li>
 *   <li>Retorna lista de PastureHistoryResult</li>
 * </ol>
 * 
 * <h2>Entrada:</h2>
 * <ul>
 *   <li>{@code animalId} - UUID del animal</li>
 * </ul>
 * 
 * <h2>Salida:</h2>
 * <ul>
 *   <li>{@code List<PastureHistoryResult>} - Lista de registros de historial ordenados cronológicamente</li>
 * </ul>
 * 
 * <h2>Excepciones:</h2>
 * <ul>
 *   <li>{@link AnimalNotFoundException} - Si el animal no existe</li>
 * </ul>
 * 
 * <h2>Invariantes:</h2>
 * <ul>
 *   <li>Los resultados están ordenados por fecha_entrada DESC (más reciente primero)</li>
 *   <li>El cálculo de dias_permanencia es preciso para ubicaciones históricas y actuales</li>
 *   <li>Si un animal nunca ha estado en un potrero, retorna lista vacía</li>
 *   <li>Un registro con fecha_salida = null representa la ubicación actual</li>
 * </ul>
 * 
 * @see PastureHistoryResult
 * @see PastureHistory
 * @see AnimalRepository
 * @see PastureHistoryRepository
 * 
 * @since 1.0
 * @author Vacapp Development Team
 */
@Service
@Transactional(readOnly = true)
public class GetMovementHistoryUseCase {
    
    private final AnimalRepository animalRepository;
    private final PastureHistoryRepository pastureHistoryRepository;
    
    /**
     * Constructor para inyección de dependencias.
     * 
     * @param animalRepository repositorio de animales
     * @param pastureHistoryRepository repositorio de historial de potreros
     */
    public GetMovementHistoryUseCase(
            AnimalRepository animalRepository,
            PastureHistoryRepository pastureHistoryRepository) {
        this.animalRepository = animalRepository;
        this.pastureHistoryRepository = pastureHistoryRepository;
    }
    
    /**
     * Ejecuta el caso de uso para obtener el historial completo de movimientos de un animal.
     * <p>
     * Retorna todos los registros de movimientos ordenados por fecha_entrada DESC (más reciente primero).
     * Para cada registro, calcula automáticamente dias_permanencia según:
     * <ul>
     *   <li>Si fecha_salida != null: dias = DATEDIFF(fecha_salida, fecha_entrada)</li>
     *   <li>Si fecha_salida = null: dias = DATEDIFF(NOW(), fecha_entrada)</li>
     * </ul>
     * </p>
     * 
     * @param animalId UUID del animal
     * @return lista de PastureHistoryResult ordenados cronológicamente (más reciente primero)
     * @throws AnimalNotFoundException si el animal no existe
     * @throws IllegalArgumentException si animalId es null
     */
    public List<PastureHistoryResult> execute(UUID animalId) {
        // Validación de entrada
        if (animalId == null) {
            throw new IllegalArgumentException("animalId no puede ser null");
        }
        
        // 1. Validar que el animal existe
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new AnimalNotFoundException(
                        "Animal no encontrado con ID: " + animalId));
        
        // 2. Obtener todos los registros de PastureHistory para el animal
        // El repositorio ya los retorna ordenados por fecha_entrada DESC
        List<PastureHistory> historyRecords = pastureHistoryRepository.findHistoryByAnimalId(animalId);
        
        // 3. Mapear a PastureHistoryResult (el cálculo de dias_permanencia se hace en fromDomain)
        // Los resultados ya están ordenados por fecha_entrada DESC (más reciente primero)
        return historyRecords.stream()
                .map(PastureHistoryResult::fromDomain)
                .toList();
    }
}
