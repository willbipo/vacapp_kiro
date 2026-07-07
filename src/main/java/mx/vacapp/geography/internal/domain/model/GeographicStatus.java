package mx.vacapp.geography.internal.domain.model;

/**
 * Enum que representa el estado de una entidad geográfica.
 * 
 * Los estados posibles son:
 * - ACTIVE: entidad activa y disponible para operaciones
 * - ARCHIVED: entidad archivada (soft delete), no disponible para operaciones
 */
public enum GeographicStatus {
    
    /**
     * Entidad activa y disponible para operaciones normales.
     */
    ACTIVE,
    
    /**
     * Entidad archivada (soft delete).
     * No está disponible para nuevas operaciones pero se preserva
     * en la base de datos para historial y auditoría.
     */
    ARCHIVED
}
