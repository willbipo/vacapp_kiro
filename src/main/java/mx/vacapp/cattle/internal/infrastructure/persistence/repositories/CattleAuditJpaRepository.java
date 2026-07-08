package mx.vacapp.cattle.internal.infrastructure.persistence.repositories;

import mx.vacapp.cattle.internal.infrastructure.persistence.entities.CattleAuditEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad CattleAuditEntity.
 * 
 * Este repositorio proporciona operaciones CRUD básicas a través de JpaRepository
 * y métodos de consulta personalizados para auditoría de cambios en el inventario.
 * 
 * Permite rastrear todas las modificaciones realizadas sobre animales:
 * - CREATE: Creación de un nuevo animal
 * - UPDATE: Actualización de datos del animal
 * - CHANGE_STATUS: Cambio de estado (Activa, Vendida, Muerta, etc.)
 * - MOVE_PASTURE: Movimiento entre potreros
 * - DELETE: Eliminación del animal
 * 
 * Los registros de auditoría se retienen por mínimo 730 días (2 años)
 * según los requisitos del sistema.
 */
public interface CattleAuditJpaRepository extends JpaRepository<CattleAuditEntity, UUID> {
    
    /**
     * Obtiene el historial completo de auditoría de un animal específico.
     * 
     * Retorna todos los registros de auditoría ordenados cronológicamente
     * desde el más reciente al más antiguo. Útil para visualizar la trazabilidad
     * completa de cambios en un animal.
     * 
     * @param animalId UUID del animal
     * @param pageable configuración de paginación (page, size, sort)
     * @return lista paginada de registros de auditoría ordenados por timestamp DESC
     */
    List<CattleAuditEntity> findByAnimalIdOrderByTimestampDesc(UUID animalId, Pageable pageable);
    
    /**
     * Filtra registros de auditoría por tipo de operación y tenant.
     * 
     * Permite obtener un subconjunto específico de operaciones para análisis
     * o reportes. Por ejemplo:
     * - MOVE_PASTURE: Historial de movimientos de ganado entre potreros
     * - CHANGE_STATUS: Cambios de estado (ventas, muertes, etc.)
     * - CREATE: Nuevos registros de animales
     * 
     * Los resultados se ordenan cronológicamente desde el más reciente.
     * 
     * @param operationType tipo de operación a filtrar (CREATE, UPDATE, CHANGE_STATUS, MOVE_PASTURE, DELETE)
     * @param tenantId UUID del tenant propietario
     * @param pageable configuración de paginación (page, size, sort)
     * @return lista paginada de registros de auditoría filtrados por operación y tenant
     */
    List<CattleAuditEntity> findByOperationTypeAndTenantIdOrderByTimestampDesc(
        CattleAuditEntity.OperationType operationType, 
        UUID tenantId, 
        Pageable pageable
    );
}
