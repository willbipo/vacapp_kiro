package mx.vacapp.cattle.internal.application.usecases.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.cattle.internal.application.usecases.commands.HealthEventResult;
import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.HealthEvent;
import mx.vacapp.cattle.internal.domain.model.exceptions.AnimalNotFoundException;
import mx.vacapp.cattle.internal.domain.repository.AnimalRepository;
import mx.vacapp.cattle.internal.domain.repository.HealthEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso para obtener el historial completo de eventos de salud de un animal.
 * 
 * <p>Este caso de uso implementa el flujo completo de consulta del historial de salud
 * de un animal. Retorna todos los eventos de salud (vacunaciones, tratamientos,
 * partos, diagnósticos) ordenados cronológicamente de más reciente a más antiguo.</p>
 * 
 * <h2>Flujo de Ejecución:</h2>
 * <ol>
 *   <li>Validar que el animal existe y pertenece al tenant del usuario</li>
 *   <li>Lanzar AnimalNotFoundException si el animal no existe o no pertenece al tenant</li>
 *   <li>Obtener todos los eventos de salud del animal desde HealthEventRepository</li>
 *   <li>Los eventos vienen ordenados cronológicamente (fecha DESC) por el repositorio</li>
 *   <li>Convertir las entidades de dominio HealthEvent a HealthEventResult</li>
 *   <li>Retornar lista de HealthEventResult ordenada cronológicamente</li>
 * </ol>
 * 
 * <h2>Validaciones de Negocio:</h2>
 * <ul>
 *   <li><b>Animal existe:</b> El animal debe existir y pertenecer al tenant del usuario (Requirement 8.6)</li>
 *   <li><b>Multi-tenancy:</b> El repositorio filtra automáticamente por tenant_id del contexto de seguridad</li>
 * </ul>
 * 
 * <h2>Orden Cronológico:</h2>
 * <p>Los eventos de salud se retornan ordenados por fecha_evento DESC (más reciente primero).
 * Este ordenamiento es garantizado por el HealthEventRepository.findByAnimalId() que retorna
 * eventos ordenados cronológicamente.</p>
 * 
 * <h2>Tipos de Eventos Incluidos:</h2>
 * <ul>
 *   <li><b>VACCINATION:</b> Vacunaciones del animal</li>
 *   <li><b>TREATMENT:</b> Tratamientos médicos</li>
 *   <li><b>BIRTH:</b> Eventos de parto (si el animal es madre)</li>
 *   <li><b>DIAGNOSIS:</b> Diagnósticos veterinarios</li>
 * </ul>
 * 
 * <h2>Comportamiento para Animales sin Historial:</h2>
 * <p>Si el animal no tiene eventos de salud registrados, el caso de uso retorna una lista vacía
 * en lugar de lanzar una excepción. Esto es el comportamiento esperado para animales nuevos
 * o animales sin eventos de salud.</p>
 * 
 * <h2>Transaccionalidad:</h2>
 * <p>Operación de solo lectura marcada con {@code @Transactional(readOnly = true)} para
 * optimizar el rendimiento de la consulta.</p>
 * 
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
public class GetHealthHistoryUseCase {
    
    private final AnimalRepository animalRepository;
    private final HealthEventRepository healthEventRepository;
    
    /**
     * Ejecuta el caso de uso de consulta de historial de salud.
     * 
     * <p>Retorna el historial completo de eventos de salud del animal ordenado
     * cronológicamente de más reciente a más antiguo (fecha DESC).</p>
     * 
     * @param animalId UUID del animal del cual consultar el historial de salud
     * @return Lista de HealthEventResult ordenada por fecha DESC (más reciente primero),
     *         lista vacía si el animal no tiene eventos de salud registrados
     * @throws AnimalNotFoundException si el animal no existe o no pertenece al tenant del usuario
     * @throws IllegalArgumentException si animalId es null
     */
    @Transactional(readOnly = true)
    public List<HealthEventResult> execute(UUID animalId) {
        log.info("Iniciando consulta de historial de salud: animalId={}", animalId);
        
        // Validar parámetro de entrada
        if (animalId == null) {
            log.warn("Intento de consultar historial con animalId null");
            throw new IllegalArgumentException("animalId no puede ser null");
        }
        
        // 1. Validar que el animal existe y pertenece al tenant (Requirement 8.8)
        validateAnimalExists(animalId);
        
        // 2. Obtener todos los eventos de salud del animal ordenados por fecha DESC
        // El repositorio garantiza el ordenamiento cronológico (Requirement 8.6)
        List<HealthEvent> healthEvents = healthEventRepository.findByAnimalId(animalId);
        
        log.debug("Eventos de salud encontrados: count={}, animalId={}", 
                  healthEvents.size(), animalId);
        
        // 3. Convertir entidades de dominio a DTOs de resultado
        List<HealthEventResult> results = healthEvents.stream()
            .map(HealthEventResult::fromDomain)
            .collect(Collectors.toList());
        
        log.info("Historial de salud consultado exitosamente: animalId={}, eventosCount={}", 
                 animalId, results.size());
        
        // 4. Retornar lista ordenada cronológicamente
        return results;
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
    private Animal validateAnimalExists(UUID animalId) {
        log.debug("Validando existencia de animal: animalId={}", animalId);
        
        return animalRepository.findById(animalId)
            .orElseThrow(() -> {
                log.warn("Animal no encontrado o no pertenece al tenant: animalId={}", animalId);
                return new AnimalNotFoundException("Animal no encontrado");
            });
    }
}
