package mx.vacapp.cattle.internal.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Value Object que representa el historial de ubicación de un animal en potreros.
 * 
 * Registra cuando un animal entra y sale de un potrero específico,
 * permitiendo trazabilidad completa de movimientos entre potreros.
 * 
 * Un registro con fecha_salida = null representa la ubicación actual del animal.
 * 
 * Invariantes de dominio:
 * - Un animal puede tener múltiples registros históricos (fecha_salida != null)
 * - Un animal tiene máximo 1 registro actual (fecha_salida = null) en cualquier momento
 * - fecha_entrada siempre debe ser anterior o igual a fecha_salida
 * - dias_permanencia se calcula automáticamente según fechas
 */
public class PastureHistory {
    
    private final UUID historyId;
    private final UUID animalId;
    private final UUID potreroId;
    private final Instant fechaEntrada;
    private final Instant fechaSalida;     // null = ubicación actual
    private final Integer diasPermanencia;  // Calculado automáticamente
    private final Instant createdAt;
    private final UUID createdBy;
    
    /**
     * Constructor privado. Usar factory methods create() o restore().
     */
    private PastureHistory(Builder builder) {
        this.historyId = builder.historyId;
        this.animalId = builder.animalId;
        this.potreroId = builder.potreroId;
        this.fechaEntrada = builder.fechaEntrada;
        this.fechaSalida = builder.fechaSalida;
        this.diasPermanencia = builder.diasPermanencia;
        this.createdAt = builder.createdAt;
        this.createdBy = builder.createdBy;
    }
    
    /**
     * Factory method para crear un nuevo registro cuando un animal entra a un potrero.
     * 
     * @param animalId UUID del animal
     * @param potreroId UUID del potrero
     * @param createdBy UUID del usuario que registra el movimiento
     * @return nuevo registro con fecha_entrada = NOW y fecha_salida = null
     */
    public static PastureHistory create(UUID animalId, UUID potreroId, UUID createdBy) {
        Instant now = Instant.now();
        return new Builder()
                .historyId(UUID.randomUUID())
                .animalId(animalId)
                .potreroId(potreroId)
                .fechaEntrada(now)
                .fechaSalida(null)
                .diasPermanencia(null)
                .createdAt(now)
                .createdBy(createdBy)
                .build();
    }
    
    /**
     * Factory method para crear un registro con fecha de entrada específica.
     * Útil para importar datos históricos o registros retroactivos.
     * 
     * @param animalId UUID del animal
     * @param potreroId UUID del potrero
     * @param fechaEntrada fecha/hora de entrada al potrero
     * @param createdBy UUID del usuario que registra
     * @return nuevo registro con fecha_entrada especificada y fecha_salida = null
     */
    public static PastureHistory create(UUID animalId, UUID potreroId, Instant fechaEntrada, UUID createdBy) {
        return new Builder()
                .historyId(UUID.randomUUID())
                .animalId(animalId)
                .potreroId(potreroId)
                .fechaEntrada(fechaEntrada)
                .fechaSalida(null)
                .diasPermanencia(null)
                .createdAt(Instant.now())
                .createdBy(createdBy)
                .build();
    }
    
    /**
     * Factory method para restaurar un registro desde persistencia.
     * 
     * @param historyId UUID del registro
     * @param animalId UUID del animal
     * @param potreroId UUID del potrero
     * @param fechaEntrada fecha/hora de entrada
     * @param fechaSalida fecha/hora de salida (puede ser null)
     * @param diasPermanencia días calculados de permanencia
     * @param createdAt timestamp de creación del registro
     * @param createdBy UUID del usuario creador
     * @return registro restaurado desde persistencia
     */
    public static PastureHistory restore(UUID historyId, UUID animalId, UUID potreroId,
                                         Instant fechaEntrada, Instant fechaSalida,
                                         Integer diasPermanencia, Instant createdAt, UUID createdBy) {
        return new Builder()
                .historyId(historyId)
                .animalId(animalId)
                .potreroId(potreroId)
                .fechaEntrada(fechaEntrada)
                .fechaSalida(fechaSalida)
                .diasPermanencia(diasPermanencia)
                .createdAt(createdAt)
                .createdBy(createdBy)
                .build();
    }
    
    /**
     * Marca la salida del animal del potrero con fecha/hora actual.
     * 
     * @return nuevo objeto PastureHistory con fecha_salida = NOW
     */
    public PastureHistory markExit() {
        return markExit(Instant.now());
    }
    
    /**
     * Marca la salida del animal del potrero con fecha/hora específica.
     * 
     * @param fechaSalida fecha/hora de salida
     * @return nuevo objeto PastureHistory con fecha_salida especificada
     * @throws IllegalArgumentException si fechaSalida es anterior a fechaEntrada
     */
    public PastureHistory markExit(Instant fechaSalida) {
        if (fechaSalida.isBefore(this.fechaEntrada)) {
            throw new IllegalArgumentException("La fecha de salida no puede ser anterior a la fecha de entrada");
        }
        
        // Calcular días de permanencia
        long dias = java.time.Duration.between(this.fechaEntrada, fechaSalida).toDays();
        
        return new Builder()
                .historyId(this.historyId)
                .animalId(this.animalId)
                .potreroId(this.potreroId)
                .fechaEntrada(this.fechaEntrada)
                .fechaSalida(fechaSalida)
                .diasPermanencia((int) dias)
                .createdAt(this.createdAt)
                .createdBy(this.createdBy)
                .build();
    }
    
    /**
     * Verifica si este registro representa la ubicación actual del animal.
     * 
     * @return true si fecha_salida es null (animal aún está en este potrero)
     */
    public boolean isCurrent() {
        return this.fechaSalida == null;
    }
    
    // Getters
    
    public UUID getHistoryId() {
        return historyId;
    }
    
    public UUID getAnimalId() {
        return animalId;
    }
    
    public UUID getPotreroId() {
        return potreroId;
    }
    
    public Instant getFechaEntrada() {
        return fechaEntrada;
    }
    
    public Instant getFechaSalida() {
        return fechaSalida;
    }
    
    public Integer getDiasPermanencia() {
        return diasPermanencia;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public UUID getCreatedBy() {
        return createdBy;
    }
    
    // Builder Pattern
    
    public static class Builder {
        private UUID historyId;
        private UUID animalId;
        private UUID potreroId;
        private Instant fechaEntrada;
        private Instant fechaSalida;
        private Integer diasPermanencia;
        private Instant createdAt;
        private UUID createdBy;
        
        public Builder historyId(UUID historyId) {
            this.historyId = historyId;
            return this;
        }
        
        public Builder animalId(UUID animalId) {
            this.animalId = animalId;
            return this;
        }
        
        public Builder potreroId(UUID potreroId) {
            this.potreroId = potreroId;
            return this;
        }
        
        public Builder fechaEntrada(Instant fechaEntrada) {
            this.fechaEntrada = fechaEntrada;
            return this;
        }
        
        public Builder fechaSalida(Instant fechaSalida) {
            this.fechaSalida = fechaSalida;
            return this;
        }
        
        public Builder diasPermanencia(Integer diasPermanencia) {
            this.diasPermanencia = diasPermanencia;
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
        
        public PastureHistory build() {
            // Validaciones básicas
            if (animalId == null) {
                throw new IllegalArgumentException("animalId no puede ser null");
            }
            if (potreroId == null) {
                throw new IllegalArgumentException("potreroId no puede ser null");
            }
            if (fechaEntrada == null) {
                throw new IllegalArgumentException("fechaEntrada no puede ser null");
            }
            if (createdBy == null) {
                throw new IllegalArgumentException("createdBy no puede ser null");
            }
            
            return new PastureHistory(this);
        }
    }
    
    @Override
    public String toString() {
        return "PastureHistory{" +
                "historyId=" + historyId +
                ", animalId=" + animalId +
                ", potreroId=" + potreroId +
                ", fechaEntrada=" + fechaEntrada +
                ", fechaSalida=" + fechaSalida +
                ", diasPermanencia=" + diasPermanencia +
                ", createdAt=" + createdAt +
                ", createdBy=" + createdBy +
                '}';
    }
}
