package mx.vacapp.cattle.internal.domain.repository;

import mx.vacapp.cattle.internal.domain.model.PastureHistory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para persistencia de historial de movimientos entre potreros (Repository Port Pattern).
 * 
 * Esta es una interfaz pura de dominio sin anotaciones de Spring/JPA.
 * Define el contrato para operaciones de persistencia del historial de ubicación
 * de animales en potreros sin acoplarse a ninguna tecnología específica
 * (Clean Architecture / Hexagonal Architecture).
 * 
 * La implementación concreta estará en la capa de infraestructura
 * (internal/infrastructure/persistence/impl/PastureHistoryRepositoryImpl.java)
 * 
 * IMPORTANTE: El historial permite trazabilidad completa de movimientos.
 * Un animal puede tener múltiples registros históricos (fecha_salida != null)
 * pero solo un registro actual (fecha_salida = null) en cualquier momento.
 */
public interface PastureHistoryRepository {
    
    /**
     * Inserta un nuevo registro de entrada de animal a potrero.
     * 
     * Típicamente usado cuando:
     * - Un animal es registrado por primera vez y asignado a un potrero
     * - Un animal es movido de un potrero a otro (después de cerrar el registro anterior)
     * 
     * @param history registro de historial con fecha_entrada y fecha_salida = null
     * @return el registro persistido con ID generado
     * @throws IllegalArgumentException si el history es null
     * @throws IllegalStateException si ya existe un registro actual (fecha_salida = null) para el mismo animal
     */
    PastureHistory insert(PastureHistory history);
    
    /**
     * Busca el registro actual (ubicación actual) de un animal.
     * 
     * Retorna el registro donde fecha_salida = null, lo que indica
     * que el animal aún está en ese potrero.
     * 
     * @param animalId UUID del animal
     * @return Optional con el registro actual si el animal está en un potrero,
     *         empty si el animal no tiene ubicación actual (vendido/muerto)
     */
    Optional<PastureHistory> findCurrentByAnimalId(UUID animalId);
    
    /**
     * Actualiza la fecha de salida de un registro específico.
     * 
     * Usado cuando un animal sale de un potrero (por movimiento a otro potrero,
     * venta, muerte, etc.)
     * 
     * @param historyId UUID del registro a actualizar
     * @param fechaSalida fecha/hora de salida del potrero
     * @throws IllegalArgumentException si historyId es null o fechaSalida es null
     * @throws IllegalArgumentException si fechaSalida es anterior a fecha_entrada del registro
     * @throws IllegalStateException si el registro no existe
     */
    void updateFechaSalida(UUID historyId, Instant fechaSalida);
    
    /**
     * Obtiene el historial completo de movimientos de un animal.
     * 
     * Retorna todos los registros (actuales e históricos) ordenados cronológicamente
     * de más reciente a más antiguo (fecha_entrada DESC).
     * 
     * Incluye:
     * - Registro actual (fecha_salida = null) si existe
     * - Todos los registros históricos (fecha_salida != null)
     * 
     * @param animalId UUID del animal
     * @return lista ordenada de registros de historial (más reciente primero),
     *         lista vacía si el animal nunca ha estado en un potrero
     */
    List<PastureHistory> findHistoryByAnimalId(UUID animalId);
    
    /**
     * Lista todos los animales actualmente en un potrero específico.
     * 
     * Retorna los IDs de animales que tienen un registro con:
     * - potrero_id = potreroId especificado
     * - fecha_salida = null (aún están en el potrero)
     * 
     * @param potreroId UUID del potrero
     * @return lista de UUIDs de animales actualmente en el potrero,
     *         lista vacía si el potrero no tiene animales
     */
    List<UUID> findAnimalIdsByPotreroId(UUID potreroId);
    
    /**
     * Cuenta cuántos animales están actualmente en un potrero.
     * 
     * @param potreroId UUID del potrero
     * @return cantidad de animales con registro actual (fecha_salida = null) en el potrero
     */
    int countAnimalsInPasture(UUID potreroId);
}
