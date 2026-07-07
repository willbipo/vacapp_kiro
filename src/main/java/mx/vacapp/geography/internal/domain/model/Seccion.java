package mx.vacapp.geography.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad de dominio que representa una sección de rancho.
 * POJO puro sin anotaciones de Spring/JPA.
 * 
 * Una sección es una división opcional de un rancho, utilizada típicamente
 * para terrenos grandes. Nivel intermedio en la jerarquía geográfica.
 */
public class Seccion {
    private final UUID seccionId;
    private final String nombre;
    private final BigDecimal superficie;  // En metros cuadrados
    private final UUID ranchoId;
    private final String descripcion;
    private final GeographicStatus status;
    private final UUID tenantId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID createdBy;
    private final UUID updatedBy;
    
    // Constructor privado para forzar uso de factory methods
    private Seccion(Builder builder) {
        this.seccionId = builder.seccionId;
        this.nombre = builder.nombre;
        this.superficie = builder.superficie;
        this.ranchoId = builder.ranchoId;
        this.descripcion = builder.descripcion;
        this.status = builder.status;
        this.tenantId = builder.tenantId;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
    }
    
    /**
     * Factory method para crear nueva sección.
     * 
     * @param nombre nombre de la sección (será trimmeado)
     * @param superficie superficie en metros cuadrados
     * @param ranchoId ID del rancho padre
     * @param descripcion descripción opcional
     * @param tenantId ID del tenant propietario
     * @param createdBy ID del usuario que crea
     * @return nueva instancia de Seccion con status ACTIVE
     */
    public static Seccion create(String nombre, BigDecimal superficie, UUID ranchoId,
                                String descripcion, UUID tenantId, UUID createdBy) {
        Instant now = Instant.now();
        return new Builder()
            .seccionId(UUID.randomUUID())
            .nombre(nombre.trim())
            .superficie(superficie)
            .ranchoId(ranchoId)
            .descripcion(descripcion)
            .status(GeographicStatus.ACTIVE)
            .tenantId(tenantId)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
    }
    
    /**
     * Verifica si la sección está activa.
     * 
     * @return true si el status es ACTIVE
     */
    public boolean isActive() {
        return this.status == GeographicStatus.ACTIVE;
    }
    
    /**
     * Actualiza la superficie de la sección.
     * Retorna una nueva instancia (inmutabilidad).
     * 
     * @param newSuperficie nueva superficie en metros cuadrados
     * @param updatedBy ID del usuario que actualiza
     * @return nueva instancia con superficie actualizada
     */
    public Seccion updateSuperficie(BigDecimal newSuperficie, UUID updatedBy) {
        return new Builder()
            .from(this)
            .superficie(newSuperficie)
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();
    }
    
    /**
     * Actualiza el nombre y descripción de la sección.
     * Retorna una nueva instancia (inmutabilidad).
     * 
     * @param newNombre nuevo nombre
     * @param newDescripcion nueva descripción
     * @param updatedBy ID del usuario que actualiza
     * @return nueva instancia con datos actualizados
     */
    public Seccion updateInfo(String newNombre, String newDescripcion, UUID updatedBy) {
        return new Builder()
            .from(this)
            .nombre(newNombre.trim())
            .descripcion(newDescripcion)
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();
    }
    
    /**
     * Archiva la sección (soft delete).
     * Retorna una nueva instancia con status ARCHIVED.
     * 
     * @param archivedBy ID del usuario que archiva
     * @return nueva instancia archivada
     */
    public Seccion archive(UUID archivedBy) {
        return new Builder()
            .from(this)
            .status(GeographicStatus.ARCHIVED)
            .updatedAt(Instant.now())
            .updatedBy(archivedBy)
            .build();
    }
    
    // Getters (sin setters - inmutabilidad)
    public UUID getSeccionId() { return seccionId; }
    public String getNombre() { return nombre; }
    public BigDecimal getSuperficie() { return superficie; }
    public UUID getRanchoId() { return ranchoId; }
    public String getDescripcion() { return descripcion; }
    public GeographicStatus getStatus() { return status; }
    public UUID getTenantId() { return tenantId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    
    // Builder pattern para inmutabilidad
    public static class Builder {
        private UUID seccionId;
        private String nombre;
        private BigDecimal superficie;
        private UUID ranchoId;
        private String descripcion;
        private GeographicStatus status;
        private UUID tenantId;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;
        
        public Builder seccionId(UUID seccionId) { 
            this.seccionId = seccionId; 
            return this; 
        }
        
        public Builder nombre(String nombre) { 
            this.nombre = nombre; 
            return this; 
        }
        
        public Builder superficie(BigDecimal superficie) { 
            this.superficie = superficie; 
            return this; 
        }
        
        public Builder ranchoId(UUID ranchoId) { 
            this.ranchoId = ranchoId; 
            return this; 
        }
        
        public Builder descripcion(String descripcion) { 
            this.descripcion = descripcion; 
            return this; 
        }
        
        public Builder status(GeographicStatus status) { 
            this.status = status; 
            return this; 
        }
        
        public Builder tenantId(UUID tenantId) { 
            this.tenantId = tenantId; 
            return this; 
        }
        
        public Builder createdAt(Instant createdAt) { 
            this.createdAt = createdAt; 
            return this; 
        }
        
        public Builder updatedAt(Instant updatedAt) { 
            this.updatedAt = updatedAt; 
            return this; 
        }
        
        public Builder createdBy(UUID createdBy) { 
            this.createdBy = createdBy; 
            return this; 
        }
        
        public Builder updatedBy(UUID updatedBy) { 
            this.updatedBy = updatedBy; 
            return this; 
        }
        
        /**
         * Copia todos los valores desde una sección existente.
         * Útil para crear versiones modificadas (inmutabilidad).
         */
        public Builder from(Seccion seccion) {
            this.seccionId = seccion.seccionId;
            this.nombre = seccion.nombre;
            this.superficie = seccion.superficie;
            this.ranchoId = seccion.ranchoId;
            this.descripcion = seccion.descripcion;
            this.status = seccion.status;
            this.tenantId = seccion.tenantId;
            this.createdAt = seccion.createdAt;
            this.updatedAt = seccion.updatedAt;
            this.createdBy = seccion.createdBy;
            this.updatedBy = seccion.updatedBy;
            return this;
        }
        
        public Seccion build() {
            return new Seccion(this);
        }
    }
}
