package mx.vacapp.cattle.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad de dominio que representa un registro de peso de un animal en una fecha específica.
 * 
 * Permite registrar múltiples pesos para un animal en diferentes fechas,
 * facilitando el monitoreo de crecimiento y engorda del ganado.
 * 
 * Esta es una entidad de dominio pura sin anotaciones de Spring/JPA.
 */
public class WeightRecord {
    
    private final UUID weightId;
    private final UUID animalId;
    private final BigDecimal pesoKg;
    private final LocalDate fechaPesaje;
    private final String notas;
    private final Instant createdAt;
    private final UUID createdBy;
    
    private WeightRecord(Builder builder) {
        this.weightId = builder.weightId;
        this.animalId = builder.animalId;
        this.pesoKg = builder.pesoKg;
        this.fechaPesaje = builder.fechaPesaje;
        this.notas = builder.notas;
        this.createdAt = builder.createdAt;
        this.createdBy = builder.createdBy;
    }
    
    /**
     * Crea un nuevo registro de peso.
     * 
     * @param animalId ID del animal
     * @param pesoKg peso en kilogramos (debe ser mayor que cero)
     * @param fechaPesaje fecha del pesaje (no puede ser futura)
     * @param notas observaciones opcionales
     * @param createdBy ID del usuario que crea el registro
     * @return nuevo registro de peso
     * @throws IllegalArgumentException si el peso es cero o negativo, o la fecha es futura
     */
    public static WeightRecord create(UUID animalId, BigDecimal pesoKg, 
                                     LocalDate fechaPesaje, String notas, UUID createdBy) {
        if (pesoKg == null || pesoKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El peso debe ser mayor que cero");
        }
        if (fechaPesaje.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de pesaje no puede ser futura");
        }
        
        Instant now = Instant.now();
        
        return new Builder()
            .weightId(UUID.randomUUID())
            .animalId(animalId)
            .pesoKg(pesoKg)
            .fechaPesaje(fechaPesaje)
            .notas(notas)
            .createdAt(now)
            .createdBy(createdBy)
            .build();
    }
    
    // Getters
    
    public UUID getWeightId() {
        return weightId;
    }
    
    public UUID getAnimalId() {
        return animalId;
    }
    
    public BigDecimal getPesoKg() {
        return pesoKg;
    }
    
    public LocalDate getFechaPesaje() {
        return fechaPesaje;
    }
    
    public String getNotas() {
        return notas;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public UUID getCreatedBy() {
        return createdBy;
    }
    
    /**
     * Builder para construir instancias de WeightRecord.
     */
    public static class Builder {
        private UUID weightId;
        private UUID animalId;
        private BigDecimal pesoKg;
        private LocalDate fechaPesaje;
        private String notas;
        private Instant createdAt;
        private UUID createdBy;
        
        public Builder weightId(UUID weightId) {
            this.weightId = weightId;
            return this;
        }
        
        public Builder animalId(UUID animalId) {
            this.animalId = animalId;
            return this;
        }
        
        public Builder pesoKg(BigDecimal pesoKg) {
            this.pesoKg = pesoKg;
            return this;
        }
        
        public Builder fechaPesaje(LocalDate fechaPesaje) {
            this.fechaPesaje = fechaPesaje;
            return this;
        }
        
        public Builder notas(String notas) {
            this.notas = notas;
            return this;
        }
        
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder createdBy(UUID createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        
        public WeightRecord build() {
            return new WeightRecord(this);
        }
    }
}
