package mx.vacapp.cattle.internal.domain.repository;

import mx.vacapp.cattle.internal.domain.model.WeightRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para persistencia de registros de peso (Repository Port Pattern).
 * 
 * Esta es una interfaz pura de dominio sin anotaciones de Spring/JPA.
 * Define el contrato para operaciones de persistencia de pesos de animales
 * sin acoplarse a ninguna tecnología específica
 * (Clean Architecture / Hexagonal Architecture).
 * 
 * La implementación concreta estará en la capa de infraestructura
 * (internal/infrastructure/persistence/impl/WeightRepositoryImpl.java)
 * 
 * IMPORTANTE: Los pesos permiten monitorear el crecimiento y engorda del ganado.
 * Cada animal puede tener múltiples registros de peso en diferentes fechas,
 * permitiendo calcular ganancia diaria entre mediciones consecutivas.
 */
public interface WeightRepository {
    
    /**
     * Persiste un registro de peso en el sistema.
     * 
     * @param weightRecord entidad de dominio WeightRecord a persistir
     * @return el registro de peso persistido con ID generado
     * @throws IllegalArgumentException si weightRecord es null
     * @throws IllegalArgumentException si peso_kg <= 0
     * @throws IllegalArgumentException si fecha_pesaje es posterior a la fecha actual
     */
    WeightRecord save(WeightRecord weightRecord);
    
    /**
     * Obtiene todos los registros de peso de un animal específico.
     * 
     * Retorna los pesos ordenados cronológicamente de más reciente a más antiguo
     * (fecha_pesaje DESC) para facilitar el cálculo de ganancia diaria.
     * 
     * @param animalId UUID del animal
     * @return lista ordenada de registros de peso (más reciente primero),
     *         lista vacía si el animal no tiene pesos registrados
     */
    List<WeightRecord> findByAnimalId(UUID animalId);
    
    /**
     * Obtiene el peso anterior más cercano a una fecha de pesaje específica.
     * 
     * Busca el registro de peso con fecha_pesaje inmediatamente anterior a la fecha dada
     * para el mismo animal. Útil para calcular ganancia diaria entre pesajes consecutivos.
     * 
     * @param animalId UUID del animal
     * @param fechaPesaje fecha de referencia (busca el peso inmediatamente anterior a esta fecha)
     * @return Optional con el peso anterior más cercano si existe,
     *         empty si no hay pesajes anteriores a la fecha especificada
     */
    Optional<WeightRecord> findPreviousWeight(UUID animalId, LocalDate fechaPesaje);
    
    /**
     * Obtiene el peso más reciente (último) de un animal.
     * 
     * Retorna el registro con la fecha_pesaje más reciente para el animal,
     * que representa el peso actual del animal.
     * 
     * @param animalId UUID del animal
     * @return Optional con el peso más reciente si existe,
     *         empty si el animal no tiene pesos registrados
     */
    Optional<WeightRecord> findLatestWeight(UUID animalId);
}
