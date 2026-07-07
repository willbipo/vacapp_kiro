package mx.vacapp.geography.internal.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mx.vacapp.geography.internal.domain.model.GeographicStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que representa una sección de rancho en la base de datos.
 * 
 * Esta entidad NO se usa en la capa de dominio. La transformación entre
 * SeccionEntity y Seccion (dominio) se realiza mediante SeccionMapper.
 */
@Entity
@Table(name = "secciones", indexes = {
    @Index(name = "idx_secciones_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_secciones_rancho_id", columnList = "rancho_id"),
    @Index(name = "idx_secciones_status", columnList = "status"),
    @Index(name = "idx_secciones_rancho_tenant", columnList = "rancho_id, tenant_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeccionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "seccion_id", updatable = false, nullable = false)
    private UUID seccionId;
    
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
    
    @Column(name = "superficie", nullable = false, precision = 15, scale = 2)
    private BigDecimal superficie;
    
    @Column(name = "rancho_id", nullable = false)
    private UUID ranchoId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rancho_id", insertable = false, updatable = false)
    private RanchoEntity rancho;
    
    @Column(name = "descripcion", length = 500)
    private String descripcion;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GeographicStatus status;
    
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;
    
    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
