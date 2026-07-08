package mx.vacapp.cattle.internal.infrastructure.persistence.repositories;

import mx.vacapp.cattle.internal.infrastructure.persistence.entities.WeightEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad WeightEntity.
 * 
 * Este repositorio proporciona operaciones CRUD básicas a través de JpaRepository
 * y métodos de consulta personalizados siguiendo las convenciones de Spring Data JPA.
 * 
 * Permite registrar y consultar múltiples pesos para un animal en diferentes fechas,
 * facilitando el monitoreo de crecimiento y engorda del ganado. Los métodos de consulta
 * incluyen ordenación cronológica automática para facilitar el análisis de tendencias.
 * 
 * Casos de uso principales:
 * - Registrar peso actual del animal con fecha y notas opcionales
 * - Consultar historial completo de pesos ordenado cronológicamente
 * - Obtener peso más reciente para cálculo de peso_actual
 * - Obtener peso anterior a una fecha para cálculo de ganancia diaria
 */
public interface WeightJpaRepository extends JpaRepository<WeightEntity, UUID> {
    
    /**
     * Obtiene todos los pesos de un animal ordenados cronológicamente descendente.
     * 
     * Retorna el historial completo de pesos del animal, con el registro más reciente
     * primero. Esencial para visualizar la evolución del peso y calcular ganancias
     * diarias entre registros consecutivos.
     * 
     * @param animalId UUID del animal propietario de los registros de peso
     * @return lista de pesos ordenados por fecha_pesaje DESC (más reciente primero)
     */
    List<WeightEntity> findByAnimalIdOrderByFechaPesajeDesc(UUID animalId);
    
    /**
     * Obtiene el peso más reciente de un animal.
     * 
     * Método optimizado que retorna únicamente el registro de peso con la fecha_pesaje
     * más reciente. Útil para obtener el peso_actual sin cargar todo el historial.
     * Este valor se muestra en las vistas de lista de animales y en el detalle.
     * 
     * @param animalId UUID del animal
     * @return Optional con el peso más reciente si existe, empty si el animal no tiene pesos registrados
     */
    Optional<WeightEntity> findFirstByAnimalIdOrderByFechaPesajeDesc(UUID animalId);
    
    /**
     * Obtiene el peso anterior más cercano a una fecha específica.
     * 
     * Retorna el peso más reciente que fue registrado ANTES de la fecha especificada.
     * Esencial para calcular la ganancia diaria: (peso_actual - peso_anterior) / días_diferencia.
     * 
     * Ejemplo de uso:
     * - Animal pesó 450 kg el 2024-01-15
     * - Animal pesó 480 kg el 2024-02-15
     * - findFirstByAnimalIdAndFechaPesajeLessThanOrderByFechaPesajeDesc(id, 2024-02-15)
     *   → retorna registro del 2024-01-15 (450 kg)
     * - ganancia_diaria = (480 - 450) / 31 días = 0.97 kg/día
     * 
     * @param animalId UUID del animal
     * @param fechaPesaje fecha límite (no incluida, búsqueda estricta con <)
     * @return Optional con el peso anterior más cercano si existe, empty si no hay registros previos
     */
    Optional<WeightEntity> findFirstByAnimalIdAndFechaPesajeLessThanOrderByFechaPesajeDesc(
        UUID animalId, 
        LocalDate fechaPesaje
    );
}
