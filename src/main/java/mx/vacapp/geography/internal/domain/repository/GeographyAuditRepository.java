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
    
    // ========== MÉTODOS HELPER PARA AUDITORÍA ==========
    
    /**
     * Registra la creación de un Rancho.
     */
    default void logRanchoCreation(mx.vacapp.geography.internal.domain.model.Rancho rancho, UUID userId) {
        logAuditEvent(
            "RANCHO",
            rancho.getRanchoId(),
            "CREATE",
            null,
            String.format("{\"nombre\":\"%s\",\"superficie\":%s}", rancho.getNombre(), rancho.getSuperficieTotal()),
            userId,
            rancho.getTenantId(),
            "Rancho creado"
        );
    }
    
    /**
     * Registra la actualización de un Rancho.
     */
    default void logRanchoUpdate(mx.vacapp.geography.internal.domain.model.Rancho oldRancho,
                                  mx.vacapp.geography.internal.domain.model.Rancho newRancho,
                                  UUID userId) {
        logAuditEvent(
            "RANCHO",
            newRancho.getRanchoId(),
            "UPDATE",
            String.format("{\"nombre\":\"%s\",\"superficie\":%s}", oldRancho.getNombre(), oldRancho.getSuperficieTotal()),
            String.format("{\"nombre\":\"%s\",\"superficie\":%s}", newRancho.getNombre(), newRancho.getSuperficieTotal()),
            userId,
            newRancho.getTenantId(),
            "Rancho actualizado"
        );
    }
    
    /**
     * Registra el archivado de un Rancho.
     */
    default void logRanchoArchive(mx.vacapp.geography.internal.domain.model.Rancho oldRancho,
                                   mx.vacapp.geography.internal.domain.model.Rancho archivedRancho,
                                   UUID userId) {
        logAuditEvent(
            "RANCHO",
            archivedRancho.getRanchoId(),
            "ARCHIVE",
            String.format("{\"status\":\"%s\"}", oldRancho.getStatus()),
            String.format("{\"status\":\"%s\"}", archivedRancho.getStatus()),
            userId,
            archivedRancho.getTenantId(),
            "Rancho archivado"
        );
    }
    
    /**
     * Registra la creación de una Sección.
     */
    default void logSeccionCreation(mx.vacapp.geography.internal.domain.model.Seccion seccion, UUID userId) {
        logAuditEvent(
            "SECCION",
            seccion.getSeccionId(),
            "CREATE",
            null,
            String.format("{\"nombre\":\"%s\",\"superficie\":%s}", seccion.getNombre(), seccion.getSuperficie()),
            userId,
            seccion.getTenantId(),
            "Sección creada"
        );
    }
    
    /**
     * Registra la actualización de una Sección.
     */
    default void logSeccionUpdate(mx.vacapp.geography.internal.domain.model.Seccion oldSeccion,
                                   mx.vacapp.geography.internal.domain.model.Seccion newSeccion,
                                   UUID userId) {
        logAuditEvent(
            "SECCION",
            newSeccion.getSeccionId(),
            "UPDATE",
            String.format("{\"nombre\":\"%s\",\"superficie\":%s}", oldSeccion.getNombre(), oldSeccion.getSuperficie()),
            String.format("{\"nombre\":\"%s\",\"superficie\":%s}", newSeccion.getNombre(), newSeccion.getSuperficie()),
            userId,
            newSeccion.getTenantId(),
            "Sección actualizada"
        );
    }
    
    /**
     * Registra el archivado de una Sección.
     */
    default void logSeccionArchive(mx.vacapp.geography.internal.domain.model.Seccion oldSeccion,
                                    mx.vacapp.geography.internal.domain.model.Seccion archivedSeccion,
                                    UUID userId) {
        logAuditEvent(
            "SECCION",
            archivedSeccion.getSeccionId(),
            "ARCHIVE",
            String.format("{\"status\":\"%s\"}", oldSeccion.getStatus()),
            String.format("{\"status\":\"%s\"}", archivedSeccion.getStatus()),
            userId,
            archivedSeccion.getTenantId(),
            "Sección archivada"
        );
    }
    
    /**
     * Registra la creación de un Potrero.
     */
    default void logPotreroCreation(mx.vacapp.geography.internal.domain.model.Potrero potrero, UUID userId) {
        logAuditEvent(
            "POTRERO",
            potrero.getPotreroId(),
            "CREATE",
            null,
            String.format("{\"nombre\":\"%s\",\"superficie\":%s}", potrero.getNombre(), potrero.getSuperficie()),
            userId,
            potrero.getTenantId(),
            "Potrero creado"
        );
    }
    
    /**
     * Registra la actualización de un Potrero.
     */
    default void logPotreroUpdate(mx.vacapp.geography.internal.domain.model.Potrero oldPotrero,
                                   mx.vacapp.geography.internal.domain.model.Potrero newPotrero,
                                   UUID userId) {
        logAuditEvent(
            "POTRERO",
            newPotrero.getPotreroId(),
            "UPDATE",
            String.format("{\"nombre\":\"%s\",\"superficie\":%s}", oldPotrero.getNombre(), oldPotrero.getSuperficie()),
            String.format("{\"nombre\":\"%s\",\"superficie\":%s}", newPotrero.getNombre(), newPotrero.getSuperficie()),
            userId,
            newPotrero.getTenantId(),
            "Potrero actualizado"
        );
    }
    
    /**
     * Registra el archivado de un Potrero.
     */
    default void logPotreroArchive(mx.vacapp.geography.internal.domain.model.Potrero oldPotrero,
                                    mx.vacapp.geography.internal.domain.model.Potrero archivedPotrero,
                                    UUID userId) {
        logAuditEvent(
            "POTRERO",
            archivedPotrero.getPotreroId(),
            "ARCHIVE",
            String.format("{\"status\":\"%s\"}", oldPotrero.getStatus()),
            String.format("{\"status\":\"%s\"}", archivedPotrero.getStatus()),
            userId,
            archivedPotrero.getTenantId(),
            "Potrero archivado"
        );
    }
}
