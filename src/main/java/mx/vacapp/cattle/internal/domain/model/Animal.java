package mx.vacapp.cattle.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad de dominio que representa un animal bovino.
 * Clase inmutable (todos los campos final) implementada con patrón Builder.
 * POJO puro sin anotaciones de Spring/JPA (Clean Architecture).
 * 
 * Esta es la entidad central del módulo cattle-inventory y representa
 * un bovino con su identificación única (arete), información biológica,
 * genealogía, ubicación, y estado comercial.
 */
public final class Animal {
    
    // Identificación
    private final UUID animalId;
    private final String arete;          // Identificador único global
    private final String areteAnterior;  // Arete previo (opcional)
    
    // Información biológica
    private final Sex sexo;
    private final Breed raza;
    private final LocalDate fechaNacimiento;
    private final Integer meses;         // Calculado desde fecha_nacimiento
    private final LocalDate fechaAretado;
    
    // Clasificación comercial
    private final CattleType tipo;
    private final CattleStatus status;
    
    // Información adicional
    private final String folioReemo;     // Opcional, regulación mexicana
    private final String nota;           // Observaciones libres
    
    // Genealogía
    private final UUID madreId;          // Nullable
    private final UUID padreId;          // Nullable
    
    // Contexto organizacional
    private final UUID ranchoId;
    private final UUID tenantId;
    
    // Auditoría
    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID createdBy;
    private final UUID updatedBy;
    
    // Campos de venta (solo relevantes cuando status = Vendida)
    private final LocalDate fechaVenta;
    private final BigDecimal precioVenta;
    
    // Campos de muerte (solo relevantes cuando status = Muerta)
    private final LocalDate fechaMuerte;
    private final String motivoMuerte;
    
    /**
     * Constructor privado. Usar Builder para construir instancias.
     */
    private Animal(Builder builder) {
        this.animalId = builder.animalId;
        this.arete = builder.arete;
        this.areteAnterior = builder.areteAnterior;
        this.sexo = builder.sexo;
        this.raza = builder.raza;
        this.fechaNacimiento = builder.fechaNacimiento;
        this.meses = builder.meses;
        this.fechaAretado = builder.fechaAretado;
        this.tipo = builder.tipo;
        this.status = builder.status;
        this.folioReemo = builder.folioReemo;
        this.nota = builder.nota;
        this.madreId = builder.madreId;
        this.padreId = builder.padreId;
        this.ranchoId = builder.ranchoId;
        this.tenantId = builder.tenantId;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
        this.fechaVenta = builder.fechaVenta;
        this.precioVenta = builder.precioVenta;
        this.fechaMuerte = builder.fechaMuerte;
        this.motivoMuerte = builder.motivoMuerte;
    }
    
    /**
     * Factory method para crear un nuevo animal.
     * Asigna valores por defecto: UUID aleatorio, status ACTIVA, tipo VENTA,
     * calcula edad en meses, normaliza arete a mayúsculas, y establece timestamps.
     * 
     * @param arete identificador único del animal (será convertido a mayúsculas)
     * @param sexo sexo del animal (Macho o Hembra)
     * @param raza raza del animal
     * @param fechaNacimiento fecha de nacimiento (no puede ser futura)
     * @param tipo tipo comercial (si es null, se asigna VENTA)
     * @param ranchoId UUID del rancho al que pertenece
     * @param tenantId UUID del tenant propietario
     * @param createdBy UUID del usuario que crea el registro
     * @return nueva instancia de Animal con valores por defecto
     */
    public static Animal create(String arete, Sex sexo, Breed raza, 
                               LocalDate fechaNacimiento, CattleType tipo,
                               UUID ranchoId, UUID tenantId, UUID createdBy) {
        Instant now = Instant.now();
        int meses = AgeCalculator.calculateMonths(fechaNacimiento, LocalDate.now());
        
        return new Builder()
            .animalId(UUID.randomUUID())
            .arete(arete.toUpperCase())
            .sexo(sexo)
            .raza(raza)
            .fechaNacimiento(fechaNacimiento)
            .meses(meses)
            .tipo(tipo != null ? tipo : CattleType.VENTA)
            .status(CattleStatus.ACTIVA)
            .ranchoId(ranchoId)
            .tenantId(tenantId)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
    }
    
    /**
     * Verifica si el animal está activo (presente físicamente en el rancho).
     * 
     * @return true si status es ACTIVA, PRENADA o EN_REPOSO
     */
    public boolean isActive() {
        return this.status == CattleStatus.ACTIVA || 
               this.status == CattleStatus.PRENADA || 
               this.status == CattleStatus.EN_REPOSO;
    }
    
    /**
     * Verifica si el animal está vendido o muerto.
     * 
     * @return true si status es VENDIDA o MUERTA
     */
    public boolean isSoldOrDead() {
        return this.status == CattleStatus.VENDIDA || 
               this.status == CattleStatus.MUERTA;
    }
    
    /**
     * Marca el animal como vendido.
     * Retorna una nueva instancia con status VENDIDA y los datos de venta.
     * 
     * @param fechaVenta fecha de la venta
     * @param precioVenta precio de venta (puede ser null)
     * @param updatedBy UUID del usuario que registra la venta
     * @return nueva instancia de Animal con status VENDIDA
     */
    public Animal markAsSold(LocalDate fechaVenta, BigDecimal precioVenta, UUID updatedBy) {
        return new Builder()
            .from(this)
            .status(CattleStatus.VENDIDA)
            .fechaVenta(fechaVenta)
            .precioVenta(precioVenta)
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();
    }
    
    /**
     * Marca el animal como muerto.
     * Retorna una nueva instancia con status MUERTA y los datos de muerte.
     * 
     * @param fechaMuerte fecha de la muerte
     * @param motivoMuerte motivo de la muerte (opcional)
     * @param updatedBy UUID del usuario que registra la muerte
     * @return nueva instancia de Animal con status MUERTA
     */
    public Animal markAsDead(LocalDate fechaMuerte, String motivoMuerte, UUID updatedBy) {
        return new Builder()
            .from(this)
            .status(CattleStatus.MUERTA)
            .fechaMuerte(fechaMuerte)
            .motivoMuerte(motivoMuerte)
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();
    }
    
    /**
     * Marca el animal como preñado.
     * Solo aplica a hembras. Lanza excepción si se intenta aplicar a macho.
     * 
     * @param updatedBy UUID del usuario que registra el cambio de estado
     * @return nueva instancia de Animal con status PRENADA
     * @throws IllegalArgumentException si el animal no es hembra
     */
    public Animal markAsPregnant(UUID updatedBy) {
        if (this.sexo != Sex.HEMBRA) {
            throw new IllegalArgumentException("Solo hembras pueden estar preñadas");
        }
        return new Builder()
            .from(this)
            .status(CattleStatus.PRENADA)
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();
    }
    
    /**
     * Actualiza el campo nota del animal.
     * 
     * @param nota nueva nota (puede ser null para limpiar)
     * @param updatedBy UUID del usuario que actualiza
     * @return nueva instancia de Animal con nota actualizada
     */
    public Animal updateNota(String nota, UUID updatedBy) {
        return new Builder()
            .from(this)
            .nota(nota)
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();
    }
    
    /**
     * Actualiza el tipo comercial del animal.
     * 
     * @param tipo nuevo tipo comercial
     * @param updatedBy UUID del usuario que actualiza
     * @return nueva instancia de Animal con tipo actualizado
     */
    public Animal updateTipo(CattleType tipo, UUID updatedBy) {
        return new Builder()
            .from(this)
            .tipo(tipo)
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();
    }
    
    /**
     * Actualiza el folio REEMO del animal.
     * 
     * @param folioReemo nuevo folio REEMO (puede ser null)
     * @param updatedBy UUID del usuario que actualiza
     * @return nueva instancia de Animal con folio actualizado
     */
    public Animal updateFolioReemo(String folioReemo, UUID updatedBy) {
        return new Builder()
            .from(this)
            .folioReemo(folioReemo)
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();
    }
    
    // Getters
    
    public UUID getAnimalId() {
        return animalId;
    }
    
    public String getArete() {
        return arete;
    }
    
    public String getAreteAnterior() {
        return areteAnterior;
    }
    
    public Sex getSexo() {
        return sexo;
    }
    
    public Breed getRaza() {
        return raza;
    }
    
    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }
    
    public Integer getMeses() {
        return meses;
    }
    
    public LocalDate getFechaAretado() {
        return fechaAretado;
    }
    
    public CattleType getTipo() {
        return tipo;
    }
    
    public CattleStatus getStatus() {
        return status;
    }
    
    public String getFolioReemo() {
        return folioReemo;
    }
    
    public String getNota() {
        return nota;
    }
    
    public UUID getMadreId() {
        return madreId;
    }
    
    public UUID getPadreId() {
        return padreId;
    }
    
    public UUID getRanchoId() {
        return ranchoId;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public UUID getCreatedBy() {
        return createdBy;
    }
    
    public UUID getUpdatedBy() {
        return updatedBy;
    }
    
    public LocalDate getFechaVenta() {
        return fechaVenta;
    }
    
    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }
    
    public LocalDate getFechaMuerte() {
        return fechaMuerte;
    }
    
    public String getMotivoMuerte() {
        return motivoMuerte;
    }
    
    /**
     * Builder para construir instancias de Animal de forma inmutable.
     * Soporta construcción desde cero o copia de instancia existente (método from).
     */
    public static final class Builder {
        private UUID animalId;
        private String arete;
        private String areteAnterior;
        private Sex sexo;
        private Breed raza;
        private LocalDate fechaNacimiento;
        private Integer meses;
        private LocalDate fechaAretado;
        private CattleType tipo;
        private CattleStatus status;
        private String folioReemo;
        private String nota;
        private UUID madreId;
        private UUID padreId;
        private UUID ranchoId;
        private UUID tenantId;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;
        private LocalDate fechaVenta;
        private BigDecimal precioVenta;
        private LocalDate fechaMuerte;
        private String motivoMuerte;
        
        public Builder() {
        }
        
        /**
         * Copia todos los campos de una instancia existente.
         * Útil para crear versiones modificadas de un animal existente.
         * 
         * @param animal instancia a copiar
         * @return este builder
         */
        public Builder from(Animal animal) {
            this.animalId = animal.animalId;
            this.arete = animal.arete;
            this.areteAnterior = animal.areteAnterior;
            this.sexo = animal.sexo;
            this.raza = animal.raza;
            this.fechaNacimiento = animal.fechaNacimiento;
            this.meses = animal.meses;
            this.fechaAretado = animal.fechaAretado;
            this.tipo = animal.tipo;
            this.status = animal.status;
            this.folioReemo = animal.folioReemo;
            this.nota = animal.nota;
            this.madreId = animal.madreId;
            this.padreId = animal.padreId;
            this.ranchoId = animal.ranchoId;
            this.tenantId = animal.tenantId;
            this.createdAt = animal.createdAt;
            this.updatedAt = animal.updatedAt;
            this.createdBy = animal.createdBy;
            this.updatedBy = animal.updatedBy;
            this.fechaVenta = animal.fechaVenta;
            this.precioVenta = animal.precioVenta;
            this.fechaMuerte = animal.fechaMuerte;
            this.motivoMuerte = animal.motivoMuerte;
            return this;
        }
        
        public Builder animalId(UUID animalId) {
            this.animalId = animalId;
            return this;
        }
        
        public Builder arete(String arete) {
            this.arete = arete;
            return this;
        }
        
        public Builder areteAnterior(String areteAnterior) {
            this.areteAnterior = areteAnterior;
            return this;
        }
        
        public Builder sexo(Sex sexo) {
            this.sexo = sexo;
            return this;
        }
        
        public Builder raza(Breed raza) {
            this.raza = raza;
            return this;
        }
        
        public Builder fechaNacimiento(LocalDate fechaNacimiento) {
            this.fechaNacimiento = fechaNacimiento;
            return this;
        }
        
        public Builder meses(Integer meses) {
            this.meses = meses;
            return this;
        }
        
        public Builder fechaAretado(LocalDate fechaAretado) {
            this.fechaAretado = fechaAretado;
            return this;
        }
        
        public Builder tipo(CattleType tipo) {
            this.tipo = tipo;
            return this;
        }
        
        public Builder status(CattleStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder folioReemo(String folioReemo) {
            this.folioReemo = folioReemo;
            return this;
        }
        
        public Builder nota(String nota) {
            this.nota = nota;
            return this;
        }
        
        public Builder madreId(UUID madreId) {
            this.madreId = madreId;
            return this;
        }
        
        public Builder padreId(UUID padreId) {
            this.padreId = padreId;
            return this;
        }
        
        public Builder ranchoId(UUID ranchoId) {
            this.ranchoId = ranchoId;
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
        
        public Builder fechaVenta(LocalDate fechaVenta) {
            this.fechaVenta = fechaVenta;
            return this;
        }
        
        public Builder precioVenta(BigDecimal precioVenta) {
            this.precioVenta = precioVenta;
            return this;
        }
        
        public Builder fechaMuerte(LocalDate fechaMuerte) {
            this.fechaMuerte = fechaMuerte;
            return this;
        }
        
        public Builder motivoMuerte(String motivoMuerte) {
            this.motivoMuerte = motivoMuerte;
            return this;
        }
        
        /**
         * Construye la instancia de Animal.
         * 
         * @return nueva instancia inmutable de Animal
         */
        public Animal build() {
            return new Animal(this);
        }
    }
}
