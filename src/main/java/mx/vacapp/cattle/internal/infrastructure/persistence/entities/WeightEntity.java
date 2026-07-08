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
 * Entidad JPA que representa un registro de peso de un animal en la base de datos.
 * 
 * Esta entidad NO se usa en la capa de dominio. La transformación entre
 * WeightEntity y WeightRecord (dominio) se realiza mediante WeightMapper.
 * 
 * Permite registrar múltiples pesos para un animal en diferentes fechas,
 * facilitando el monitoreo de crecimiento y engorda del ganado.
 */
@Entity
@Table(name = "cattle_weights", 
    indexes = {
        @Index(name = "idx_cattle_weights_animal_id", columnList = "animal_id"),
        @Index(name = "idx_cattle_weights_fecha_pesaje", columnList = "fecha_pesaje"),
        @Index(name = "idx_cattle_weights_animal_fecha", columnList = "animal_id, fecha_pesaje")
    }
    // Check constraint: peso_kg > 0 (managed in migration script V6__create_cattle_weights_table.sql)
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeightEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "weight_id", updatable = false, nullable = false)
    private UUID weightId;
    
    @Column(name = "animal_id", nullable = false)
    private UUID animalId;
    
    @Column(name = "peso_kg", nullable = false, precision = 8, scale = 2)
    private BigDecimal pesoKg;
    
    @Column(name = "fecha_pesaje", nullable = false)
    private LocalDate fechaPesaje;
    
    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;
    
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
}
