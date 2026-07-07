package mx.vacapp.geography.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad de dominio que representa un rancho (terreno total).
 * POJO puro sin anotaciones de Spring/JPA.
 * 
 * Un rancho es el nivel superior en la jerarquía geográfica y puede contener
 * secciones o potreros directamente.
 */
public class Rancho {
    private final UUID ranchoId;
    private final String nombre;
    private final BigDecimal superficieTotal;  // En metros cuadrados
    private final String descripcion;
    private final GeographicStatus status;
    private final UUID tenantId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID createdBy;
    private final UUID updatedBy;
    
    // Constructor privado para forzar uso de factory methods
    private Rancho(Builder builder) {
        this.ranchoId = builder.ranchoId;
        this.nombre = builder.nombre;
        this.superficieTotal = builder.superficieTotal;
        this.descripcion = builder.descripcion;
        this.status = builder.status;
        this.tenantId = builder.tenantId;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
    }
    
    /**
     * Factory method para crear nuevo rancho.
     * 
     * @param nombre nombre del rancho (será trimmeado)
     * @param superficieTotal superficie total en metros cuadrados
     * @param descripcion descripción opcional
     * @param tenantId ID del tenant propietario
     * @param createdBy ID del usuario que crea
     * @return nueva instancia de Rancho con status ACTIVE
     */
    public static Rancho create(String nombre, BigDecimal superficieTotal, 
                               String descripcion, UUID tenantId, UUID createdBy) {
        Instant now = Instant.now();
        return new Builder()
            .ranchoId(UUID.randomUUID())
            .nombre(nombre.trim())
            .superficieTotal(superficieTotal)
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
     * Verifica si el rancho está activo.
     * 
     * @return true si el status es ACTIVE
     */
    public boolean isActive() {
        return this.status == GeographicStatus.ACTIVE;
    }
    
    /**
     * Actualiza la superficie total del rancho.
     * Retorna una nueva instancia (inmutabilidad).
     * 
     * @param newSuperficie nueva superficie en metros cuadrados
     * @param updatedBy ID del usuario que actualiza
     * @return nueva instancia con superficie actualizada
     */
    public Rancho updateSuperficie(BigDecimal newSuperficie, UUID updatedBy) {
        return new Builder()
            .from(this)
            .superficieTotal(newSuperficie)
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();
    }
    
    /**
     * Actualiza el nombre y descripción del rancho.
     * Retorna una nueva instancia (inmutabilidad).
     * 
     * @param newNombre nuevo nombre
     * @param newDescripcion nueva descripción
     * @param updatedBy ID del usuario que actualiza
     * @return nueva instancia con datos actualizados
     */
    public Rancho updateInfo(String newNombre, String newDescripcion, UUID updatedBy) {
        return new Builder()
            .from(this)
            .nombre(newNombre.trim())
            .descripcion(newDescripcion)
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();
    }
    
    /**
     * Archiva el rancho (soft delete).
     * Retorna una nueva instancia con status ARCHIVED.
     * 
     * @param archivedBy ID del usuario que archiva
     * @return nueva instancia archivada
     */
    public Rancho archive(UUID archivedBy) {
        return new Builder()
            .from(this)
            .status(GeographicStatus.ARCHIVED)
            .updatedAt(Instant.now())
            .updatedBy(archivedBy)
            .build();
    }
    
    // Getters (sin setters - inmutabilidad)
    public UUID getRanchoId() { return ranchoId; }
    public String getNombre() { return nombre; }
    public BigDecimal getSuperficieTotal() { return superficieTotal; }
    public String getDescripcion() { return descripcion; }
    public GeographicStatus getStatus() { return status; }
    public UUID getTenantId() { return tenantId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    
    // Builder pattern para inmutabilidad
    public static class Builder {
        private UUID ranchoId;
        private String nombre;
        private BigDecimal superficieTotal;
        private String descripcion;
        private GeographicStatus status;
        private UUID tenantId;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;
        
        public Builder ranchoId(UUID ranchoId) { 
            this.ranchoId = ranchoId; 
            return this; 
        }
        
        public Builder nombre(String nombre) { 
            this.nombre = nombre; 
            return this; 
        }
        
        public Builder superficieTotal(BigDecimal superficieTotal) { 
            this.superficieTotal = superficieTotal; 
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
         * Copia todos los valores desde un rancho existente.
         * Útil para crear versiones modificadas (inmutabilidad).
         */
        public Builder from(Rancho rancho) {
            this.ranchoId = rancho.ranchoId;
            this.nombre = rancho.nombre;
            this.superficieTotal = rancho.superficieTotal;
            this.descripcion = rancho.descripcion;
            this.status = rancho.status;
            this.tenantId = rancho.tenantId;
            this.createdAt = rancho.createdAt;
            this.updatedAt = rancho.updatedAt;
            this.createdBy = rancho.createdBy;
            this.updatedBy = rancho.updatedBy;
            return this;
        }
        
        public Rancho build() {
            return new Rancho(this);
        }
    }
}
