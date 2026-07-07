package mx.vacapp.geography.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad de dominio que representa un potrero (unidad mínima de pastoreo).
 * POJO puro sin anotaciones de Spring/JPA.
 * 
 * Un potrero es el nivel inferior en la jerarquía geográfica y es donde
 * se alberga físicamente el ganado. Puede estar vinculado directamente a un
 * rancho o a una sección.
 */
public class Potrero {
    private final UUID potreroId;
    private final String nombre;
    private final BigDecimal superficie;  // En metros cuadrados
    private final UUID ranchoId;
    private final UUID seccionId;  // Nullable - null si está vinculado directamente al rancho
    private final Integer cattleCount;  // Cantidad de ganado asignado
    private final String descripcion;
    private final GeographicStatus status;
    private final UUID tenantId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID createdBy;
    private final UUID updatedBy;
    
    // Constructor privado para forzar uso de factory methods
    private Potrero(Builder builder) {
        this.potreroId = builder.potreroId;
        this.nombre = builder.nombre;
        this.superficie = builder.superficie;
        this.ranchoId = builder.ranchoId;
        this.seccionId = builder.seccionId;
        this.cattleCount = builder.cattleCount;
        this.descripcion = builder.descripcion;
        this.status = builder.status;
        this.tenantId = builder.tenantId;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
    }
    
    /**
     * Factory method para crear nuevo potrero.
     * 
     * @param nombre nombre del potrero (será trimmeado)
     * @param superficie superficie en metros cuadrados
     * @param ranchoId ID del rancho padre
     * @param seccionId ID de la sección padre (null si vinculado directamente al rancho)
     * @param descripcion descripción opcional
     * @param tenantId ID del tenant propietario
     * @param createdBy ID del usuario que crea
     * @return nueva instancia de Potrero con status ACTIVE y cattleCount = 0
     */
    public static Potrero create(String nombre, BigDecimal superficie, UUID ranchoId,
                                UUID seccionId, String descripcion, UUID tenantId, 
                                UUID createdBy) {
        Instant now = Instant.now();
        return new Builder()
            .potreroId(UUID.randomUUID())
            .nombre(nombre.trim())
            .superficie(superficie)
            .ranchoId(ranchoId)
            .seccionId(seccionId)
            .cattleCount(0)  // Inicialmente sin ganado
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
     * Verifica si el potrero está activo.
     * 
     * @return true si el status es ACTIVE
     */
    public boolean isActive() {
        return this.status == GeographicStatus.ACTIVE;
    }
    
    /**
     * Verifica si el potrero está vinculado directamente al rancho.
     * 
     * @return true si seccionId es null
     */
    public boolean hasDirectRanchoLink() {
        return this.seccionId == null;
    }
    
    /**
     * Verifica si el potrero tiene ganado asignado.
     * 
     * @return true si cattleCount > 0
     */
    public boolean hasCattle() {
        return this.cattleCount > 0;
    }
    
    /**
     * Asigna ganado al potrero.
     * Retorna una nueva instancia con el conteo actualizado (inmutabilidad).
     * 
     * @param count cantidad de ganado a asignar (puede ser negativo para remover)
     * @param updatedBy ID del usuario que actualiza
     * @return nueva instancia con cattleCount actualizado
     */
    public Potrero assignCattle(int count, UUID updatedBy) {
        return new Builder()
            .from(this)
            .cattleCount(this.cattleCount + count)
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();
    }
    
    /**
     * Actualiza la superficie del potrero.
     * Retorna una nueva instancia (inmutabilidad).
     * 
     * @param newSuperficie nueva superficie en metros cuadrados
     * @param updatedBy ID del usuario que actualiza
     * @return nueva instancia con superficie actualizada
     */
    public Potrero updateSuperficie(BigDecimal newSuperficie, UUID updatedBy) {
        return new Builder()
            .from(this)
            .superficie(newSuperficie)
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();
    }
    
    /**
     * Actualiza el nombre y descripción del potrero.
     * Retorna una nueva instancia (inmutabilidad).
     * 
     * @param newNombre nuevo nombre
     * @param newDescripcion nueva descripción
     * @param updatedBy ID del usuario que actualiza
     * @return nueva instancia con datos actualizados
     */
    public Potrero updateInfo(String newNombre, String newDescripcion, UUID updatedBy) {
        return new Builder()
            .from(this)
            .nombre(newNombre.trim())
            .descripcion(newDescripcion)
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();
    }
    
    /**
     * Archiva el potrero (soft delete).
     * Retorna una nueva instancia con status ARCHIVED.
     * 
     * @param archivedBy ID del usuario que archiva
     * @return nueva instancia archivada
     */
    public Potrero archive(UUID archivedBy) {
        return new Builder()
            .from(this)
            .status(GeographicStatus.ARCHIVED)
            .updatedAt(Instant.now())
            .updatedBy(archivedBy)
            .build();
    }
    
    // Getters (sin setters - inmutabilidad)
    public UUID getPotreroId() { return potreroId; }
    public String getNombre() { return nombre; }
    public BigDecimal getSuperficie() { return superficie; }
    public UUID getRanchoId() { return ranchoId; }
    public UUID getSeccionId() { return seccionId; }
    public Integer getCattleCount() { return cattleCount; }
    public String getDescripcion() { return descripcion; }
    public GeographicStatus getStatus() { return status; }
    public UUID getTenantId() { return tenantId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    
    // Builder pattern para inmutabilidad
    public static class Builder {
        private UUID potreroId;
        private String nombre;
        private BigDecimal superficie;
        private UUID ranchoId;
        private UUID seccionId;
        private Integer cattleCount;
        private String descripcion;
        private GeographicStatus status;
        private UUID tenantId;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;
        
        public Builder potreroId(UUID potreroId) { 
            this.potreroId = potreroId; 
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
        
        public Builder seccionId(UUID seccionId) { 
            this.seccionId = seccionId; 
            return this; 
        }
        
        public Builder cattleCount(Integer cattleCount) { 
            this.cattleCount = cattleCount; 
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
         * Copia todos los valores desde un potrero existente.
         * Útil para crear versiones modificadas (inmutabilidad).
         */
        public Builder from(Potrero potrero) {
            this.potreroId = potrero.potreroId;
            this.nombre = potrero.nombre;
            this.superficie = potrero.superficie;
            this.ranchoId = potrero.ranchoId;
            this.seccionId = potrero.seccionId;
            this.cattleCount = potrero.cattleCount;
            this.descripcion = potrero.descripcion;
            this.status = potrero.status;
            this.tenantId = potrero.tenantId;
            this.createdAt = potrero.createdAt;
            this.updatedAt = potrero.updatedAt;
            this.createdBy = potrero.createdBy;
            this.updatedBy = potrero.updatedBy;
            return this;
        }
        
        public Potrero build() {
            return new Potrero(this);
        }
    }
}
