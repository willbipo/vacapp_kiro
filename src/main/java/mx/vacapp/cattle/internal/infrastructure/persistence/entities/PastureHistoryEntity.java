package mx.vacapp.cattle.internal.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que representa el historial de ubicación de un animal en potreros.
 * 
 * Esta entidad NO se usa en la capa de dominio. La transformación entre
 * PastureHistoryEntity y PastureHistory (dominio) se realiza mediante PastureHistoryMapper.
 * 
 * Registra cuando un animal entra y sale de un potrero específico, permitiendo
 * trazabilidad completa de movimientos entre potreros.
 * 
 * Un registro con fecha_salida = null representa la ubicación actual del animal.
 * 
 * Constraint UNIQUE (animal_id, fecha_salida) garantiza que un animal tenga
 * máximo 1 registro con fecha_salida = null (ubicación actual única).
 */
@Entity
@Table(name = "pasture_history",
    indexes = {
        @Index(name = "idx_pasture_history_animal_id", columnList = "animal_id"),
        @Index(name = "idx_pasture_history_potrero_id", columnList = "potrero_id"),
        @Index(name = "idx_pasture_history_current", columnList = "animal_id, fecha_salida")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_animal_current_pasture", columnNames = {"animal_id", "fecha_salida"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PastureHistoryEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "history_id", updatable = false, nullable = false)
    private UUID historyId;
    
    @Column(name = "animal_id", nullable = false)
    private UUID animalId;
    
    @Column(name = "potrero_id", nullable = false)
    private UUID potreroId;
    
    @Column(name = "fecha_entrada", nullable = false)
    private Instant fechaEntrada;
    
    /**
     * Fecha de salida del potrero.
     * NULL indica que el animal aún está en este potrero (ubicación actual).
     */
    @Column(name = "fecha_salida")
    private Instant fechaSalida;
    
    /**
     * Campo calculado MySQL GENERATED STORED.
     * Calcula días de permanencia:
     * - Si fecha_salida IS NULL: DATEDIFF(NOW(), fecha_entrada)
     * - Si fecha_salida IS NOT NULL: DATEDIFF(fecha_salida, fecha_entrada)
     * 
     * Se mapea con @Formula para leer el valor calculado por MySQL.
     * NO se persiste manualmente desde la aplicación.
     */
    @Formula("CASE WHEN fecha_salida IS NULL THEN DATEDIFF(CURRENT_TIMESTAMP, fecha_entrada) ELSE DATEDIFF(fecha_salida, fecha_entrada) END")
    @Column(name = "dias_permanencia", insertable = false, updatable = false)
    private Integer diasPermanencia;
    
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
