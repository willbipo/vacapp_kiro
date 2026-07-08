package mx.vacapp.cattle.internal.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad JPA que representa un animal bovino en la base de datos.
 * 
 * Esta entidad NO se usa en la capa de dominio. La transformación entre
 * AnimalEntity y Animal (dominio) se realiza mediante AnimalMapper.
 * 
 * El arete es único a nivel global (no por tenant) para prevenir duplicados
 * en futuras integraciones y cumplir con regulaciones de identificación.
 * 
 * Los enums (sexo, raza, status, tipo) se almacenan como STRING para mayor
 * legibilidad en la base de datos y facilitar queries directas.
 */
@Entity
@Table(name = "animals", 
    indexes = {
        @Index(name = "idx_animals_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_animals_rancho_id", columnList = "rancho_id"),
        @Index(name = "idx_animals_arete", columnList = "arete"),
        @Index(name = "idx_animals_status", columnList = "status"),
        @Index(name = "idx_animals_madre_id", columnList = "madre_id"),
        @Index(name = "idx_animals_padre_id", columnList = "padre_id"),
        @Index(name = "idx_animals_folio_reemo", columnList = "folio_reemo")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_animals_arete", columnNames = {"arete"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalEntity {
    
    // ========== Identificación ==========
    
    @Id
    @Column(name = "animal_id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID animalId;
    
    /**
     * Identificador único global del animal.
     * Único a nivel global (no por tenant) para prevenir duplicados.
     * Longitud: 4-20 caracteres alfanuméricos.
     */
    @Column(name = "arete", nullable = false, unique = true, length = 20)
    private String arete;
    
    /**
     * Arete anterior del animal (opcional).
     * Permite duplicados ya que es solo referencial.
     */
    @Column(name = "arete_anterior", length = 20)
    private String areteAnterior;
    
    // ========== Información Biológica ==========
    
    /**
     * Sexo del animal: macho o hembra.
     * Almacenado como STRING para legibilidad.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", nullable = false, length = 10)
    private SexEnum sexo;
    
    /**
     * Raza del animal.
     * Catálogo: Charolais, Angus, Brahman, Hereford, Simmental, Limousin,
     * Criollo, Brangus, Santa Gertrudis, Cruzada.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "raza", nullable = false, length = 50)
    private BreedEnum raza;
    
    /**
     * Fecha de nacimiento del animal.
     * No puede ser futura.
     */
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;
    
    /**
     * Edad del animal en meses completos.
     * Campo calculado automáticamente: TIMESTAMPDIFF(MONTH, fecha_nacimiento, CURRENT_DATE).
     * No se mapea directamente ya que es generado por la base de datos.
     */
    @Column(name = "meses", insertable = false, updatable = false)
    private Integer meses;
    
    /**
     * Fecha en que se colocó el arete al animal (opcional).
     * No puede ser anterior a fecha_nacimiento ni futura.
     */
    @Column(name = "fecha_aretado")
    private LocalDate fechaAretado;
    
    /**
     * Peso al nacer en kilogramos (opcional).
     * Se almacena por separado del historial de pesos.
     */
    @Column(name = "peso_nacimiento_kg", precision = 8, scale = 2)
    private BigDecimal pesoNacimientoKg;
    
    /**
     * Porcentaje de genética pura del animal (0-100).
     * Ejemplo: 75 para 75% de genética Charolais.
     */
    @Column(name = "porcentaje_genetica", precision = 5, scale = 2)
    private BigDecimal porcentajeGenetica;
    
    /**
     * Procedencia del animal (nombre del rancho de origen, región, etc.).
     */
    @Column(name = "procedencia", length = 200)
    private String procedencia;
    
    /**
     * Lote al que pertenece el animal.
     * Útil para agrupar animales por fecha de compra o nacimiento.
     */
    @Column(name = "lote", length = 50)
    private String lote;
    
    // ========== Clasificación Comercial ==========
    
    /**
     * Tipo comercial del animal.
     * Catálogo: Venta, Cría, Engorda, Semental, Vientre.
     * Valor por defecto: Venta.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    @Builder.Default
    private CattleTypeEnum tipo = CattleTypeEnum.VENTA;
    
    /**
     * Estado actual del animal.
     * Catálogo: Activa, Vendida, Muerta, Prestada, Preñada, En Reposo.
     * Valor por defecto: Activa.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CattleStatusEnum status = CattleStatusEnum.ACTIVA;
    
    // ========== Información Legal y Adicional ==========
    
    /**
     * Folio REEMO (Registro Electrónico de Movilización de México).
     * Opcional. Alfanumérico con guiones permitidos.
     * Múltiples animales pueden compartir el mismo folio (movilización en lote).
     */
    @Column(name = "folio_reemo", length = 50)
    private String folioReemo;
    
    /**
     * Folio SINIIGA (Sistema Nacional de Identificación Individual de Ganado).
     * Opcional. Identificador oficial del gobierno mexicano.
     */
    @Column(name = "folio_siniiga", length = 50)
    private String folioSiniiga;
    
    /**
     * Observaciones libres sobre el animal.
     * Campo de texto para información que no encaja en campos estructurados.
     */
    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
    
    // ========== Genealogía ==========
    
    /**
     * UUID de la madre del animal.
     * Debe existir en la tabla animals, tener sexo = Hembra, y pertenecer al mismo rancho.
     */
    @Column(name = "madre_id", columnDefinition = "BINARY(16)")
    private UUID madreId;
    
    /**
     * UUID del padre del animal.
     * Debe existir en la tabla animals, tener sexo = Macho, y pertenecer al mismo rancho.
     */
    @Column(name = "padre_id", columnDefinition = "BINARY(16)")
    private UUID padreId;
    
    // ========== Contexto Organizacional ==========
    
    /**
     * UUID del rancho al que pertenece el animal.
     * Referencia al módulo geographic-control.
     */
    @Column(name = "rancho_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID ranchoId;
    
    /**
     * UUID del tenant propietario del animal.
     * Se usa para filtrado multi-tenant en todas las consultas.
     */
    @Column(name = "tenant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID tenantId;
    
    // ========== Auditoría ==========
    
    /**
     * Timestamp de creación del registro.
     * Generado automáticamente por Hibernate.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * Timestamp de última actualización del registro.
     * Actualizado automáticamente por Hibernate.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * UUID del usuario que creó el registro.
     */
    @Column(name = "created_by", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID createdBy;
    
    /**
     * UUID del usuario que realizó la última actualización.
     */
    @Column(name = "updated_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID updatedBy;
    
    // ========== Campos de Venta (solo relevantes cuando status = Vendida) ==========
    
    /**
     * Fecha de venta del animal.
     * Requerido cuando status = Vendida.
     */
    @Column(name = "fecha_venta")
    private LocalDate fechaVenta;
    
    /**
     * Precio de venta del animal en la moneda local.
     * Opcional incluso cuando status = Vendida.
     */
    @Column(name = "precio_venta", precision = 12, scale = 2)
    private BigDecimal precioVenta;
    
    // ========== Campos de Muerte (solo relevantes cuando status = Muerta) ==========
    
    /**
     * Fecha de muerte del animal.
     * Requerido cuando status = Muerta.
     */
    @Column(name = "fecha_muerte")
    private LocalDate fechaMuerte;
    
    /**
     * Motivo de la muerte del animal.
     * Opcional. Ejemplos: "Enfermedad respiratoria", "Accidente", "Parto complicado".
     */
    @Column(name = "motivo_muerte", length = 500)
    private String motivoMuerte;
    
    // ========== Enums Internos ==========
    
    /**
     * Enum que representa el sexo del animal.
     */
    public enum SexEnum {
        MACHO("macho"),
        HEMBRA("hembra");
        
        private final String value;
        
        SexEnum(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    /**
     * Enum que representa la raza del animal.
     */
    public enum BreedEnum {
        CHAROLAIS("charolais"),
        ANGUS("angus"),
        BRAHMAN("brahman"),
        HEREFORD("hereford"),
        SIMMENTAL("simmental"),
        LIMOUSIN("limousin"),
        CRIOLLO("criollo"),
        BRANGUS("brangus"),
        SANTA_GERTRUDIS("santa_gertrudis"),
        CRUZADA("cruzada");
        
        private final String value;
        
        BreedEnum(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    /**
     * Enum que representa el estado del animal.
     */
    public enum CattleStatusEnum {
        ACTIVA("activa"),
        VENDIDA("vendida"),
        MUERTA("muerta"),
        PRESTADA("prestada"),
        PRENADA("prenada"),
        EN_REPOSO("en_reposo");
        
        private final String value;
        
        CattleStatusEnum(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    /**
     * Enum que representa el tipo comercial del animal.
     */
    public enum CattleTypeEnum {
        VENTA("venta"),
        CRIA("cria"),
        ENGORDA("engorda"),
        SEMENTAL("semental"),
        VIENTRE("vientre");
        
        private final String value;
        
        CattleTypeEnum(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}
