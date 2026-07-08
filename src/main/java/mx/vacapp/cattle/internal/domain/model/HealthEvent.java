package mx.vacapp.cattle.internal.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de dominio que representa un evento de salud de un animal.
 * 
 * Un evento de salud puede ser:
 * - Vacunación (VACCINATION): Aplicación de vacuna preventiva
 * - Tratamiento (TREATMENT): Medicación o procedimiento curativo
 * - Parto (BIRTH): Nacimiento de una cría (el animal es la madre)
 * - Diagnóstico (DIAGNOSIS): Evaluación veterinaria del estado de salud
 * 
 * POJO puro sin anotaciones de Spring/JPA (Clean Architecture).
 */
public class HealthEvent {
    private final UUID eventId;
    private final UUID animalId;
    private final HealthEventType tipoEvento;
    private final LocalDate fecha;
    private final String descripcion;
    private final String medicamento;
    private final String dosis;
    private final String veterinario;
    private final LocalDate proximaFecha;
    private final BigDecimal costo;
    private final String observaciones;
    private final LocalDateTime recordedAt;
    private final UUID recordedBy;
    
    private HealthEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.animalId = builder.animalId;
        this.tipoEvento = builder.tipoEvento;
        this.fecha = builder.fecha;
        this.descripcion = builder.descripcion;
        this.medicamento = builder.medicamento;
        this.dosis = builder.dosis;
        this.veterinario = builder.veterinario;
        this.proximaFecha = builder.proximaFecha;
        this.costo = builder.costo;
        this.observaciones = builder.observaciones;
        this.recordedAt = builder.recordedAt;
        this.recordedBy = builder.recordedBy;
    }
    
    /**
     * Crea un nuevo evento de salud.
     * 
     * @param animalId UUID del animal
     * @param tipoEvento tipo de evento
     * @param fecha fecha del evento
     * @param descripcion descripción del evento
     * @param recordedBy UUID del usuario que registra el evento
     * @return nuevo evento de salud
     */
    public static HealthEvent create(UUID animalId, HealthEventType tipoEvento, 
                                     LocalDate fecha, String descripcion, 
                                     UUID recordedBy) {
        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del evento no puede ser futura");
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        return new Builder()
            .eventId(UUID.randomUUID())
            .animalId(animalId)
            .tipoEvento(tipoEvento)
            .fecha(fecha)
            .descripcion(descripcion)
            .recordedAt(now)
            .recordedBy(recordedBy)
            .build();
    }
    
    /**
     * Verifica si este evento es de tipo vacunación.
     */
    public boolean isVacunacion() {
        return this.tipoEvento == HealthEventType.Vacunacion;
    }
    
    /**
     * Verifica si este evento es de tipo desparasitación.
     */
    public boolean isDesparasitacion() {
        return this.tipoEvento == HealthEventType.Desparasitacion;
    }
    
    /**
     * Verifica si este evento es de tipo tratamiento.
     */
    public boolean isTratamiento() {
        return this.tipoEvento == HealthEventType.Tratamiento;
    }
    
    /**
     * Verifica si este evento es de tipo diagnóstico.
     */
    public boolean isDiagnostico() {
        return this.tipoEvento == HealthEventType.Diagnostico;
    }
    
    /**
     * Verifica si este evento es de tipo cirugía.
     */
    public boolean isCirugia() {
        return this.tipoEvento == HealthEventType.Cirugia;
    }
    
    /**
     * Verifica si este evento es de tipo revisión.
     */
    public boolean isRevision() {
        return this.tipoEvento == HealthEventType.Revision;
    }
    
    // Getters
    
    public UUID getEventId() {
        return eventId;
    }
    
    public UUID getAnimalId() {
        return animalId;
    }
    
    public HealthEventType getTipoEvento() {
        return tipoEvento;
    }
    
    public LocalDate getFecha() {
        return fecha;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public String getMedicamento() {
        return medicamento;
    }
    
    public String getDosis() {
        return dosis;
    }
    
    public String getVeterinario() {
        return veterinario;
    }
    
    public LocalDate getProximaFecha() {
        return proximaFecha;
    }
    
    public BigDecimal getCosto() {
        return costo;
    }
    
    public String getObservaciones() {
        return observaciones;
    }
    
    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
    
    public UUID getRecordedBy() {
        return recordedBy;
    }
    
    // Builder Pattern
    
    public static class Builder {
        private UUID eventId;
        private UUID animalId;
        private HealthEventType tipoEvento;
        private LocalDate fecha;
        private String descripcion;
        private String medicamento;
        private String dosis;
        private String veterinario;
        private LocalDate proximaFecha;
        private BigDecimal costo;
        private String observaciones;
        private LocalDateTime recordedAt;
        private UUID recordedBy;
        
        public Builder eventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }
        
        public Builder animalId(UUID animalId) {
            this.animalId = animalId;
            return this;
        }
        
        public Builder tipoEvento(HealthEventType tipoEvento) {
            this.tipoEvento = tipoEvento;
            return this;
        }
        
        public Builder fecha(LocalDate fecha) {
            this.fecha = fecha;
            return this;
        }
        
        public Builder descripcion(String descripcion) {
            this.descripcion = descripcion;
            return this;
        }
        
        public Builder medicamento(String medicamento) {
            this.medicamento = medicamento;
            return this;
        }
        
        public Builder dosis(String dosis) {
            this.dosis = dosis;
            return this;
        }
        
        public Builder veterinario(String veterinario) {
            this.veterinario = veterinario;
            return this;
        }
        
        public Builder proximaFecha(LocalDate proximaFecha) {
            this.proximaFecha = proximaFecha;
            return this;
        }
        
        public Builder costo(BigDecimal costo) {
            this.costo = costo;
            return this;
        }
        
        public Builder observaciones(String observaciones) {
            this.observaciones = observaciones;
            return this;
        }
        
        public Builder recordedAt(LocalDateTime recordedAt) {
            this.recordedAt = recordedAt;
            return this;
        }
        
        public Builder recordedBy(UUID recordedBy) {
            this.recordedBy = recordedBy;
            return this;
        }
        
        public HealthEvent build() {
            if (animalId == null) {
                throw new IllegalArgumentException("animalId es requerido");
            }
            if (tipoEvento == null) {
                throw new IllegalArgumentException("tipoEvento es requerido");
            }
            if (fecha == null) {
                throw new IllegalArgumentException("fecha es requerida");
            }
            if (descripcion == null || descripcion.trim().isEmpty()) {
                throw new IllegalArgumentException("descripcion es requerida");
            }
            if (recordedBy == null) {
                throw new IllegalArgumentException("recordedBy es requerido");
            }
            
            return new HealthEvent(this);
        }
    }
}
