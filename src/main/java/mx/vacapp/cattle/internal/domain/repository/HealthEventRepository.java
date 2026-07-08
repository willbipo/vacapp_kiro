package mx.vacapp.cattle.internal.domain.repository;

import mx.vacapp.cattle.internal.domain.model.HealthEvent;
import mx.vacapp.cattle.internal.domain.model.HealthEventType;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida para persistencia de eventos de salud (Repository Port Pattern).
 * 
 * Esta es una interfaz pura de dominio sin anotaciones de Spring/JPA.
 * Define el contrato para operaciones de persistencia de eventos de salud
 * (vacunaciones, tratamientos médicos, partos, diagnósticos) sin acoplarse
 * a ninguna tecnología específica (Clean Architecture / Hexagonal Architecture).
 * 
 * La implementación concreta estará en la capa de infraestructura
 * (internal/infrastructure/persistence/impl/HealthEventRepositoryImpl.java)
 * 
 * IMPORTANTE: Todos los métodos deben validar que el animal pertenece al tenant_id
 * del contexto de seguridad para garantizar multi-tenancy.
 * 
 * Tipos de eventos soportados:
 * - VACCINATION: Vacunación del animal
 * - TREATMENT: Tratamiento médico
 * - BIRTH: Evento de parto (cría nacida)
 * - DIAGNOSIS: Diagnóstico veterinario
 */
public interface HealthEventRepository {
    
    /**
     * Persiste un evento de salud en el sistema.
     * 
     * @param healthEvent entidad de dominio HealthEvent a persistir
     * @return el evento persistido con datos actualizados (event_id generado)
     * @throws IllegalArgumentException si el healthEvent es null
     * @throws IllegalArgumentException si el animal_id no existe o no pertenece al tenant del contexto
     */
    HealthEvent save(HealthEvent healthEvent);
    
    /**
     * Busca todos los eventos de salud de un animal específico.
     * 
     * Retorna el historial completo ordenado cronológicamente de más reciente
     * a más antiguo (fecha_evento DESC).
     * 
     * Filtra automáticamente por tenant_id validando que el animal pertenece
     * al tenant del contexto de seguridad.
     * 
     * @param animalId UUID del animal
     * @return lista ordenada de eventos de salud (más reciente primero),
     *         lista vacía si el animal no tiene eventos de salud
     * @throws IllegalArgumentException si animalId es null
     */
    List<HealthEvent> findByAnimalId(UUID animalId);
    
    /**
     * Busca eventos de salud por tipo específico dentro de un tenant.
     * 
     * Filtra automáticamente por tenant_id del contexto de seguridad.
     * Útil para obtener solo vacunaciones, solo tratamientos, etc.
     * 
     * @param eventType tipo de evento (VACCINATION, TREATMENT, BIRTH, DIAGNOSIS)
     * @param tenantId UUID del tenant
     * @return lista de eventos del tipo especificado ordenados por fecha_evento DESC,
     *         lista vacía si no hay eventos de ese tipo
     * @throws IllegalArgumentException si eventType es null o tenantId es null
     */
    List<HealthEvent> findByEventType(HealthEventType eventType, UUID tenantId);
    
    /**
     * Busca vacunaciones próximas a vencer en los próximos N días.
     * 
     * Calcula próximas dosis basándose en:
     * - event_type = VACCINATION
     * - fecha_evento + intervalo_dias (almacenado en descripcion JSON)
     * - La fecha calculada cae dentro de los próximos daysAhead días
     * 
     * Filtra automáticamente por tenant_id del contexto de seguridad.
     * 
     * @param tenantId UUID del tenant
     * @param daysAhead cantidad de días hacia adelante para buscar vacunaciones próximas
     * @return lista de eventos de vacunación con próximas dosis próximas a vencer,
     *         lista vacía si no hay vacunaciones próximas
     * @throws IllegalArgumentException si tenantId es null o daysAhead < 0
     */
    List<HealthEvent> findUpcomingVaccinations(UUID tenantId, int daysAhead);
    
    /**
     * Busca eventos de parto (BIRTH) para una madre específica.
     * 
     * Retorna todos los eventos de tipo BIRTH donde animal_id corresponde
     * a la madre especificada, ordenados por fecha_evento DESC.
     * 
     * Útil para:
     * - Obtener historial de crías de una hembra
     * - Calcular intervalos entre partos
     * - Validar capacidad reproductiva
     * 
     * @param motherId UUID de la madre
     * @return lista de eventos de parto de la madre ordenados cronológicamente,
     *         lista vacía si la madre no tiene eventos de parto registrados
     * @throws IllegalArgumentException si motherId es null
     */
    List<HealthEvent> findBirthEventsByMotherId(UUID motherId);
}
