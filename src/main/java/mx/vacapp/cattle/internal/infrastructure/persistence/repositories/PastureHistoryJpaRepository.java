package mx.vacapp.cattle.internal.infrastructure.persistence.repositories;

import mx.vacapp.cattle.internal.infrastructure.persistence.entities.PastureHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad PastureHistoryEntity.
 * 
 * Este repositorio gestiona el historial completo de movimientos de animales
 * entre potreros, proporcionando trazabilidad de ubicación temporal.
 * 
 * Conceptos clave:
 * - Un registro con fecha_salida = NULL representa la ubicación actual del animal
 * - El constraint UNIQUE (animal_id, fecha_salida) garantiza ubicación actual única
 * - El campo dias_permanencia es calculado automáticamente por MySQL
 */
public interface PastureHistoryJpaRepository extends JpaRepository<PastureHistoryEntity, UUID> {
    
    /**
     * Obtiene la ubicación actual de un animal (potrero donde está actualmente).
     * 
     * Retorna el registro de historial con fecha_salida = NULL, que representa
     * el potrero actual del animal. Un animal solo puede tener un registro activo
     * (sin fecha de salida) en un momento dado.
     * 
     * Útil para:
     * - Consultar dónde está un animal antes de moverlo
     * - Validar que un animal no esté en múltiples potreros simultáneamente
     * - Obtener potrero_actual para mostrar en la interfaz
     * 
     * @param animalId UUID del animal
     * @return Optional con el registro de ubicación actual, o empty si el animal
     *         no está en ningún potrero (caso raro: vendido/muerto sin cerrar historial)
     */
    Optional<PastureHistoryEntity> findByAnimalIdAndFechaSalidaIsNull(UUID animalId);
    
    /**
     * Obtiene el historial completo de movimientos de un animal ordenado cronológicamente.
     * 
     * Retorna todos los registros de movimientos del animal, desde el más reciente
     * al más antiguo (ORDER BY fecha_entrada DESC). Incluye:
     * - El registro actual (fecha_salida = NULL) si existe
     * - Todos los registros históricos con fecha_entrada y fecha_salida
     * 
     * Útil para:
     * - Mostrar línea de tiempo de movimientos en la interfaz
     * - Análisis de pastoreo rotacional
     * - Auditorías de trazabilidad de ubicación
     * 
     * @param animalId UUID del animal
     * @return lista de registros de historial ordenados de más reciente a más antiguo
     *         (lista vacía si el animal nunca ha sido asignado a un potrero)
     */
    List<PastureHistoryEntity> findByAnimalIdOrderByFechaEntradaDesc(UUID animalId);
    
    /**
     * Cierra la ubicación actual del animal estableciendo fecha_salida.
     * 
     * Ejecuta UPDATE en el registro con fecha_salida = NULL para el animal,
     * estableciendo fecha_salida = :fechaSalida. Esta operación "cierra" el
     * historial actual antes de mover el animal a un nuevo potrero.
     * 
     * Debe invocarse antes de insertar un nuevo registro de ubicación para
     * mantener la integridad: un animal solo puede tener una ubicación actual.
     * 
     * Flujo típico de movimiento:
     * 1. updateFechaSalidaByAnimalId(animalId, NOW()) - cierra ubicación actual
     * 2. save(new PastureHistoryEntity(..., fechaSalida = NULL)) - abre nueva ubicación
     * 
     * @param animalId UUID del animal cuya ubicación actual se cerrará
     * @param fechaSalida timestamp de salida del potrero (típicamente Instant.now())
     * @return cantidad de registros actualizados (0 si el animal no tenía ubicación
     *         actual, 1 si se cerró correctamente la ubicación)
     */
    @Modifying
    @Query("UPDATE PastureHistoryEntity p SET p.fechaSalida = :fechaSalida WHERE p.animalId = :animalId AND p.fechaSalida IS NULL")
    int updateFechaSalidaByAnimalId(@Param("animalId") UUID animalId, @Param("fechaSalida") Instant fechaSalida);
    
    /**
     * Lista todos los animales actualmente en un potrero específico.
     * 
     * Retorna registros con:
     * - potrero_id = :potreroId
     * - fecha_salida = NULL (animales aún en el potrero)
     * 
     * @param potreroId UUID del potrero
     * @return lista de registros de animales actualmente en el potrero
     */
    List<PastureHistoryEntity> findByPotreroIdAndFechaSalidaIsNull(UUID potreroId);
    
    /**
     * Cuenta cuántos animales están actualmente en un potrero.
     * 
     * @param potreroId UUID del potrero
     * @return cantidad de animales con ubicación actual en el potrero
     */
    long countByPotreroIdAndFechaSalidaIsNull(UUID potreroId);
}
