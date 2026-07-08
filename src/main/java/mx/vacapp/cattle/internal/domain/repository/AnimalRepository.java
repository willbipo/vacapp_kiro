package mx.vacapp.cattle.internal.domain.repository;

import mx.vacapp.cattle.internal.domain.model.Animal;
import mx.vacapp.cattle.internal.domain.model.CattleStatus;
import mx.vacapp.cattle.internal.domain.model.CattleType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para persistencia de animales (Repository Port Pattern).
 * 
 * Esta es una interfaz pura de dominio sin anotaciones de Spring/JPA.
 * Define el contrato para operaciones de persistencia de animales sin acoplarse
 * a ninguna tecnología específica (Clean Architecture / Hexagonal Architecture).
 * 
 * La implementación concreta estará en la capa de infraestructura
 * (internal/infrastructure/persistence/impl/AnimalRepositoryImpl.java)
 * 
 * IMPORTANTE: Todos los métodos que retornan colecciones deben filtrar automáticamente
 * por tenant_id del contexto de seguridad para garantizar multi-tenancy.
 */
public interface AnimalRepository {
    
    /**
     * Persiste un animal en el sistema.
     * Si el animal ya existe (mismo animal_id), actualiza el registro.
     * 
     * @param animal entidad de dominio Animal a persistir
     * @return el animal persistido con datos actualizados
     * @throws IllegalArgumentException si el animal es null
     */
    Animal save(Animal animal);
    
    /**
     * Busca un animal por su identificador único.
     * 
     * @param animalId UUID del animal
     * @return Optional con el animal si existe y pertenece al tenant del contexto, empty en caso contrario
     */
    Optional<Animal> findById(UUID animalId);
    
    /**
     * Busca un animal por su número de arete.
     * Búsqueda case-insensitive.
     * 
     * @param arete número de arete del animal (será normalizado a mayúsculas)
     * @return Optional con el animal si existe y pertenece al tenant del contexto, empty en caso contrario
     */
    Optional<Animal> findByArete(String arete);
    
    /**
     * Verifica si existe un animal con el arete especificado.
     * Validación a nivel GLOBAL (no filtra por tenant_id) para garantizar unicidad de arete.
     * 
     * @param arete número de arete a verificar
     * @return true si existe un animal con ese arete en cualquier tenant, false en caso contrario
     */
    boolean existsByArete(String arete);
    
    /**
     * Lista todos los animales de un rancho con paginación.
     * Filtra automáticamente por tenant_id del contexto de seguridad.
     * 
     * @param tenantId UUID del tenant
     * @param ranchoId UUID del rancho
     * @param page número de página (0-based)
     * @param size tamaño de página (1-100)
     * @return lista paginada de animales del rancho
     */
    List<Animal> findAll(UUID tenantId, UUID ranchoId, int page, int size);
    
    /**
     * Cuenta el total de animales en un rancho.
     * Filtra automáticamente por tenant_id del contexto de seguridad.
     * 
     * @param tenantId UUID del tenant
     * @param ranchoId UUID del rancho
     * @return cantidad total de animales del rancho
     */
    long count(UUID tenantId, UUID ranchoId);
    
    /**
     * Lista animales por status específico.
     * Filtra automáticamente por tenant_id del contexto de seguridad.
     * 
     * @param status estado del animal (ACTIVA, VENDIDA, MUERTA, etc.)
     * @param tenantId UUID del tenant
     * @return lista de animales con el status especificado
     */
    List<Animal> findByStatus(CattleStatus status, UUID tenantId);
    
    /**
     * Lista todos los animales de un rancho sin paginación.
     * Filtra automáticamente por tenant_id del contexto de seguridad.
     * 
     * @param ranchoId UUID del rancho
     * @param tenantId UUID del tenant
     * @return lista completa de animales del rancho
     */
    List<Animal> findByRancho(UUID ranchoId, UUID tenantId);
    
    /**
     * Busca animales cuyo arete contenga la cadena de búsqueda especificada.
     * Búsqueda case-insensitive usando ILIKE/LOWER pattern matching.
     * Filtra automáticamente por tenant_id del contexto de seguridad.
     * 
     * @param tenantId UUID del tenant
     * @param areteQuery cadena de búsqueda (será normalizada a minúsculas)
     * @return lista de animales cuyo arete contiene la cadena de búsqueda, ordenada por arete ASC
     */
    List<Animal> findByAreteContaining(UUID tenantId, String areteQuery);
    
    /**
     * Lista animales aplicando filtros opcionales.
     * Filtra automáticamente por tenant_id del contexto de seguridad.
     * Los filtros que sean null no se aplicarán en la consulta.
     * 
     * @param tenantId UUID del tenant (requerido)
     * @param status filtro opcional por status del animal
     * @param tipo filtro opcional por tipo comercial del animal
     * @param ranchoId filtro opcional por rancho
     * @param potreroId filtro opcional por potrero actual (requiere JOIN con pasture_history)
     * @return lista de animales que cumplen con los filtros especificados, ordenada por arete ASC
     */
    List<Animal> findByFilters(UUID tenantId, 
                               CattleStatus status, 
                               CattleType tipo,
                               UUID ranchoId, 
                               UUID potreroId);
}
