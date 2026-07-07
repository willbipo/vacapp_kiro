package mx.vacapp.geography.internal.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida (interfaz) para registro de auditoría de cambios geográficos.
 * 
 * Registra todas las operaciones CREATE, UPDATE, ARCHIVE sobre entidades
 * geográficas (Rancho, Sección, Potrero).
 */
public interface GeographyAuditRepository {
    
    /**
     * Registra un evento de auditoría.
     * 
     * @param entityType tipo de entidad (RANCHO, SECCION, POTRERO)
     * @param entityId UUID de la entidad afectada
     * @param operationType tipo de operación (CREATE, UPDATE, ARCHIVE)
     * @param oldValues JSON con valores anteriores (null para CREATE)
     * @param newValues JSON con valores nuevos
     * @param modifiedBy UUID del usuario que realizó la operación
     * @param tenantId UUID del tenant
     * @param reason razón opcional del cambio (max 500 chars)
     */
    void logAuditEvent(
        String entityType,
        UUID entityId,
        String operationType,
        String oldValues,
        String newValues,
        UUID modifiedBy,
        UUID tenantId,
        String reason
    );
    
    /**
     * Obtiene el historial de auditoría de una entidad específica.
     * 
     * @param entityType tipo de entidad
     * @param entityId UUID de la entidad
     * @param tenantId UUID del tenant
     * @return lista de eventos de auditoría ordenados por timestamp desc
     */
    List<AuditEvent> findByEntityTypeAndEntityIdAndTenantId(
        String entityType,
        UUID entityId,
        UUID tenantId
    );
    
    /**
     * Obtiene el historial de auditoría filtrado por tipo de entidad.
     * 
     * @param entityType tipo de entidad
     * @param tenantId UUID del tenant
     * @param startDate fecha inicial (opcional)
     * @param endDate fecha final (opcional)
     * @param page número de página
     * @param size tamaño de página
     * @return lista de eventos de auditoría
     */
    List<AuditEvent> findByEntityTypeAndTenantId(
        String entityType,
        UUID tenantId,
        Instant startDate,
        Instant endDate,
        int page,
        int size
    );
    
    /**
     * Obtiene el historial completo de auditoría del tenant.
     * 
     * @param tenantId UUID del tenant
     * @param startDate fecha inicial (opcional)
     * @param endDate fecha final (opcional)
     * @param page número de página
     * @param size tamaño de página
     * @return lista de eventos de auditoría
     */
    List<AuditEvent> findByTenantId(
        UUID tenantId,
        Instant startDate,
        Instant endDate,
        int page,
        int size
    );
    
    /**
     * Record que representa un evento de auditoría.
     */
    record AuditEvent(
        UUID auditId,
        String entityType,
        UUID entityId,
        String operationType,
        Instant timestamp,
        UUID modifiedBy,
        UUID tenantId,
        String oldValues,
        String newValues,
        String reason
    ) {}
}
