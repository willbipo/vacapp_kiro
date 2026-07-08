# Design Document: Clinical History Module

## Overview

El módulo **clinical-history** es un componente core de Vacapp que proporciona funcionalidades completas para el registro, gestión y seguimiento de intervenciones veterinarias en el ganado. Este módulo implementa dos tipos de registros: **vacunaciones** (con integración transaccional al módulo vaccination-management para descuento automático de stock) y **tratamientos médicos generales** (desparasitaciones, antibióticos, cirugías, etc.). El módulo garantiza inmutabilidad de registros médicos, control de acceso basado en roles (solo veterinarios), cálculo automático de próximas dosis, y generación de reportes de salud.

### Contexto

Vacapp es un monolito modular construido con Spring Modulith siguiendo Clean Architecture. El módulo clinical-history debe implementarse después de user-management, cattle-inventory, y vaccination-management ya que depende de los tres para validaciones y operaciones transaccionales. Este módulo es fundamental para el seguimiento de salud del ganado y cumplimiento de protocolos veterinarios.

### Objetivos del Módulo

1. **Registro inmutable**: Historial clínico que no se puede modificar ni eliminar (integridad médica)
2. **Control de acceso**: Solo veterinarios autorizados pueden crear registros
3. **Integración transaccional**: Descuento automático de stock de vacunas de forma atómica
4. **Cálculo automático**: Próximas dosis calculadas según intervalo de refuerzo
5. **Reportes de salud**: Historial por animal, reportes por periodo, alertas de dosis vencidas
6. **Multi-tenancy**: Aislamiento completo de datos clínicos por organización
7. **Auditoría completa**: Registro de todas las intervenciones con trazabilidad

### Tecnologías

- **Backend**: Java 21, Spring Boot 4.1.0, Spring Modulith
- **Seguridad**: Spring Security 6 (integración con user-management)
- **Persistencia**: Spring Data JPA, MySQL 8
- **Validación**: Bean Validation (Hibernate Validator)
- **Transacciones**: Spring @Transactional con propagación REQUIRED
- **Caching**: Spring Cache con Caffeine
- **API Documentation**: OpenAPI 3.0 (Design-First con YAML)
- **Frontend Web**: Thymeleaf, HTML5, CSS Grid, JavaScript Vanilla
- **Utilidades**: Lombok, MapStruct (opcional para mappers)

## Architecture

### Arquitectura de Capas (Clean Architecture)

El módulo clinical-history sigue estrictamente la arquitectura hexagonal con separación de responsabilidades en tres capas principales:

```
mx.vacapp.clinicalhistory/
│
├── ClinicalHistoryService.java        ← API PÚBLICA (único punto de entrada para otros módulos)
│
└── internal/                          ← TODO lo demás es PRIVADO (inaccesible desde otros módulos)
    │
    ├── domain/                        ← Capa de Dominio (lógica de negocio pura)
    │   ├── model/                     ← Entidades de negocio (POJOs sin JPA)
    │   │   ├── ClinicalRecord.java     ← Entidad principal de registro clínico
    │   │   ├── VaccinationRecord.java  ← Registro específico de vacunación
    │   │   ├── TreatmentRecord.java    ← Registro específico de tratamiento
    │   │   ├── InterventionType.java   ← Enum (VACCINATION, TREATMENT)
    │   │   ├── TreatmentType.java      ← Enum (DESPARASITACION, ANTIBIOTICO, VITAMINAS, etc.)
    │   │   ├── ViaAdministracion.java  ← Enum (INTRAMUSCULAR, SUBCUTANEA, etc.)
    │   │   ├── NextDoseCalculator.java ← Value Object para cálculo de próximas dosis
    │   │   └── exceptions/            ← Excepciones de dominio
    │   │       ├── AnimalNotFoundException.java
    │   │       ├── AnimalInactiveException.java
    │   │       ├── VaccineNotFoundException.java
    │   │       ├── InsufficientStockException.java
    │   │       ├── ExpiredLotException.java
    │   │       ├── UnauthorizedRoleException.java
    │   │       └── FutureDateException.java
    │   │
    │   └── repository/                ← Puertos de salida (interfaces)
    │       ├── ClinicalRecordRepository.java
    │       ├── VaccinationRecordRepository.java
    │       ├── TreatmentRecordRepository.java
    │       ├── ClinicalAuditRepository.java
    │       ├── CattleServicePort.java          ← Puerto para integración con cattle-inventory
    │       ├── VaccinationServicePort.java     ← Puerto para integración con vaccination-management
    │       └── UserServicePort.java            ← Puerto para integración con user-management
    │
    ├── application/                   ← Capa de Aplicación (casos de uso)
    │   └── usecases/
    │       ├── vaccination/           ← Casos de uso de vacunaciones
    │       │   ├── RegisterVaccinationUseCase.java
    │       │   ├── GetVaccinationUseCase.java
    │       │   ├── ListVaccinationsByAnimalUseCase.java
    │       │   └── GetUpcomingDosesUseCase.java
    │       ├── treatment/             ← Casos de uso de tratamientos
    │       │   ├── RegisterTreatmentUseCase.java
    │       │   ├── GetTreatmentUseCase.java
    │       │   └── ListTreatmentsByAnimalUseCase.java
    │       ├── report/                ← Casos de uso de reportes
    │       │   ├── GenerateAnimalReportUseCase.java
    │       │   ├── GeneratePeriodReportUseCase.java
    │       │   ├── GetOverdueDosesUseCase.java
    │       │   └── GetUpcomingDosesAlertsUseCase.java
    │       └── commands/              ← Comandos y resultados (records)
    │           ├── RegisterVaccinationCommand.java
    │           ├── VaccinationResult.java
    │           ├── RegisterTreatmentCommand.java
    │           ├── TreatmentResult.java
    │           ├── AnimalReportResult.java
    │           ├── PeriodReportResult.java
    │           └── UpcomingDoseResult.java
    │
    └── infrastructure/                ← Capa de Infraestructura (adaptadores)
        │
        ├── controllers/               ← Adaptadores de entrada HTTP
        │   ├── mobile/                ← Controladores REST (API JSON)
        │   │   ├── VaccinationRestController.java
        │   │   ├── TreatmentRestController.java
        │   │   ├── ClinicalReportRestController.java
        │   │   ├── ClinicalAlertRestController.java
        │   │   └── dtos/              ← DTOs Request/Response (Records)
        │   │       ├── RegisterVaccinationRequest.java
        │   │       ├── VaccinationResponse.java
        │   │       ├── RegisterTreatmentRequest.java
        │   │       ├── TreatmentResponse.java
        │   │       ├── AnimalReportResponse.java
        │   │       ├── PeriodReportResponse.java
        │   │       └── UpcomingDoseResponse.java
        │   │
        │   └── web/                   ← Controladores MVC (Thymeleaf)
        │       ├── ClinicalHistoryWebController.java
        │       └── dtos/              ← Form DTOs (Records)
        │           ├── VaccinationFormDto.java
        │           └── TreatmentFormDto.java
        │
        ├── persistence/               ← Adaptador de salida (JPA)
        │   ├── entities/              ← Entidades JPA
        │   │   ├── ClinicalRecordEntity.java      ← @Entity con @Table("clinical_records")
        │   │   ├── VaccinationRecordEntity.java   ← @Entity con @Table("vaccination_records")
        │   │   ├── TreatmentRecordEntity.java     ← @Entity con @Table("treatment_records")
        │   │   └── ClinicalAuditEntity.java       ← @Entity con @Table("clinical_history_audit")
        │   ├── repositories/          ← Repositorios JPA
        │   │   ├── ClinicalRecordJpaRepository.java
        │   │   ├── VaccinationRecordJpaRepository.java
        │   │   ├── TreatmentRecordJpaRepository.java
        │   │   └── ClinicalAuditJpaRepository.java
        │   ├── impl/                  ← Implementaciones de puertos
        │   │   ├── ClinicalRecordRepositoryImpl.java
        │   │   ├── VaccinationRecordRepositoryImpl.java
        │   │   ├── TreatmentRecordRepositoryImpl.java
        │   │   └── ClinicalAuditRepositoryImpl.java
        │   └── mappers/               ← Transformación entre capas
        │       ├── ClinicalRecordMapper.java
        │       ├── VaccinationRecordMapper.java
        │       └── TreatmentRecordMapper.java
        │
        ├── integration/               ← Adaptadores de integración con otros módulos
        │   ├── CattleServiceAdapter.java         ← Implementa CattleServicePort
        │   ├── VaccinationServiceAdapter.java    ← Implementa VaccinationServicePort
        │   └── UserServiceAdapter.java           ← Implementa UserServicePort
        │
        ├── cache/                     ← Configuración de caching
        │   ├── CacheConfig.java       ← Configuración Caffeine
        │   └── CacheNames.java        ← Constantes de nombres de cache
        │
        └── config/                    ← Configuración del módulo
            ├── ClinicalHistoryModuleConfig.java  ← Beans del módulo
            └── OpenApiConfig.java                ← Configuración Swagger
```

### Flujo de Datos

#### Flujo de Registro de Vacunación con Descuento Automático de Stock

```
1. Veterinario envía POST /api/v1/clinical-history/vaccinations con {animal_id, vacuna_id, lote, dosis_aplicada, via_administracion, fecha_aplicacion}
   ↓
2. VaccinationRestController recibe RegisterVaccinationRequest, valida con @Valid
   ↓
3. VaccinationRestController extrae tenant_id y veterinario_id del JWT_Token
   ↓
4. VaccinationRestController verifica que usuario tiene rol VETERINARIO
   IF NOT VETERINARIO → retorna HTTP 403 "Acceso denegado"
   ↓
5. VaccinationRestController mapea a RegisterVaccinationCommand
   ↓
6. RegisterVaccinationUseCase.execute(RegisterVaccinationCommand) [INICIO TRANSACCIÓN]
   ↓
7. CattleServicePort.getAnimalById(animal_id) → valida existencia y status ACTIVA
   IF NOT EXISTS → THROW AnimalNotFoundException → rollback
   IF INACTIVE → THROW AnimalInactiveException → rollback
   ↓
8. VaccinationServicePort.getVaccinaById(vacuna_id) → obtiene datos de vacuna
   IF NOT EXISTS → THROW VaccineNotFoundException → rollback
   ↓
9. VaccinationServicePort.validateLoteStock(lote, dosis_aplicada) → valida stock y vencimiento
   IF INSUFFICIENT_STOCK → THROW InsufficientStockException → rollback
   IF EXPIRED → THROW ExpiredLotException → rollback
   ↓
10. VaccinationRecord.create(...) → crea entidad de dominio con:
    - intervention_type = VACCINATION
    - proxima_dosis = fecha_aplicacion + vacuna.intervalo_refuerzo_dias
    - desnormalización: nombre_vacuna, laboratorio, lote, fecha_vencimiento
    ↓
11. VaccinationRecordRepository.save(vaccinationRecord) → persiste
    ↓
12. VaccinationServicePort.decrementStock(lote, dosis_aplicada, record_id) → descuenta stock TRANSACCIONAL
    IF FAILS → propagate exception → rollback completo
    ↓
13. ClinicalAuditRepository.logVaccinationCreation(vaccinationRecord, veterinario_id, stock_antes, stock_despues) → auditoría
    ↓
14. [FIN TRANSACCIÓN - COMMIT si todo OK, ROLLBACK si cualquier paso falla]
    ↓
15. Retorna VaccinationResult con proxima_dosis calculada
    ↓
16. VaccinationRestController mapea a VaccinationResponse
    ↓
17. ResponseEntity.status(201).body(VaccinationResponse) → HTTP 201
```

#### Flujo de Registro de Tratamiento Médico General

```
1. Veterinario envía POST /api/v1/clinical-history/treatments con {animal_id, tipo_tratamiento, medicamento_nombre, dosis_aplicada, via_administracion, diagnostico, duracion_tratamiento_dias, costo_tratamiento, fecha_aplicacion}
   ↓
2. TreatmentRestController recibe RegisterTreatmentRequest, valida con @Valid
   ↓
3. TreatmentRestController extrae tenant_id y veterinario_id del JWT_Token
   ↓
4. TreatmentRestController verifica que usuario tiene rol VETERINARIO
   ↓
5. TreatmentRestController mapea a RegisterTreatmentCommand
   ↓
6. RegisterTreatmentUseCase.execute(RegisterTreatmentCommand) [INICIO TRANSACCIÓN]
   ↓
7. CattleServicePort.getAnimalById(animal_id) → valida existencia y status ACTIVA
   ↓
8. UserServicePort.getVeterinarianById(veterinario_id) → valida veterinario activo
   ↓
9. TreatmentRecord.create(...) → crea entidad de dominio con:
    - intervention_type = TREATMENT
    - fecha_fin_tratamiento = fecha_aplicacion + duracion_tratamiento_dias
    - desnormalización: veterinario_nombre, veterinario_cedula
    ↓
10. TreatmentRecordRepository.save(treatmentRecord) → persiste
    ↓
11. ClinicalAuditRepository.logTreatmentCreation(treatmentRecord, veterinario_id) → auditoría
    ↓
12. [FIN TRANSACCIÓN - COMMIT]
    ↓
13. Retorna TreatmentResult
    ↓
14. ResponseEntity.status(201).body(TreatmentResponse) → HTTP 201
```

#### Flujo de Generación de Reporte por Animal con Cache

```
1. Usuario envía GET /api/v1/clinical-history/reports/by-animal?animal_id={id}
   ↓
2. ClinicalReportRestController.getAnimalReport(animal_id)
   ↓
3. GenerateAnimalReportUseCase.execute(animal_id)
   ↓
4. Spring Cache verifica: existe en cache "report:animal:{id}:tenant:{tenantId}"?
   ↓
5. IF cache hit:
   |  Retornar AnimalReportResult desde cache (sin query DB)
   ELSE:
   |  CattleServicePort.getAnimalById(animal_id) → obtener info del animal
   |  VaccinationRecordRepository.findByAnimalId(animal_id) → obtener vacunaciones
   |  TreatmentRecordRepository.findByAnimalId(animal_id) → obtener tratamientos
   |  Construir AnimalReportResult con:
   |    - animal_info (identificador, nombre, tipo, edad)
   |    - total_vacunaciones, total_tratamientos
   |    - ultima_vacunacion, ultimo_tratamiento
   |    - proximas_dosis_pendientes[]
   |    - historial[] (vacunaciones + tratamientos ordenados por fecha desc)
   |  Almacenar en cache con TTL 10 minutos
   ↓
6. Retornar AnimalReportResult
   ↓
7. ClinicalReportRestController mapea a AnimalReportResponse
   ↓
8. ResponseEntity.ok(AnimalReportResponse) → HTTP 200
```

### Principios de Diseño

1. **Inmutabilidad de registros**: No se permite UPDATE ni DELETE de registros clínicos (integridad médica)
2. **Transaccionalidad**: Registro de vacunación + descuento de stock es operación atómica
3. **Fail-fast**: Validaciones tempranas antes de operaciones costosas
4. **Desnormalización intencional**: Copias de datos (nombre_vacuna, nombre_veterinario) para historial inmutable
5. **Control de acceso**: Solo veterinarios crean registros, otros roles solo leen
6. **Integración resiliente**: Si vaccination-management falla, rechazar vacunaciones pero permitir tratamientos
7. **Caching estratégico**: Reportes cacheados, invalidados al crear nuevos registros
8. **Auditoría completa**: Log de todas las operaciones con resultado de descuento de stock

## Components and Interfaces

### 1. API Pública del Módulo

#### ClinicalHistoryService.java

```java
package mx.vacapp.clinicalhistory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública del módulo de historial clínico.
 * Único punto de entrada para otros módulos de Vacapp.
 */
public interface ClinicalHistoryService {
    
    /**
     * Obtiene el conteo de vacunaciones de un animal.
     * 
     * @param animalId UUID del animal
     * @return cantidad de vacunaciones registradas
     */
    int getVaccinationCount(UUID animalId);
    
    /**
     * Obtiene el conteo de tratamientos de un animal.
     * 
     * @param animalId UUID del animal
     * @return cantidad de tratamientos registrados
     */
    int getTreatmentCount(UUID animalId);
    
    /**
     * Verifica si un animal tiene dosis pendientes vencidas.
     * 
     * @param animalId UUID del animal
     * @return true si tiene dosis vencidas
     */
    boolean hasOverdueDoses(UUID animalId);
    
    /**
     * Obtiene la última fecha de intervención de un animal.
     * 
     * @param animalId UUID del animal
     * @return Optional con la fecha de la última intervención
     */
    Optional<LocalDate> getLastInterventionDate(UUID animalId);
    
    /**
     * Obtiene las próximas dosis pendientes de un animal.
     * 
     * @param animalId UUID del animal
     * @return lista de próximas dosis con vacuna y fecha
     */
    List<UpcomingDose> getUpcomingDoses(UUID animalId);
}
```

### 2. Capa de Dominio

#### ClinicalRecord.java (Entidad Base de Dominio)

```java
package mx.vacapp.clinicalhistory.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad base de dominio para registros clínicos.
 * POJO puro sin anotaciones de Spring/JPA.
 * INMUTABLE - no se permite modificar después de creación.
 */
public abstract class ClinicalRecord {
    private final UUID recordId;
    private final UUID animalId;
    private final String animalNoIdentificador;  // Desnormalización
    private final String animalNombre;            // Desnormalización
    private final InterventionType interventionType;
    private final BigDecimal dosisAplicada;
    private final ViaAdministracion viaAdministracion;
    private final LocalDate fechaAplicacion;
    private final String notas;
    private final UUID aplicadoPor;  // Veterinario ID
    private final String veterinarioNombre;       // Desnormalización
    private final String veterinarioCedula;       // Desnormalización
    private final UUID tenantId;
    private final Instant createdAt;
    
    protected ClinicalRecord(Builder<?> builder) {
        this.recordId = builder.recordId;
        this.animalId = builder.animalId;
        this.animalNoIdentificador = builder.animalNoIdentificador;
        this.animalNombre = builder.animalNombre;
        this.interventionType = builder.interventionType;
        this.dosisAplicada = builder.dosisAplicada;
        this.viaAdministracion = builder.viaAdministracion;
        this.fechaAplicacion = builder.fechaAplicacion;
        this.notas = builder.notas;
        this.aplicadoPor = builder.aplicadoPor;
        this.veterinarioNombre = builder.veterinarioNombre;
        this.veterinarioCedula = builder.veterinarioCedula;
        this.tenantId = builder.tenantId;
        this.createdAt = builder.createdAt;
    }
    
    // Validaciones de negocio
    public void validateFechaAplicacion() {
        if (fechaAplicacion.isAfter(LocalDate.now())) {
            throw new FutureDateException("La fecha de aplicación no puede ser futura");
        }
    }
    
    public boolean isOlderThanOneYear() {
        return fechaAplicacion.isBefore(LocalDate.now().minusYears(1));
    }
    
    // Getters (sin setters - inmutabilidad)
    public UUID getRecordId() { return recordId; }
    public UUID getAnimalId() { return animalId; }
    public String getAnimalNoIdentificador() { return animalNoIdentificador; }
    public String getAnimalNombre() { return animalNombre; }
    public InterventionType getInterventionType() { return interventionType; }
    public BigDecimal getDosisAplicada() { return dosisAplicada; }
    public ViaAdministracion getViaAdministracion() { return viaAdministracion; }
    public LocalDate getFechaAplicacion() { return fechaAplicacion; }
    public String getNotas() { return notas; }
    public UUID getAplicadoPor() { return aplicadoPor; }
    public String getVeterinarioNombre() { return veterinarioNombre; }
    public String getVeterinarioCedula() { return veterinarioCedula; }
    public UUID getTenantId() { return tenantId; }
    public Instant getCreatedAt() { return createdAt; }
    
    // Builder genérico para herencia
    public static abstract class Builder<T extends Builder<T>> {
        private UUID recordId;
        private UUID animalId;
        private String animalNoIdentificador;
        private String animalNombre;
        private InterventionType interventionType;
        private BigDecimal dosisAplicada;
        private ViaAdministracion viaAdministracion;
        private LocalDate fechaAplicacion;
        private String notas;
        private UUID aplicadoPor;
        private String veterinarioNombre;
        private String veterinarioCedula;
        private UUID tenantId;
        private Instant createdAt;
        
        protected abstract T self();
        
        public T recordId(UUID recordId) { this.recordId = recordId; return self(); }
        public T animalId(UUID animalId) { this.animalId = animalId; return self(); }
        public T animalNoIdentificador(String animalNoIdentificador) { 
            this.animalNoIdentificador = animalNoIdentificador; return self(); 
        }
        public T animalNombre(String animalNombre) { 
            this.animalNombre = animalNombre; return self(); 
        }
        public T interventionType(InterventionType interventionType) { 
            this.interventionType = interventionType; return self(); 
        }
        public T dosisAplicada(BigDecimal dosisAplicada) { 
            this.dosisAplicada = dosisAplicada; return self(); 
        }
        public T viaAdministracion(ViaAdministracion viaAdministracion) { 
            this.viaAdministracion = viaAdministracion; return self(); 
        }
        public T fechaAplicacion(LocalDate fechaAplicacion) { 
            this.fechaAplicacion = fechaAplicacion; return self(); 
        }
        public T notas(String notas) { this.notas = notas; return self(); }
        public T aplicadoPor(UUID aplicadoPor) { 
            this.aplicadoPor = aplicadoPor; return self(); 
        }
        public T veterinarioNombre(String veterinarioNombre) { 
            this.veterinarioNombre = veterinarioNombre; return self(); 
        }
        public T veterinarioCedula(String veterinarioCedula) { 
            this.veterinarioCedula = veterinarioCedula; return self(); 
        }
        public T tenantId(UUID tenantId) { this.tenantId = tenantId; return self(); }
        public T createdAt(Instant createdAt) { 
            this.createdAt = createdAt; return self(); 
        }
    }
}
```

#### VaccinationRecord.java (Entidad de Dominio)

```java
package mx.vacapp.clinicalhistory.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad de dominio para registros de vacunación.
 * Extiende ClinicalRecord con campos específicos de vacunas.
 */
public class VaccinationRecord extends ClinicalRecord {
    private final UUID vacunaId;
    private final String nombreVacuna;          // Desnormalización
    private final String laboratorio;            // Desnormalización
    private final String lote;
    private final LocalDate fechaVencimientoLote; // Desnormalización
    private final LocalDate proximaDosis;        // Calculado automáticamente
    
    private VaccinationRecord(Builder builder) {
        super(builder);
        this.vacunaId = builder.vacunaId;
        this.nombreVacuna = builder.nombreVacuna;
        this.laboratorio = builder.laboratorio;
        this.lote = builder.lote;
        this.fechaVencimientoLote = builder.fechaVencimientoLote;
        this.proximaDosis = builder.proximaDosis;
    }
    
    // Factory method para crear nueva vacunación
    public static VaccinationRecord create(
        UUID animalId, String animalNoIdentificador, String animalNombre,
        UUID vacunaId, String nombreVacuna, String laboratorio, String lote,
        LocalDate fechaVencimientoLote, BigDecimal dosisAplicada,
        ViaAdministracion viaAdministracion, LocalDate fechaAplicacion,
        Integer intervaloRefuerzoDias, String notas, UUID veterinarioId,
        String veterinarioNombre, String veterinarioCedula, UUID tenantId
    ) {
        Instant now = Instant.now();
        LocalDate proximaDosis = NextDoseCalculator.calculate(
            fechaAplicacion, intervaloRefuerzoDias
        );
        
        VaccinationRecord record = new Builder()
            .recordId(UUID.randomUUID())
            .animalId(animalId)
            .animalNoIdentificador(animalNoIdentificador)
            .animalNombre(animalNombre)
            .interventionType(InterventionType.VACCINATION)
            .vacunaId(vacunaId)
            .nombreVacuna(nombreVacuna)
            .laboratorio(laboratorio)
            .lote(lote)
            .fechaVencimientoLote(fechaVencimientoLote)
            .dosisAplicada(dosisAplicada)
            .viaAdministracion(viaAdministracion)
            .fechaAplicacion(fechaAplicacion)
            .proximaDosis(proximaDosis)
            .notas(notas)
            .aplicadoPor(veterinarioId)
            .veterinarioNombre(veterinarioNombre)
            .veterinarioCedula(veterinarioCedula)
            .tenantId(tenantId)
            .createdAt(now)
            .build();
        
        record.validateFechaAplicacion();
        return record;
    }
    
    // Métodos de negocio
    public boolean isOverdue() {
        return proximaDosis != null && proximaDosis.isBefore(LocalDate.now());
    }
    
    public int getDaysUntilNextDose() {
        if (proximaDosis == null) return -1;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(
            LocalDate.now(), proximaDosis
        );
    }
    
    public boolean hasNextDose() {
        return proximaDosis != null;
    }
    
    // Getters
    public UUID getVacunaId() { return vacunaId; }
    public String getNombreVacuna() { return nombreVacuna; }
    public String getLaboratorio() { return laboratorio; }
    public String getLote() { return lote; }
    public LocalDate getFechaVencimientoLote() { return fechaVencimientoLote; }
    public LocalDate getProximaDosis() { return proximaDosis; }
    
    // Builder específico de VaccinationRecord
    public static class Builder extends ClinicalRecord.Builder<Builder> {
        private UUID vacunaId;
        private String nombreVacuna;
        private String laboratorio;
        private String lote;
        private LocalDate fechaVencimientoLote;
        private LocalDate proximaDosis;
        
        @Override
        protected Builder self() { return this; }
        
        public Builder vacunaId(UUID vacunaId) { 
            this.vacunaId = vacunaId; return this; 
        }
        public Builder nombreVacuna(String nombreVacuna) { 
            this.nombreVacuna = nombreVacuna; return this; 
        }
        public Builder laboratorio(String laboratorio) { 
            this.laboratorio = laboratorio; return this; 
        }
        public Builder lote(String lote) { this.lote = lote; return this; }
        public Builder fechaVencimientoLote(LocalDate fechaVencimientoLote) { 
            this.fechaVencimientoLote = fechaVencimientoLote; return this; 
        }
        public Builder proximaDosis(LocalDate proximaDosis) { 
            this.proximaDosis = proximaDosis; return this; 
        }
        
        public VaccinationRecord build() {
            return new VaccinationRecord(this);
        }
    }
}
```

#### TreatmentRecord.java (Entidad de Dominio)

```java
package mx.vacapp.clinicalhistory.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad de dominio para registros de tratamientos médicos generales.
 */
public class TreatmentRecord extends ClinicalRecord {
    private final TreatmentType tipoTratamiento;
    private final String medicamentoNombre;
    private final String diagnostico;
    private final Integer duracionTratamientoDias;
    private final LocalDate fechaFinTratamiento;
    private final BigDecimal costoTratamiento;
    
    private TreatmentRecord(Builder builder) {
        super(builder);
        this.tipoTratamiento = builder.tipoTratamiento;
        this.medicamentoNombre = builder.medicamentoNombre;
        this.diagnostico = builder.diagnostico;
        this.duracionTratamientoDias = builder.duracionTratamientoDias;
        this.fechaFinTratamiento = builder.fechaFinTratamiento;
        this.costoTratamiento = builder.costoTratamiento;
    }
    
    // Factory method
    public static TreatmentRecord create(
        UUID animalId, String animalNoIdentificador, String animalNombre,
        TreatmentType tipoTratamiento, String medicamentoNombre,
        BigDecimal dosisAplicada, ViaAdministracion viaAdministracion,
        String diagnostico, Integer duracionTratamientoDias,
        BigDecimal costoTratamiento, LocalDate fechaAplicacion,
        String notas, UUID veterinarioId, String veterinarioNombre,
        String veterinarioCedula, UUID tenantId
    ) {
        Instant now = Instant.now();
        LocalDate fechaFin = fechaAplicacion.plusDays(duracionTratamientoDias);
        
        TreatmentRecord record = new Builder()
            .recordId(UUID.randomUUID())
            .animalId(animalId)
            .animalNoIdentificador(animalNoIdentificador)
            .animalNombre(animalNombre)
            .interventionType(InterventionType.TREATMENT)
            .tipoTratamiento(tipoTratamiento)
            .medicamentoNombre(medicamentoNombre)
            .dosisAplicada(dosisAplicada)
            .viaAdministracion(viaAdministracion)
            .diagnostico(diagnostico)
            .duracionTratamientoDias(duracionTratamientoDias)
            .fechaFinTratamiento(fechaFin)
            .costoTratamiento(costoTratamiento)
            .fechaAplicacion(fechaAplicacion)
            .notas(notas)
            .aplicadoPor(veterinarioId)
            .veterinarioNombre(veterinarioNombre)
            .veterinarioCedula(veterinarioCedula)
            .tenantId(tenantId)
            .createdAt(now)
            .build();
        
        record.validateFechaAplicacion();
        record.validateDiagnostico();
        return record;
    }
    
    // Métodos de negocio
    public void validateDiagnostico() {
        if (tipoTratamiento == TreatmentType.OTRO && 
            (notas == null || notas.trim().length() < 10)) {
            throw new IllegalArgumentException(
                "Tratamientos de tipo OTRO requieren notas detalladas (mínimo 10 caracteres)"
            );
        }
    }
    
    public boolean isTreatmentFinished() {
        return fechaFinTratamiento.isBefore(LocalDate.now());
    }
    
    public int getDaysRemaining() {
        if (isTreatmentFinished()) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(
            LocalDate.now(), fechaFinTratamiento
        );
    }
    
    // Getters
    public TreatmentType getTipoTratamiento() { return tipoTratamiento; }
    public String getMedicamentoNombre() { return medicamentoNombre; }
    public String getDiagnostico() { return diagnostico; }
    public Integer getDuracionTratamientoDias() { return duracionTratamientoDias; }
    public LocalDate getFechaFinTratamiento() { return fechaFinTratamiento; }
    public BigDecimal getCostoTratamiento() { return costoTratamiento; }
    
    // Builder
    public static class Builder extends ClinicalRecord.Builder<Builder> {
        private TreatmentType tipoTratamiento;
        private String medicamentoNombre;
        private String diagnostico;
        private Integer duracionTratamientoDias;
        private LocalDate fechaFinTratamiento;
        private BigDecimal costoTratamiento;
        
        @Override
        protected Builder self() { return this; }
        
        public Builder tipoTratamiento(TreatmentType tipoTratamiento) { 
            this.tipoTratamiento = tipoTratamiento; return this; 
        }
        public Builder medicamentoNombre(String medicamentoNombre) { 
            this.medicamentoNombre = medicamentoNombre; return this; 
        }
        public Builder diagnostico(String diagnostico) { 
            this.diagnostico = diagnostico; return this; 
        }
        public Builder duracionTratamientoDias(Integer duracionTratamientoDias) { 
            this.duracionTratamientoDias = duracionTratamientoDias; return this; 
        }
        public Builder fechaFinTratamiento(LocalDate fechaFinTratamiento) { 
            this.fechaFinTratamiento = fechaFinTratamiento; return this; 
        }
        public Builder costoTratamiento(BigDecimal costoTratamiento) { 
            this.costoTratamiento = costoTratamiento; return this; 
        }
        
        public TreatmentRecord build() {
            return new TreatmentRecord(this);
        }
    }
}
```

### 3. Enums y Value Objects

#### InterventionType.java
```java
public enum InterventionType {
    VACCINATION,  // Vacunación con descuento de stock
    TREATMENT     // Tratamiento médico general
}
```

#### TreatmentType.java
```java
public enum TreatmentType {
    DESPARASITACION,
    ANTIBIOTICO,
    VITAMINAS,
    CIRUGIA,
    CURACION,
    OTRO
}
```

#### ViaAdministracion.java
```java
public enum ViaAdministracion {
    INTRAMUSCULAR,
    SUBCUTANEA,
    INTRAVENOSA,
    ORAL,
    TOPICA
}
```

#### NextDoseCalculator.java (Value Object)
```java
package mx.vacapp.clinicalhistory.internal.domain.model;

import java.time.LocalDate;

/**
 * Value Object para cálculo de próximas dosis de vacunas.
 */
public class NextDoseCalculator {
    
    /**
     * Calcula la fecha de la próxima dosis basándose en el intervalo de refuerzo.
     * 
     * @param fechaAplicacion fecha en que se aplicó la vacuna
     * @param intervaloRefuerzoDias días hasta el próximo refuerzo (puede ser null para dosis única)
     * @return fecha de la próxima dosis, o null si es dosis única
     */
    public static LocalDate calculate(LocalDate fechaAplicacion, Integer intervaloRefuerzoDias) {
        if (intervaloRefuerzoDias == null || intervaloRefuerzoDias <= 0) {
            return null;  // Vacuna de dosis única
        }
        
        LocalDate proximaDosis = fechaAplicacion.plusDays(intervaloRefuerzoDias);
        
        // Si la próxima dosis es más de 10 años en el futuro, considerarlo inválido
        if (proximaDosis.isAfter(LocalDate.now().plusYears(10))) {
            return null;
        }
        
        return proximaDosis;
    }
    
    /**
     * Calcula días restantes hasta la próxima dosis.
     * 
     * @param proximaDosis fecha de la próxima dosis
     * @return días restantes (negativo si está vencida)
     */
    public static int calculateDaysRemaining(LocalDate proximaDosis) {
        if (proximaDosis == null) return -1;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(
            LocalDate.now(), proximaDosis
        );
    }
    
    /**
     * Verifica si una dosis está vencida.
     * 
     * @param proximaDosis fecha de la próxima dosis
     * @return true si está vencida
     */
    public static boolean isOverdue(LocalDate proximaDosis) {
        return proximaDosis != null && proximaDosis.isBefore(LocalDate.now());
    }
}
```

## Database Schema

### Tabla: clinical_records (Tabla base)

```sql
CREATE TABLE clinical_records (
    record_id BINARY(16) PRIMARY KEY,
    animal_id BINARY(16) NOT NULL,
    animal_no_identificador VARCHAR(50) NOT NULL,
    animal_nombre VARCHAR(200),
    intervention_type ENUM('VACCINATION', 'TREATMENT') NOT NULL,
    dosis_aplicada DECIMAL(10,2) NOT NULL,
    via_administracion ENUM('INTRAMUSCULAR', 'SUBCUTANEA', 'INTRAVENOSA', 'ORAL', 'TOPICA') NOT NULL,
    fecha_aplicacion DATE NOT NULL,
    notas TEXT,
    aplicado_por BINARY(16) NOT NULL,
    veterinario_nombre VARCHAR(200) NOT NULL,
    veterinario_cedula VARCHAR(50),
    tenant_id BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_animal_id (animal_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_fecha_aplicacion (fecha_aplicacion),
    INDEX idx_aplicado_por (aplicado_por),
    INDEX idx_intervention_type (intervention_type),
    INDEX idx_composite_animal_tenant (animal_id, tenant_id),
    INDEX idx_composite_fecha_tenant (fecha_aplicacion, tenant_id)
);
```

### Tabla: vaccination_records (Herencia - vacunaciones)

```sql
CREATE TABLE vaccination_records (
    record_id BINARY(16) PRIMARY KEY,
    vacuna_id BINARY(16) NOT NULL,
    nombre_vacuna VARCHAR(200) NOT NULL,
    laboratorio VARCHAR(200),
    lote VARCHAR(100) NOT NULL,
    fecha_vencimiento_lote DATE NOT NULL,
    proxima_dosis DATE,
    
    FOREIGN KEY (record_id) REFERENCES clinical_records(record_id),
    INDEX idx_vacuna_id (vacuna_id),
    INDEX idx_proxima_dosis (proxima_dosis),
    INDEX idx_lote (lote)
);
```

### Tabla: treatment_records (Herencia - tratamientos)

```sql
CREATE TABLE treatment_records (
    record_id BINARY(16) PRIMARY KEY,
    tipo_tratamiento ENUM('DESPARASITACION', 'ANTIBIOTICO', 'VITAMINAS', 'CIRUGIA', 'CURACION', 'OTRO') NOT NULL,
    medicamento_nombre VARCHAR(200) NOT NULL,
    diagnostico TEXT NOT NULL,
    duracion_tratamiento_dias INT NOT NULL,
    fecha_fin_tratamiento DATE NOT NULL,
    costo_tratamiento DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    
    FOREIGN KEY (record_id) REFERENCES clinical_records(record_id),
    INDEX idx_tipo_tratamiento (tipo_tratamiento),
    INDEX idx_fecha_fin (fecha_fin_tratamiento)
);
```

### Tabla: clinical_history_audit (Auditoría)

```sql
CREATE TABLE clinical_history_audit (
    audit_id BINARY(16) PRIMARY KEY,
    entity_type ENUM('VACCINATION', 'TREATMENT') NOT NULL,
    entity_id BINARY(16) NOT NULL,
    operation_type ENUM('CREATE') NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    new_values JSON NOT NULL,
    stock_antes INT,
    stock_despues INT,
    lote_id VARCHAR(100),
    
    INDEX idx_entity_id (entity_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_modified_by (modified_by)
);
```

## Integration Contracts

### CattleServicePort (Puerto para cattle-inventory)

```java
public interface CattleServicePort {
    AnimalInfo getAnimalById(UUID animalId);
    void validateAnimalActive(UUID animalId);
}

record AnimalInfo(
    UUID animalId,
    String noIdentificador,
    String nombre,
    String tipo,
    String status,
    UUID tenantId
) {}
```

### VaccinationServicePort (Puerto para vaccination-management)

```java
public interface VaccinationServicePort {
    VaccineInfo getVaccinaById(UUID vacunaId);
    void validateLoteStock(String lote, BigDecimal dosisRequerida);
    void decrementStock(String lote, BigDecimal dosis, UUID recordId);
}

record VaccineInfo(
    UUID vacunaId,
    String nombreComercial,
    String laboratorio,
    Integer intervaloRefuerzoDias,
    String status,
    UUID tenantId
) {}
```

### UserServicePort (Puerto para user-management)

```java
public interface UserServicePort {
    VeterinarianInfo getVeterinarianById(UUID userId);
    boolean hasRole(UUID userId, String role);
}

record VeterinarianInfo(
    UUID userId,
    String nombre,
    String cedulaProfesional,
    boolean active,
    UUID tenantId
) {}
```

## Notes

- **Inmutabilidad**: Los registros clínicos NO se pueden modificar ni eliminar después de creación
- **Transaccionalidad**: Registro de vacunación + descuento de stock debe ser atómico (ambos o ninguno)
- **Desnormalización**: Copias de datos (nombre_vacuna, nombre_veterinario) son intencionales para historial inmutable
- **Control de acceso**: Solo veterinarios crean registros, otros roles (ADMIN, MANAGER, WORKER) solo leen
- **Cálculo automático**: proximaDosis se calcula automáticamente, no debe ser modificable manualmente
- **Integración resiliente**: Si vaccination-management falla, rechazar vacunaciones pero permitir tratamientos
- **Caching**: Reportes por animal cacheados 10 minutos, invalidados al crear nuevo registro
- **Auditoría**: Incluir resultado de descuento de stock (stock_antes, stock_despues) para trazabilidad
- **Performance**: Índices en tenant_id, animal_id, fecha_aplicacion, proxima_dosis son críticos
- **Validaciones**: fecha_aplicacion no puede ser futura, dosis_aplicada debe ser > 0, lote no puede estar vencido
- **Reportes**: Eficientes incluso con miles de registros usando paginación y queries optimizadas
- **Alertas**: Sistema de alertas para dosis vencidas y próximas a vencer
- **Herencia de tablas**: clinical_records es tabla base, vaccination_records y treatment_records extienden con FK
