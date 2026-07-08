package mx.vacapp.cattle.internal.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad JPA que representa un evento de salud de un animal en la base de datos.
 * 
 * Esta entidad NO se usa en la capa de dominio. La transformación entre
 * HealthEventEntity y HealthEvent (dominio) se realiza mediante HealthEventMapper.
 * 
 * Tipos de eventos soportados:
 * - VACCINATION: Vacunación del animal
 * - TREATMENT: Tratamiento médico
 * - BIRTH: Evento de parto (cría nacida)
 * - DIAGNOSIS: Diagnóstico veterinario
 */
@Entity
@Table(name = "health_events", 
    indexes = {
        @Index(name = "idx_health_events_animal_id", columnList = "animal_id"),
        @Index(name = "idx_health_events_event_type", columnList = "event_type"),
        @Index(name = "idx_health_events_veterinario_id", columnList = "veterinario_id"),
        @Index(name = "idx_health_events_fecha_evento", columnList = "fecha_evento"),
        @Index(name = "idx_health_events_animal_fecha", columnList = "animal_id, fecha_evento")
    },
    // Check constraint: costo >= 0 (managed in migration script V7__create_health_events_table.sql)
    uniqueConstraints = {}
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthEventEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", updatable = false, nullable = false)
    private UUID eventId;
    
    @Column(name = "animal_id", nullable = false)
    private UUID animalId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private EventType eventType;
    
    @Column(name = "fecha_evento", nullable = false)
    private LocalDate fechaEvento;
    
    @Column(name = "descripcion", nullable = false, length = 1000)
    private String descripcion;
    
    @Column(name = "costo", precision = 10, scale = 2)
    private BigDecimal costo;
    
    @Column(name = "veterinario_id")
    private UUID veterinarioId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    
    /**
     * Enum que representa los tipos de eventos de salud soportados.
     */
    public enum EventType {
        VACCINATION("vaccination"),
        TREATMENT("treatment"),
        BIRTH("birth"),
        DIAGNOSIS("diagnosis");
        
        private final String value;
        
        EventType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}
