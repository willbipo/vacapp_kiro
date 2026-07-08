package mx.vacapp.cattle.internal.infrastructure.persistence.repositories;

import mx.vacapp.cattle.internal.infrastructure.persistence.entities.HealthEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad HealthEventEntity.
 * 
 * Este repositorio proporciona operaciones CRUD básicas a través de JpaRepository
 * y métodos de consulta personalizados siguiendo las convenciones de Spring Data JPA.
 * 
 * Los eventos de salud están asociados a animales, y el filtrado por tenant se realiza
 * a través de la relación con la tabla animals (JOIN). Esto garantiza el aislamiento
 * multi-tenant sin necesidad de duplicar tenant_id en la tabla health_events.
 * 
 * Tipos de eventos soportados:
 * - VACCINATION: Vacunación del animal con seguimiento de próximas dosis
 * - TREATMENT: Tratamiento médico con medicamento y dosis
 * - BIRTH: Evento de parto que crea automáticamente un nuevo animal (cría)
 * - DIAGNOSIS: Diagnóstico veterinario
 */
public interface HealthEventJpaRepository extends JpaRepository<HealthEventEntity, UUID> {
    
    /**
     * Obtiene todos los eventos de salud de un animal ordenados por fecha descendente.
     * 
     * Este método retorna el historial completo de salud del animal, desde el evento
     * más reciente hasta el más antiguo. Útil para visualizar la línea de tiempo
     * de eventos médicos, vacunaciones y partos.
     * 
     * Nota: El filtrado por tenant_id se debe realizar en la capa superior (UseCase)
     * validando que el animal pertenezca al tenant antes de llamar este método.
     * 
     * @param animalId UUID del animal
     * @return lista de eventos ordenados por fecha_evento DESC (más reciente primero)
     */
    List<HealthEventEntity> findByAnimalIdOrderByFechaEventoDesc(UUID animalId);
    
    /**
     * Filtra eventos de salud por tipo y los ordena por fecha descendente.
     * 
     * Permite obtener vistas específicas del historial de salud, por ejemplo:
     * - Solo vacunaciones (VACCINATION) para revisar esquemas de inmunización
     * - Solo tratamientos (TREATMENT) para auditoría de medicamentos
     * - Solo partos (BIRTH) para análisis reproductivo
     * 
     * IMPORTANTE: Este método NO filtra por tenant_id. Debe usarse con precaución
     * y siempre en combinación con validación de tenant en la capa de aplicación,
     * o preferiblemente usar el método con @Query que incluye filtrado por tenant.
     * 
     * @param eventType tipo de evento (VACCINATION, TREATMENT, BIRTH, DIAGNOSIS)
     * @return lista de eventos del tipo especificado ordenados por fecha_evento DESC
     */
    List<HealthEventEntity> findByEventTypeOrderByFechaEventoDesc(HealthEventEntity.EventType eventType);
    
    /**
     * Obtiene próximas vacunaciones pendientes basadas en intervalos de refuerzo.
     * 
     * Esta consulta calcula las vacunaciones que requieren refuerzo en los próximos
     * días especificados. Se basa en:
     * 1. Eventos de tipo VACCINATION
     * 2. Campo JSON en descripcion que contiene 'intervalo_dias'
     * 3. Cálculo: fecha_evento + intervalo_dias <= fecha_actual + dias_adelante
     * 
     * El filtrado por tenant_id se realiza mediante JOIN con la tabla animals
     * para garantizar que solo se retornen vacunaciones de animales del tenant.
     * 
     * Ejemplo de uso: Obtener vacunas que vencen en los próximos 30 días
     * para planificar jornadas de vacunación.
     * 
     * @param tenantId UUID del tenant propietario
     * @param diasAdelante cantidad de días hacia el futuro para la ventana de búsqueda (ej: 30)
     * @param fechaActual fecha de referencia (normalmente LocalDate.now())
     * @return lista de eventos de vacunación pendientes con fecha de próxima dosis calculada
     */
    @Query(value = """
        SELECT he.* 
        FROM health_events he
        JOIN animals a ON he.animal_id = a.animal_id
        WHERE he.event_type = 'VACCINATION'
        AND a.tenant_id = :tenantId
        AND a.status IN ('activa', 'prenada', 'en_reposo')
        AND JSON_EXTRACT(he.descripcion, '$.intervalo_dias') IS NOT NULL
        AND DATE_ADD(he.fecha_evento, INTERVAL CAST(JSON_EXTRACT(he.descripcion, '$.intervalo_dias') AS SIGNED) DAY) 
            BETWEEN :fechaActual AND DATE_ADD(:fechaActual, INTERVAL :diasAdelante DAY)
        ORDER BY he.fecha_evento DESC
        """, nativeQuery = true)
    List<HealthEventEntity> findUpcomingVaccinations(
        @Param("tenantId") UUID tenantId,
        @Param("diasAdelante") int diasAdelante,
        @Param("fechaActual") LocalDate fechaActual
    );
}
