# Design Document: Vaccination Management Module

## Overview

El módulo **vaccination-management** proporciona gestión completa del inventario de vacunas y su aplicación a ganado bovino en Vacapp. Implementa control de stock multi-lote, descuento automático al aplicar vacunas, cálculo inteligente de próximas dosis basado en intervalos de refuerzo, alertas de stock bajo y vencimientos próximos, trazabilidad completa por animal, y reportes de consumo y costos. Se integra estrechamente con cattle-inventory para validar animales activos y con user-management para verificar veterinarios autorizados.

### Contexto

Vacapp es un monolito modular construido con Spring Modulith siguiendo Clean Architecture. El módulo vaccination-management debe implementarse después de user-management y cattle-inventory ya que depende de autenticación JWT, validación de estado de animales, y contexto de tenant/rancho. Este módulo es fundamental para la gestión de salud preventiva del ganado.

### Objetivos del Módulo

1. **Inventario de vacunas**: Registro completo con categorías, tipos, vías de administración, laboratorio
2. **Gestión multi-lote**: Múltiples lotes por vacuna con fechas de vencimiento y stocks independientes
3. **Aplicación trazable**: Registro de cada aplicación con animal, lote, veterinario, fecha
4. **Descuento automático de stock**: Transaccional al aplicar vacuna
5. **Cálculo de próxima dosis**: Automático basado en intervalo_refuerzo_dias
6. **Alertas inteligentes**: Stock bajo y vencimientos próximos
7. **Reportes completos**: Consumo, costos, cobertura de vacunación
8. **Multi-tenancy**: Aislamiento por tenant_id y rancho_id

### Tecnologías

- **Backend**: Java 21, Spring Boot 4.1.0, Spring Modulith
- **Seguridad**: Spring Security 6 (integración con user-management)
- **Persistencia**: Spring Data JPA, MySQL 8
- **Validación**: Bean Validation (Hibernate Validator)
- **Scheduling**: Spring Scheduled Tasks para alertas
- **Caching**: Spring Cache con Caffeine
- **API Documentation**: OpenAPI 3.0 (Design-First con YAML)
- **Frontend Web**: Thymeleaf, HTML5, CSS Grid, JavaScript Vanilla
- **Utilidades**: Lombok

## Architecture

### Arquitectura de Capas (Clean Architecture)

```
mx.vacapp.vaccination/
│
├── VaccinationService.java          ← API PÚBLICA (único punto de entrada para otros módulos)
│
└── internal/                        ← TODO lo demás es PRIVADO
    │
    ├── domain/                      ← Capa de Dominio
    │   ├── model/
    │   │   ├── Vaccine.java             ← Entidad principal de vacuna
    │   │   ├── VaccineCategory.java     ← Enum/Entity de categoría
    │   │   ├── VaccineType.java         ← Enum/Entity de tipo
    │   │   ├── AdministrationRoute.java ← Enum/Entity de vía
    │   │   ├── VaccineLot.java          ← Lote de vacuna
    │   │   ├── VaccinationApplication.java ← Aplicación a animal
    │   │   ├── StockAlert.java          ← Alerta de stock bajo
    │   │   ├── ExpirationAlert.java     ← Alerta de vencimiento
    │   │   ├── NextDoseCalculator.java  ← Value Object cálculo próxima dosis
    │   │   └── exceptions/
    │   │       ├── VaccineNotFoundException.java
    │   │       ├── InsufficientStockException.java
    │   │       ├── ExpiredVaccineException.java
    │   │       └── InvalidVeterinarianException.java
    │   │
    │   └── repository/                 ← Puertos de salida
    │       ├── VaccineRepository.java
    │       ├── VaccineLotRepository.java
    │       ├── VaccinationApplicationRepository.java
    │       ├── StockAlertRepository.java
    │       ├── ExpirationAlertRepository.java
    │       └── VaccinationAuditRepository.java
    │
    ├── application/                    ← Capa de Aplicación
    │   └── usecases/
    │       ├── category/
    │       │   ├── CreateCategoryUseCase.java
    │       │   ├── UpdateCategoryUseCase.java
    │       │   ├── ListCategoriesUseCase.java
    │       │   ├── GetCategoryUseCase.java
    │       │   └── ArchiveCategoryUseCase.java
    │       ├── type/
    │       │   ├── CreateVaccineTypeUseCase.java
    │       │   ├── UpdateVaccineTypeUseCase.java
    │       │   ├── ListVaccineTypesUseCase.java
    │       │   └── ArchiveVaccineTypeUseCase.java
    │       ├── route/
    │       │   ├── CreateAdministrationRouteUseCase.java
    │       │   ├── UpdateAdministrationRouteUseCase.java
    │       │   ├── ListAdministrationRoutesUseCase.java
    │       │   └── ArchiveAdministrationRouteUseCase.java
    │       ├── vaccine/
    │       │   ├── CreateVaccineUseCase.java
    │       │   ├── UpdateVaccineUseCase.java
    │       │   ├── GetVaccineUseCase.java
    │       │   ├── ListVaccinesUseCase.java
    │       │   └── ArchiveVaccineUseCase.java
    │       ├── lot/
    │       │   ├── CreateVaccineLotUseCase.java
    │       │   ├── AdjustStockUseCase.java
    │       │   ├── GetLotsByVaccineUseCase.java
    │       │   └── MarkExpiredLotsUseCase.java
    │       ├── application/
    │       │   ├── ApplyVaccinationUseCase.java
    │       │   ├── GetVaccinationHistoryUseCase.java
    │       │   ├── GetUpcomingVaccinationsUseCase.java
    │       │   └── GetUnvaccinatedAnimalsUseCase.java
    │       ├── alert/
    │       │   ├── CheckStockAlertsUseCase.java
    │       │   ├── CheckExpirationAlertsUseCase.java
    │       │   ├── GetActiveStockAlertsUseCase.java
    │       │   └── GetActiveExpirationAlertsUseCase.java
    │       ├── report/
    │       │   ├── GenerateConsumptionReportUseCase.java
    │       │   ├── GenerateCostReportUseCase.java
    │       │   └── CalculateCoverageUseCase.java
    │       └── commands/
    │           ├── CreateVaccineCommand.java
    │           ├── VaccineResult.java
    │           ├── CreateVaccineLotCommand.java
    │           ├── VaccineLotResult.java
    │           ├── ApplyVaccinationCommand.java
    │           ├── VaccinationApplicationResult.java
    │           ├── ConsumptionReportResult.java
    │           └── CostReportResult.java
    │
    └── infrastructure/                 ← Capa de Infraestructura
        │
        ├── controllers/
        │   ├── mobile/                 ← API REST
        │   │   ├── VaccineCategoryRestController.java
        │   │   ├── VaccineTypeRestController.java
        │   │   ├── AdministrationRouteRestController.java
        │   │   ├── VaccineRestController.java
        │   │   ├── VaccineLotRestController.java
        │   │   ├── VaccinationRestController.java
        │   │   ├── AlertRestController.java
        │   │   ├── ReportRestController.java
        │   │   └── dtos/
        │   │       ├── CreateVaccineCategoryRequest.java
        │   │       ├── VaccineCategoryResponse.java
        │   │       ├── CreateVaccineTypeRequest.java
        │   │       ├── VaccineTypeResponse.java
        │   │       ├── CreateAdministrationRouteRequest.java
        │   │       ├── AdministrationRouteResponse.java
        │   │       ├── CreateVaccineRequest.java
        │   │       ├── VaccineResponse.java
        │   │       ├── VaccineDetailResponse.java
        │   │       ├── CreateVaccineLotRequest.java
        │   │       ├── VaccineLotResponse.java
        │   │       ├── ApplyVaccinationRequest.java
        │   │       ├── VaccinationApplicationResponse.java
        │   │       ├── VaccinationHistoryResponse.java
        │   │       ├── StockAlertResponse.java
        │   │       ├── ExpirationAlertResponse.java
        │   │       ├── ConsumptionReportResponse.java
        │   │       └── CostReportResponse.java
        │   │
        │   └── web/                    ← Controladores MVC
        │       ├── VaccineWebController.java
        │       ├── VaccinationWebController.java
        │       └── dtos/
        │           ├── VaccineFormDto.java
        │           └── ApplyVaccinationFormDto.java
        │
        ├── persistence/
        │   ├── entities/
        │   │   ├── VaccineEntity.java
        │   │   ├── VaccineCategoryEntity.java
        │   │   ├── VaccineTypeEntity.java
        │   │   ├── AdministrationRouteEntity.java
        │   │   ├── VaccineLotEntity.java
        │   │   ├── VaccinationApplicationEntity.java
        │   │   ├── StockAlertEntity.java
        │   │   ├── ExpirationAlertEntity.java
        │   │   ├── StockAdjustmentEntity.java
        │   │   └── VaccinationAuditEntity.java
        │   ├── repositories/
        │   │   ├── VaccineJpaRepository.java
        │   │   ├── VaccineLotJpaRepository.java
        │   │   ├── VaccinationApplicationJpaRepository.java
        │   │   ├── StockAlertJpaRepository.java
        │   │   └── ExpirationAlertJpaRepository.java
        │   ├── impl/
        │   │   ├── VaccineRepositoryImpl.java
        │   │   ├── VaccineLotRepositoryImpl.java
        │   │   ├── VaccinationApplicationRepositoryImpl.java
        │   │   ├── StockAlertRepositoryImpl.java
        │   │   └── ExpirationAlertRepositoryImpl.java
        │   └── mappers/
        │       ├── VaccineMapper.java
        │       ├── VaccineLotMapper.java
        │       └── VaccinationApplicationMapper.java
        │
        ├── integration/
        │   ├── CattleServiceClient.java     ← Integración con cattle-inventory
        │   └── UserServiceClient.java       ← Integración con user-management
        │
        ├── scheduling/
        │   ├── StockAlertScheduler.java     ← Job para alertas de stock
        │   └── ExpirationAlertScheduler.java ← Job para alertas de vencimiento
        │
        └── config/
            ├── VaccinationModuleConfig.java
            └── SchedulingConfig.java

```

### Flujo de Datos

#### Flujo de Aplicación de Vacuna

```
1. Usuario envía POST /api/v1/vaccinations con {id_animal, id_lote, fecha_aplicacion, dosis_aplicada, id_veterinario}
   ↓
2. VaccinationRestController recibe ApplyVaccinationRequest, valida @Valid
   ↓
3. VaccinationRestController mapea a ApplyVaccinationCommand
   ↓
4. ApplyVaccinationUseCase.execute(command)
   ↓
5. CattleService.isAnimalActive(id_animal) → validar animal activo
   ↓
6. IF NOT active: THROW InvalidAnimalException("Animal vendido o muerto")
   ↓
7. VaccineLotRepository.findById(id_lote) → obtener lote
   ↓
8. IF lote.stock_disponible <= 0: THROW InsufficientStockException
   ↓
9. IF lote.fecha_vencimiento < fecha_aplicacion: THROW ExpiredVaccineException
   ↓
10. UserService.hasRole(id_veterinario, "VETERINARIO") → validar veterinario
    ↓
11. BEGIN TRANSACTION
    |
    |-- UPDATE vaccine_lots SET stock_disponible = stock_disponible - 1 WHERE id_lote = :id
    |
    |-- VaccineRepository.findById(id_vacuna) → obtener vacuna para calcular próxima dosis
    |
    |-- IF vaccine.intervalo_refuerzo_dias NOT NULL:
    |   |  proxima_dosis_fecha = fecha_aplicacion + intervalo_refuerzo_dias
    |   ELSE:
    |   |  proxima_dosis_fecha = NULL
    |
    |-- INSERT INTO vaccination_applications (id_aplicacion, id_animal, id_vacuna, id_lote, 
    |                                          fecha_aplicacion, dosis_aplicada, id_veterinario,
    |                                          proxima_dosis_fecha, rancho_id, tenant_id, ...)
    |
    |-- VaccinationAuditRepository.logApplication(...)
    |
    |-- IF nuevo_stock <= vaccine.stock_minimo:
    |   |  INSERT INTO stock_alerts (id_alerta, id_vacuna, stock_actual, stock_minimo, ...)
    |
    COMMIT TRANSACTION
    ↓
12. Retornar VaccinationApplicationResult con próxima dosis y stock restante
    ↓
13. VaccinationRestController mapea a VaccinationApplicationResponse
    ↓
14. ResponseEntity.status(201).body(response) → HTTP 201
```

#### Flujo de Cálculo de Próxima Dosis

```
NextDoseCalculator.calculate(fecha_aplicacion, intervalo_refuerzo_dias):
  |
  IF intervalo_refuerzo_dias IS NULL:
  |  RETURN NULL  // Vacuna de dosis única
  |
  ELSE:
  |  proxima_dosis = fecha_aplicacion.plusDays(intervalo_refuerzo_dias)
  |  RETURN proxima_dosis
```

#### Flujo de Alerta de Stock Bajo (Scheduler)

```
@Scheduled(cron = "0 0 8 * * *")  // Cada día a las 8:00 AM
CheckStockAlertsUseCase.execute():
  |
  FOR EACH vaccine IN VaccineRepository.findAllActive():
  |  |
  |  stock_total = SUM(lote.stock_disponible WHERE id_vacuna = vaccine.id AND fecha_vencimiento >= CURRENT_DATE)
  |  |
  |  IF stock_total <= vaccine.stock_minimo:
  |  |  |
  |  |  alerta_existente = StockAlertRepository.findActiveByVaccine(vaccine.id)
  |  |  |
  |  |  IF alerta_existente IS NULL:
  |  |  |  INSERT INTO stock_alerts (id_vacuna, stock_actual, stock_minimo, fecha_alerta, resuelta = FALSE)
  |  |
  |  ELSE IF stock_total > vaccine.stock_minimo:
  |  |  |
  |  |  UPDATE stock_alerts SET resuelta = TRUE, fecha_resolucion = NOW() 
  |  |  WHERE id_vacuna = vaccine.id AND resuelta = FALSE
```

#### Flujo de Alerta de Vencimiento (Scheduler)

```
@Scheduled(cron = "0 0 6 * * *")  // Cada día a las 6:00 AM
CheckExpirationAlertsUseCase.execute():
  |
  lotes_por_vencer = VaccineLotRepository.findExpiringWithinDays(30)
  |
  FOR EACH lote IN lotes_por_vencer:
  |  |
  |  dias_para_vencer = DATEDIFF(lote.fecha_vencimiento, CURRENT_DATE)
  |  |
  |  alerta_existente = ExpirationAlertRepository.findByLot(lote.id)
  |  |
  |  IF alerta_existente IS NULL:
  |  |  INSERT INTO expiration_alerts (id_lote, fecha_vencimiento, dias_para_vencer, 
  |  |                                  stock_en_riesgo, fecha_alerta)
  |  |
  |  ELSE:
  |  |  UPDATE expiration_alerts SET dias_para_vencer = :dias WHERE id_lote = lote.id
  |
  // Marcar lotes vencidos
  UPDATE vaccine_lots SET stock_disponible = 0 WHERE fecha_vencimiento < CURRENT_DATE
  DELETE FROM expiration_alerts WHERE fecha_vencimiento < CURRENT_DATE
```

### Principios de Diseño

1. **Transaccionalidad**: Descuento de stock y registro de aplicación en transacción atómica
2. **Consistencia**: Validación de estado de animal antes de vacunar
3. **Trazabilidad**: Auditoría completa de todas las operaciones
4. **Alertas proactivas**: Jobs programados para detectar problemas
5. **Multi-tenancy**: Filtrado automático por tenant_id y rancho_id
6. **Integridad referencial**: FK constraints en BD para garantizar consistencia
7. **Separación de responsabilidades**: Un UseCase por operación de negocio

## Components and Interfaces

### 1. API Pública del Módulo

#### VaccinationService.java

```java
package mx.vacapp.vaccination;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública del módulo de gestión de vacunas.
 */
public interface VaccinationService {
    
    /**
     * Obtiene el total de vacunaciones de un animal.
     */
    int getVaccinationCountByAnimal(UUID animalId);
    
    /**
     * Obtiene la próxima vacunación programada de un animal.
     */
    Optional<NextVaccinationInfo> getNextScheduledVaccination(UUID animalId);
    
    /**
     * Verifica si una vacuna existe y está activa.
     */
    boolean isVaccineActive(UUID vaccineId);
    
    record NextVaccinationInfo(
        String vaccineName,
        LocalDate scheduledDate,
        int daysRemaining
    ) {}
}
```


### 2. Capa de Dominio

#### Vaccine.java (Entidad de Dominio)

```java
package mx.vacapp.vaccination.internal.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad de dominio que representa una vacuna.
 */
public class Vaccine {
    private final UUID vaccineId;
    private final String nombreComercial;
    private final UUID categoriaId;
    private final UUID tipoVacunaId;
    private final UUID viaAdministracionId;
    private final String laboratorio;
    private final String dosisRecomendada;
    private final Integer intervaloRefuerzoDias;  // NULL = dosis única
    private final Integer stockMinimo;
    private final UUID ranchoId;
    private final UUID tenantId;
    private final boolean activo;
    private final Instant createdAt;
    private final UUID createdBy;
    
    // Constructor, builder, getters omitidos
    
    public boolean requiresBooster() {
        return intervaloRefuerzoDias != null && intervaloRefuerzoDias > 0;
    }
    
    public boolean isLowStock(int currentStock) {
        return currentStock <= stockMinimo;
    }
}
```

#### VaccineLot.java

```java
package mx.vacapp.vaccination.internal.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class VaccineLot {
    private final UUID lotId;
    private final UUID vaccineId;
    private final String numeroLote;
    private final LocalDate fechaVencimiento;
    private int stockDisponible;
    private final BigDecimal precioCosto;
    private final LocalDate fechaIngreso;
    
    public boolean isExpired(LocalDate currentDate) {
        return fechaVencimiento.isBefore(currentDate);
    }
    
    public boolean hasStock() {
        return stockDisponible > 0;
    }
    
    public void decrementStock(int quantity) {
        if (stockDisponible < quantity) {
            throw new InsufficientStockException("Stock insuficiente");
        }
        this.stockDisponible -= quantity;
    }
    
    public int daysUntilExpiration(LocalDate currentDate) {
        return (int) currentDate.until(fechaVencimiento, java.time.temporal.ChronoUnit.DAYS);
    }
}
```

#### VaccinationApplication.java

```java
package mx.vacapp.vaccination.internal.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class VaccinationApplication {
    private final UUID applicationId;
    private final UUID animalId;
    private final UUID vaccineId;
    private final UUID lotId;
    private final LocalDate fechaAplicacion;
    private final String dosisAplicada;
    private final UUID veterinarioId;
    private final LocalDate proximaDosisFecha;  // NULL si no requiere refuerzo
    private final String observaciones;
    private final BigDecimal costoAplicacion;
    private final UUID ranchoId;
    private final UUID tenantId;
    
    public boolean hasNextDose() {
        return proximaDosisFecha != null;
    }
    
    public boolean isNextDoseDue(LocalDate currentDate) {
        return proximaDosisFecha != null && 
               !proximaDosisFecha.isAfter(currentDate);
    }
    
    public int daysUntilNextDose(LocalDate currentDate) {
        if (proximaDosisFecha == null) return -1;
        return (int) currentDate.until(proximaDosisFecha, java.time.temporal.ChronoUnit.DAYS);
    }
}
```

#### NextDoseCalculator.java

```java
package mx.vacapp.vaccination.internal.domain.model;

import java.time.LocalDate;

/**
 * Value Object para cálculo de próxima dosis.
 */
public class NextDoseCalculator {
    
    public static LocalDate calculate(LocalDate applicationDate, Integer intervalDays) {
        if (intervalDays == null || intervalDays <= 0) {
            return null;  // Vacuna de dosis única
        }
        return applicationDate.plusDays(intervalDays);
    }
    
    public static boolean isDueForBooster(LocalDate nextDoseDate, LocalDate currentDate, int warningDays) {
        if (nextDoseDate == null) return false;
        return !nextDoseDate.minusDays(warningDays).isAfter(currentDate);
    }
}
```

## Data Models

### Esquema de Base de Datos MySQL

```sql
-- Tabla de categorías de vacunas (sin datos predefinidos)
CREATE TABLE vaccine_categories (
    id_categoria BINARY(16) PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion TEXT,
    rancho_id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    CONSTRAINT uq_vaccine_categories_nombre_tenant UNIQUE (tenant_id, rancho_id, nombre),
    INDEX idx_vaccine_categories_tenant (tenant_id, rancho_id),
    INDEX idx_vaccine_categories_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de tipos de vacunas (sin datos predefinidos)
CREATE TABLE vaccine_types (
    id_tipo_vacuna BINARY(16) PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion TEXT,
    rancho_id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    CONSTRAINT uq_vaccine_types_nombre_tenant UNIQUE (tenant_id, rancho_id, nombre),
    INDEX idx_vaccine_types_tenant (tenant_id, rancho_id),
    INDEX idx_vaccine_types_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de vías de administración (sin datos predefinidos)
CREATE TABLE administration_routes (
    id_via_administracion BINARY(16) PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    abreviatura VARCHAR(10),
    descripcion TEXT,
    rancho_id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    CONSTRAINT uq_administration_routes_nombre_tenant UNIQUE (tenant_id, rancho_id, nombre),
    INDEX idx_administration_routes_tenant (tenant_id, rancho_id),
    INDEX idx_administration_routes_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla principal de vacunas
CREATE TABLE vaccines (
    id_vacuna BINARY(16) PRIMARY KEY,
    nombre_comercial VARCHAR(200) NOT NULL,
    id_categoria BINARY(16) NOT NULL,
    id_tipo_vacuna BINARY(16) NOT NULL,
    id_via_administracion BINARY(16) NOT NULL,
    laboratorio VARCHAR(100) NOT NULL,
    dosis_recomendada VARCHAR(50),
    intervalo_refuerzo_dias INT,  -- NULL = dosis única
    stock_minimo INT NOT NULL DEFAULT 10,
    rancho_id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    CONSTRAINT chk_intervalo_refuerzo CHECK (intervalo_refuerzo_dias IS NULL OR intervalo_refuerzo_dias > 0),
    CONSTRAINT chk_stock_minimo CHECK (stock_minimo >= 0),
    
    INDEX idx_vaccines_tenant_rancho (tenant_id, rancho_id),
    INDEX idx_vaccines_categoria (id_categoria),
    INDEX idx_vaccines_activo (activo),
    INDEX idx_vaccines_nombre (nombre_comercial),
    
    CONSTRAINT fk_vaccines_categoria FOREIGN KEY (id_categoria) REFERENCES vaccine_categories(id_categoria),
    CONSTRAINT fk_vaccines_tipo FOREIGN KEY (id_tipo_vacuna) REFERENCES vaccine_types(id_tipo_vacuna),
    CONSTRAINT fk_vaccines_via FOREIGN KEY (id_via_administracion) REFERENCES administration_routes(id_via_administracion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de lotes de vacunas
CREATE TABLE vaccine_lots (
    id_lote BINARY(16) PRIMARY KEY,
    id_vacuna BINARY(16) NOT NULL,
    numero_lote VARCHAR(50) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    stock_disponible INT NOT NULL DEFAULT 0,
    precio_costo DECIMAL(10,2),
    fecha_ingreso DATE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    
    CONSTRAINT chk_stock_disponible CHECK (stock_disponible >= 0),
    CONSTRAINT chk_precio_costo CHECK (precio_costo IS NULL OR precio_costo >= 0),
    
    INDEX idx_vaccine_lots_vacuna (id_vacuna),
    INDEX idx_vaccine_lots_vencimiento (fecha_vencimiento),
    INDEX idx_vaccine_lots_stock (stock_disponible),
    
    CONSTRAINT fk_vaccine_lots_vacuna FOREIGN KEY (id_vacuna) REFERENCES vaccines(id_vacuna)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de aplicaciones de vacunas
CREATE TABLE vaccination_applications (
    id_aplicacion BINARY(16) PRIMARY KEY,
    id_animal BINARY(16) NOT NULL,
    id_vacuna BINARY(16) NOT NULL,
    id_lote BINARY(16) NOT NULL,
    fecha_aplicacion DATE NOT NULL,
    dosis_aplicada VARCHAR(50),
    id_veterinario BINARY(16),
    proxima_dosis_fecha DATE,  -- Calculada automáticamente
    observaciones TEXT,
    costo_aplicacion DECIMAL(10,2),
    rancho_id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    
    INDEX idx_vaccination_applications_animal (id_animal),
    INDEX idx_vaccination_applications_vacuna (id_vacuna),
    INDEX idx_vaccination_applications_lote (id_lote),
    INDEX idx_vaccination_applications_tenant (tenant_id, rancho_id),
    INDEX idx_vaccination_applications_proxima_dosis (proxima_dosis_fecha),
    INDEX idx_vaccination_applications_fecha (fecha_aplicacion),
    
    CONSTRAINT fk_vaccination_applications_vacuna FOREIGN KEY (id_vacuna) REFERENCES vaccines(id_vacuna),
    CONSTRAINT fk_vaccination_applications_lote FOREIGN KEY (id_lote) REFERENCES vaccine_lots(id_lote)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de alertas de stock bajo
CREATE TABLE stock_alerts (
    id_alerta BINARY(16) PRIMARY KEY,
    id_vacuna BINARY(16) NOT NULL,
    stock_actual INT NOT NULL,
    stock_minimo INT NOT NULL,
    fecha_alerta TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resuelta BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_resolucion TIMESTAMP(6),
    tenant_id BINARY(16) NOT NULL,
    rancho_id BINARY(16) NOT NULL,
    
    INDEX idx_stock_alerts_vacuna (id_vacuna),
    INDEX idx_stock_alerts_resuelta (resuelta),
    INDEX idx_stock_alerts_tenant (tenant_id, rancho_id),
    
    CONSTRAINT fk_stock_alerts_vacuna FOREIGN KEY (id_vacuna) REFERENCES vaccines(id_vacuna)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de alertas de vencimiento
CREATE TABLE expiration_alerts (
    id_alerta BINARY(16) PRIMARY KEY,
    id_lote BINARY(16) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    dias_para_vencer INT NOT NULL,
    stock_en_riesgo INT NOT NULL,
    fecha_alerta TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    tenant_id BINARY(16) NOT NULL,
    rancho_id BINARY(16) NOT NULL,
    
    INDEX idx_expiration_alerts_lote (id_lote),
    INDEX idx_expiration_alerts_dias (dias_para_vencer),
    INDEX idx_expiration_alerts_tenant (tenant_id, rancho_id),
    
    CONSTRAINT fk_expiration_alerts_lote FOREIGN KEY (id_lote) REFERENCES vaccine_lots(id_lote)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de ajustes de stock
CREATE TABLE stock_adjustments (
    id_ajuste BINARY(16) PRIMARY KEY,
    id_lote BINARY(16) NOT NULL,
    cantidad_anterior INT NOT NULL,
    cantidad_nueva INT NOT NULL,
    motivo VARCHAR(200) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    
    INDEX idx_stock_adjustments_lote (id_lote),
    INDEX idx_stock_adjustments_fecha (created_at),
    
    CONSTRAINT fk_stock_adjustments_lote FOREIGN KEY (id_lote) REFERENCES vaccine_lots(id_lote)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de auditoría
CREATE TABLE vaccination_audit (
    audit_id BINARY(16) PRIMARY KEY,
    operation_type VARCHAR(30) NOT NULL,
    entity_id BINARY(16) NOT NULL,
    timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    modified_by BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    old_values TEXT,
    new_values TEXT,
    
    CONSTRAINT chk_operation_type CHECK (operation_type IN ('CREATE_VACCINE', 'UPDATE_VACCINE', 
                                                             'CREATE_LOT', 'ADJUST_STOCK', 
                                                             'APPLY_VACCINATION')),
    
    INDEX idx_vaccination_audit_entity (entity_id),
    INDEX idx_vaccination_audit_timestamp (timestamp),
    INDEX idx_vaccination_audit_operation (operation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## Error Handling

```java
package mx.vacapp.vaccination.internal.domain.exceptions;

public class VaccinationDomainException extends RuntimeException {
    public VaccinationDomainException(String message) {
        super(message);
    }
}

public class VaccineNotFoundException extends VaccinationDomainException {
    public VaccineNotFoundException(String message) {
        super(message);
    }
}

public class InsufficientStockException extends VaccinationDomainException {
    public InsufficientStockException(String message) {
        super(message);
    }
}

public class ExpiredVaccineException extends VaccinationDomainException {
    public ExpiredVaccineException(String message) {
        super(message);
    }
}

public class InvalidVeterinarianException extends VaccinationDomainException {
    public InvalidVeterinarianException(String message) {
        super(message);
    }
}

public class InvalidAnimalException extends VaccinationDomainException {
    public InvalidAnimalException(String message) {
        super(message);
    }
}
```

## Testing Strategy

1. **Pruebas Unitarias**: Casos de uso con mocks, lógica de cálculo de próxima dosis
2. **Pruebas de Integración**: Controllers, repositorios, integración con cattle-inventory
3. **Pruebas de Concurrencia**: Descuento simultáneo de stock (transaccionalidad)
4. **Pruebas de Schedulers**: Jobs de alertas

## Notes

- El descuento de stock DEBE ser transaccional para evitar overselling
- Los schedulers deben ejecutarse en horarios de baja carga
- Considerar índices adicionales si consultas de reportes son lentas
- Las alertas pueden integrarse con sistema de notificaciones push en futuro
- El cálculo de próxima dosis es crítico para planificación de campañas
