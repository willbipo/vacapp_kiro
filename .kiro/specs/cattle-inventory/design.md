# Design Document: Cattle Inventory Module

## Overview

El módulo **cattle-inventory** es el componente central y más crítico de Vacapp que proporciona funcionalidades completas para la gestión integral del inventario de ganado bovino. Este módulo implementa registro único de identificación (arete), genealogía completa (padre/madre/hijos), trazabilidad de movimientos entre potreros, historial de pesos, eventos de salud (vacunaciones, tratamientos, partos), y cumplimiento regulatorio mexicano (Folio REEMO).

### Contexto

Vacapp es un monolito modular construido con Spring Modulith siguiendo Clean Architecture. El módulo cattle-inventory debe implementarse después de user-management y geographic-control ya que depende de autenticación JWT, contexto de tenant, y validación de potreros activos. Este módulo es fundamental ya que todos los demás módulos funcionales (salud, reproducción, producción, nutrición) operan sobre el inventario de ganado.

### Objetivos del Módulo

1. **Identificación única**: Arete único a nivel global en Vacapp para prevenir duplicados
2. **Genealogía completa**: Registro de madre, padre, y relaciones familiares para trazabilidad genética
3. **Trazabilidad de ubicación**: Historial completo de movimientos entre potreros con fechas de entrada/salida
4. **Gestión de estados**: Control de ciclo de vida del animal (Activa, Preñada, Vendida, Muerta, etc.)
5. **Integración con geographic-control**: Validación automática de potreros activos antes de asignar animales
6. **Historial de pesos**: Registro de múltiples pesos con cálculo de ganancia diaria
7. **Eventos de salud**: Vacunaciones, tratamientos médicos, partos con creación automática de crías
8. **Cumplimiento legal**: Soporte para Folio REEMO (Registro Electrónico de Movilización de México)

### Tecnologías

- **Backend**: Java 21, Spring Boot 4.1.0, Spring Modulith
- **Seguridad**: Spring Security 6 (integración con user-management)
- **Persistencia**: Spring Data JPA, MySQL 8
- **Validación**: Bean Validation (Hibernate Validator)
- **Caching**: Spring Cache con Caffeine
- **API Documentation**: OpenAPI 3.0 (Design-First con YAML)
- **Frontend Web**: Thymeleaf, HTML5, CSS Grid, JavaScript Vanilla
- **Utilidades**: Lombok, MapStruct (opcional para mappers)

## Architecture

### Arquitectura de Capas (Clean Architecture)

El módulo cattle-inventory sigue estrictamente la arquitectura hexagonal con separación de responsabilidades en tres capas principales:

```
mx.vacapp.cattle/
│
├── CattleService.java               ← API PÚBLICA (único punto de entrada para otros módulos)
│
└── internal/                        ← TODO lo demás es PRIVADO (inaccesible desde otros módulos)
    │
    ├── domain/                      ← Capa de Dominio (lógica de negocio pura)
    │   ├── model/                   ← Entidades de negocio (POJOs sin JPA)
    │   │   ├── Animal.java              ← Entidad principal de animal
    │   │   ├── Sex.java                 ← Enum de sexo (Macho, Hembra)
    │   │   ├── Breed.java               ← Enum de raza (Charolais, Angus, etc.)
    │   │   ├── CattleStatus.java        ← Enum de estados (Activa, Vendida, Muerta, etc.)
    │   │   ├── CattleType.java          ← Enum de tipos (Venta, Cría, Engorda, etc.)
    │   │   ├── PastureAssignment.java   ← Asignación actual a potrero
    │   │   ├── PastureHistory.java      ← Historial de movimientos entre potreros
    │   │   ├── WeightRecord.java        ← Registro de peso en fecha específica
    │   │   ├── HealthEvent.java         ← Evento de salud (vacunación, tratamiento, parto)
    │   │   ├── AgeCalculator.java       ← Value Object para cálculo de edad
    │   │   └── exceptions/              ← Excepciones de dominio
    │   │       ├── DuplicateAreteException.java
    │   │       ├── InvalidGenealogyException.java
    │   │       ├── InvalidPastureException.java
    │   │       ├── InvalidStatusTransitionException.java
    │   │       ├── AnimalNotFoundException.java
    │   │       └── SoldOrDeadAnimalException.java
    │   │
    │   └── repository/                 ← Puertos de salida (interfaces)
    │       ├── AnimalRepository.java   ← Puerto de persistencia de animales
    │       ├── PastureHistoryRepository.java  ← Puerto de historial de movimientos
    │       ├── WeightRepository.java   ← Puerto de pesos
    │       ├── HealthEventRepository.java     ← Puerto de eventos de salud
    │       └── CattleAuditRepository.java     ← Puerto de auditoría
    │
    ├── application/                    ← Capa de Aplicación (casos de uso)
    │   └── usecases/
    │       ├── animal/                 ← Casos de uso de animales
    │       │   ├── CreateAnimalUseCase.java
    │       │   ├── UpdateAnimalUseCase.java
    │       │   ├── GetAnimalUseCase.java
    │       │   ├── ListAnimalsUseCase.java
    │       │   ├── ChangeStatusUseCase.java
    │       │   ├── GetAnimalStatsUseCase.java
    │       │   └── SearchAnimalsByAreteUseCase.java
    │       ├── movement/               ← Casos de uso de movimientos
    │       │   ├── MoveAnimalUseCase.java
    │       │   ├── GetMovementHistoryUseCase.java
    │       │   └── ListAnimalsInPastureUseCase.java
    │       ├── weight/                 ← Casos de uso de pesos
    │       │   ├── RecordWeightUseCase.java
    │       │   ├── GetWeightHistoryUseCase.java
    │       │   └── CalculateDailyGainUseCase.java
    │       ├── health/                 ← Casos de uso de salud
    │       │   ├── RecordHealthEventUseCase.java
    │       │   ├── GetHealthHistoryUseCase.java
    │       │   ├── RecordBirthEventUseCase.java
    │       │   └── GetUpcomingVaccinationsUseCase.java
    │       └── commands/               ← Comandos y resultados (records)
    │           ├── CreateAnimalCommand.java
    │           ├── UpdateAnimalCommand.java
    │           ├── AnimalResult.java
    │           ├── MoveAnimalCommand.java
    │           ├── RecordWeightCommand.java
    │           ├── WeightResult.java
    │           ├── RecordHealthEventCommand.java
    │           └── HealthEventResult.java
    │
    └── infrastructure/                 ← Capa de Infraestructura (adaptadores)
        │
        ├── controllers/                ← Adaptadores de entrada HTTP
        │   ├── mobile/                 ← Controladores REST (API JSON)
        │   │   ├── AnimalRestController.java
        │   │   ├── MovementRestController.java
        │   │   ├── WeightRestController.java
        │   │   ├── HealthRestController.java
        │   │   └── dtos/               ← DTOs Request/Response (Records)
        │   │       ├── CreateAnimalRequest.java
        │   │       ├── UpdateAnimalRequest.java
        │   │       ├── AnimalResponse.java
        │   │       ├── AnimalDetailResponse.java
        │   │       ├── MoveAnimalRequest.java
        │   │       ├── PastureHistoryResponse.java
        │   │       ├── RecordWeightRequest.java
        │   │       ├── WeightResponse.java
        │   │       ├── RecordHealthEventRequest.java
        │   │       ├── HealthEventResponse.java
        │   │       └── CattleStatsResponse.java
        │   │
        │   └── web/                    ← Controladores MVC (Thymeleaf)
        │       ├── CattleWebController.java
        │       ├── MovementWebController.java
        │       ├── HealthWebController.java
        │       └── dtos/               ← Form DTOs (Records)
        │           ├── AnimalFormDto.java
        │           ├── MoveAnimalFormDto.java
        │           └── HealthEventFormDto.java
        │
        ├── persistence/                ← Adaptador de salida (JPA)
        │   ├── entities/               ← Entidades JPA
        │   │   ├── AnimalEntity.java   ← @Entity con @Table("animals")
        │   │   ├── PastureHistoryEntity.java  ← @Entity con @Table("pasture_history")
        │   │   ├── WeightEntity.java   ← @Entity con @Table("cattle_weights")
        │   │   ├── HealthEventEntity.java     ← @Entity con @Table("health_events")
        │   │   └── CattleAuditEntity.java     ← @Entity con @Table("cattle_audit")
        │   ├── repositories/           ← Repositorios JPA
        │   │   ├── AnimalJpaRepository.java
        │   │   ├── PastureHistoryJpaRepository.java
        │   │   ├── WeightJpaRepository.java
        │   │   ├── HealthEventJpaRepository.java
        │   │   └── CattleAuditJpaRepository.java
        │   ├── impl/                   ← Implementaciones de puertos
        │   │   ├── AnimalRepositoryImpl.java
        │   │   ├── PastureHistoryRepositoryImpl.java
        │   │   ├── WeightRepositoryImpl.java
        │   │   ├── HealthEventRepositoryImpl.java
        │   │   └── CattleAuditRepositoryImpl.java
        │   └── mappers/                ← Transformación entre capas
        │       ├── AnimalMapper.java
        │       ├── PastureHistoryMapper.java
        │       ├── WeightMapper.java
        │       ├── HealthEventMapper.java
        │       └── AuditMapper.java
        │
        ├── integration/                ← Adaptadores para integración con otros módulos
        │   └── GeographyServiceClient.java  ← Cliente para GeographyService
        │
        ├── cache/                      ← Configuración de caching
        │   ├── CacheConfig.java        ← Configuración Caffeine
        │   └── CacheNames.java         ← Constantes de nombres de cache
        │
        └── config/                     ← Configuración del módulo
            ├── CattleModuleConfig.java  ← Beans del módulo
            └── OpenApiConfig.java       ← Configuración Swagger
```

### Flujo de Datos

#### Flujo de Creación de Animal

```
1. Usuario envía POST /api/v1/cattle con {arete, sexo, raza, fecha_nacimiento, potrero_id}
   ↓
2. AnimalRestController recibe CreateAnimalRequest, valida con @Valid
   ↓
3. AnimalRestController mapea a CreateAnimalCommand
   ↓
4. CreateAnimalUseCase.execute(CreateAnimalCommand)
   ↓
5. AnimalRepository.existsByArete(arete) → valida unicidad global
   ↓
6. GeographyService.isPotreroActive(potrero_id) → valida potrero
   ↓
7. Animal.create(...) → crea entidad con meses calculados, status Activa
   ↓
8. AnimalRepository.save(animal) → persiste (filtra por tenant_id)
   ↓
9. PastureHistoryRepository.insert({animal_id, potrero_id, fecha_entrada NOW, fecha_salida null})
   ↓
10. CattleAuditRepository.logAnimalCreation(animal, createdBy) → auditoría
    ↓
11. Retorna AnimalResult con peso_actual null, potrero_actual
    ↓
12. AnimalRestController mapea a AnimalResponse
    ↓
13. ResponseEntity.status(201).body(AnimalResponse) → HTTP 201
```

#### Flujo de Movimiento de Animal entre Potreros

```
1. Usuario envía POST /api/v1/cattle/{id}/move con {potrero_destino_id}
   ↓
2. MovementRestController recibe MoveAnimalRequest
   ↓
3. MovementRestController mapea a MoveAnimalCommand
   ↓
4. MoveAnimalUseCase.execute(MoveAnimalCommand)
   ↓
5. AnimalRepository.findById(animal_id) → obtener animal, validar tenant
   ↓
6. IF animal.status IN (Vendida, Muerta):
   |  THROW SoldOrDeadAnimalException("No se puede mover animal vendido o muerto")
   ↓
7. GeographyService.isPotreroActive(potrero_destino_id) → validar
   ↓
8. PastureHistoryRepository.findCurrentByAnimalId(animal_id) → obtener ubicación actual
   ↓
9. UPDATE pasture_history SET fecha_salida = NOW() WHERE id = current_record.id
   ↓
10. INSERT INTO pasture_history (animal_id, potrero_id, fecha_entrada NOW, fecha_salida null)
    ↓
11. CattleAuditRepository.logAnimalMovement(animal, potrero_anterior, potrero_nuevo)
    ↓
12. Retornar success
    ↓
13. ResponseEntity.ok() → HTTP 200
```

#### Flujo de Registro de Peso

```
1. Usuario envía POST /api/v1/cattle/{id}/weights con {peso_kg, fecha_pesaje, notas}
   ↓
2. WeightRestController recibe RecordWeightRequest, valida con @Valid
   ↓
3. WeightRestController mapea a RecordWeightCommand
   ↓
4. RecordWeightUseCase.execute(RecordWeightCommand)
   ↓
5. AnimalRepository.findById(animal_id) → validar existencia y tenant
   ↓
6. IF animal.status IN (Vendida, Muerta):
   |  THROW SoldOrDeadAnimalException("No se puede registrar peso de animal vendido o muerto")
   ↓
7. WeightRecord.create(peso_kg, fecha_pesaje, notas, animal_id, created_by)
   ↓
8. WeightRepository.save(weightRecord) → persiste
   ↓
9. WeightRepository.findPreviousWeight(animal_id, fecha_pesaje) → obtener peso anterior
   ↓
10. IF peso_anterior EXISTS:
    |  Calcular ganancia_diaria = (peso_kg - peso_anterior.peso_kg) / dias_diferencia
   ↓
11. Retornar WeightResult con ganancia_diaria calculada
    ↓
12. ResponseEntity.status(201).body(WeightResponse) → HTTP 201
```

#### Flujo de Registro de Evento de Parto (Birth)

```
1. Usuario envía POST /api/v1/cattle/{id}/health con {event_type: "BIRTH", cria_arete, cria_sexo}
   ↓
2. HealthRestController recibe RecordHealthEventRequest
   ↓
3. RecordBirthEventUseCase.execute(command)
   ↓
4. AnimalRepository.findById(madre_id) → obtener madre, validar sexo Hembra
   ↓
5. IF madre.sexo ≠ Hembra:
   |  THROW InvalidGenealogyException("Solo hembras pueden parir")
   ↓
6. AnimalRepository.existsByArete(cria_arete) → validar arete único
   ↓
7. PastureHistoryRepository.getCurrentPasture(madre_id) → obtener potrero actual de madre
   ↓
8. Animal.create(cria):
   |  arete = cria_arete
   |  sexo = cria_sexo
   |  fecha_nacimiento = fecha_evento
   |  madre_id = madre_id
   |  status = Activa
   |  potrero_id = potrero_madre (copiado)
   |  fecha_aretado = fecha_evento
   ↓
9. AnimalRepository.save(cria) → persiste cría
   ↓
10. PastureHistoryRepository.insert(cria, potrero_madre, fecha_entrada = fecha_evento)
    ↓
11. HealthEventRepository.insert(event: BIRTH, madre_id, descripcion = {cria_id})
    ↓
12. IF madre.status = Preñada:
    |  UPDATE madre SET status = Activa
    ↓
13. CattleAuditRepository.logBirthEvent(madre, cria, fecha_evento)
    ↓
14. Retornar HealthEventResult con cria_id
    ↓
15. ResponseEntity.status(201) → HTTP 201
```

### Principios de Diseño

1. **Encapsulamiento estricto**: Solo `CattleService.java` es público, todo bajo `internal/` es privado
2. **Separación de capas**: Dominio no depende de infraestructura (sin anotaciones JPA)
3. **Dependency Inversion**: Domain define interfaces (puertos), Infrastructure las implementa
4. **Single Responsibility**: Cada UseCase tiene una única responsabilidad
5. **Immutability**: Records para DTOs y comandos (inmutables por defecto)
6. **Fail-fast**: Validación temprana en controllers con Bean Validation
7. **Integridad genealógica**: Validación automática de relaciones madre-hijo/padre-hijo
8. **Trazabilidad de movimientos**: Historial completo preservado con fechas entrada/salida

## Components and Interfaces

### 1. API Pública del Módulo

#### CattleService.java

```java
package mx.vacapp.cattle;

import java.util.Optional;
import java.util.UUID;

/**
 * API pública del módulo de inventario de ganado.
 * Único punto de entrada para otros módulos de Vacapp.
 */
public interface CattleService {
    
    /**
     * Valida si un animal existe y está activo (no vendido/muerto).
     * 
     * @param animalId UUID del animal
     * @return true si el animal existe y status IN (Activa, Preñada, En Reposo)
     */
    boolean isAnimalActive(UUID animalId);
    
    /**
     * Obtiene el tenant_id de un animal.
     * 
     * @param animalId UUID del animal
     * @return Optional con el tenant_id, o empty si no existe
     */
    Optional<UUID> getAnimalTenantId(UUID animalId);
    
    /**
     * Cuenta cantidad de animales en un potrero específico.
     * 
     * @param potreroId UUID del potrero
     * @return cantidad de animales activos en el potrero
     */
    int countAnimalsInPasture(UUID potreroId);
    
    /**
     * Obtiene el sexo de un animal.
     * 
     * @param animalId UUID del animal
     * @return Optional con el sexo (Macho/Hembra)
     */
    Optional<String> getAnimalSex(UUID animalId);
}
```


### 2. Capa de Dominio

#### Animal.java (Entidad de Dominio)

```java
package mx.vacapp.cattle.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad de dominio que representa un animal bovino.
 * POJO puro sin anotaciones de Spring/JPA.
 */
public class Animal {
    private final UUID animalId;
    private final String arete;          // Identificador único global
    private final String areteAnterior;  // Arete previo (opcional)
    private final Sex sexo;
    private final Breed raza;
    private final LocalDate fechaNacimiento;
    private final Integer meses;         // Calculado desde fecha_nacimiento
    private final LocalDate fechaAretado;
    private final CattleType tipo;
    private final CattleStatus status;
    private final String folioReemo;     // Opcional, regulación mexicana
    private final String nota;           // Observaciones libres
    private final UUID madreId;          // Nullable
    private final UUID padreId;          // Nullable
    private final UUID ranchoId;
    private final UUID tenantId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID createdBy;
    private final UUID updatedBy;
    
    // Campos de venta/muerte (solo relevantes cuando status = Vendida/Muerta)
    private final LocalDate fechaVenta;
    private final BigDecimal precioVenta;
    private final LocalDate fechaMuerte;
    private final String motivoMuerte;
    
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
    
    public boolean isActive() {
        return this.status == CattleStatus.ACTIVA || 
               this.status == CattleStatus.PRENADA || 
               this.status == CattleStatus.EN_REPOSO;
    }
    
    public boolean isSoldOrDead() {
        return this.status == CattleStatus.VENDIDA || 
               this.status == CattleStatus.MUERTA;
    }
    
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
    
    // Getters omitidos por brevedad
    
    public static class Builder {
        // Campos omitidos por brevedad
        
        public Builder from(Animal animal) {
            // Copiar todos los campos
            return this;
        }
        
        public Animal build() {
            return new Animal(this);
        }
    }
}
```

#### Enums de Dominio

```java
public enum Sex {
    MACHO("macho"),
    HEMBRA("hembra");
    
    private final String value;
    Sex(String value) { this.value = value; }
    public String getValue() { return value; }
}

public enum Breed {
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
    Breed(String value) { this.value = value; }
    public String getValue() { return value; }
}

public enum CattleStatus {
    ACTIVA("activa"),
    VENDIDA("vendida"),
    MUERTA("muerta"),
    PRESTADA("prestada"),
    PRENADA("prenada"),
    EN_REPOSO("en_reposo");
    
    private final String value;
    CattleStatus(String value) { this.value = value; }
    public String getValue() { return value; }
}

public enum CattleType {
    VENTA("venta"),
    CRIA("cria"),
    ENGORDA("engorda"),
    SEMENTAL("semental"),
    VIENTRE("vientre");
    
    private final String value;
    CattleType(String value) { this.value = value; }
    public String getValue() { return value; }
}
```

#### AgeCalculator.java

```java
package mx.vacapp.cattle.internal.domain.model;

import java.time.LocalDate;
import java.time.Period;

/**
 * Value Object para cálculos de edad del animal.
 */
public class AgeCalculator {
    
    /**
     * Calcula edad en meses desde fecha de nacimiento hasta fecha actual.
     * 
     * @param fechaNacimiento fecha de nacimiento del animal
     * @param fechaActual fecha actual (para testing)
     * @return edad en meses completos
     */
    public static int calculateMonths(LocalDate fechaNacimiento, LocalDate fechaActual) {
        if (fechaNacimiento.isAfter(fechaActual)) {
            throw new IllegalArgumentException("Fecha de nacimiento no puede ser futura");
        }
        Period period = Period.between(fechaNacimiento, fechaActual);
        return period.getYears() * 12 + period.getMonths();
    }
}
```


## Data Models

### Modelo de Persistencia (Infrastructure Layer)

#### Esquema de Base de Datos MySQL

```sql
-- Tabla principal de animales
CREATE TABLE animals (
    animal_id BINARY(16) PRIMARY KEY,
    arete VARCHAR(20) NOT NULL UNIQUE,  -- Único a nivel global
    arete_anterior VARCHAR(20),
    sexo VARCHAR(10) NOT NULL,
    raza VARCHAR(50) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    meses INT GENERATED ALWAYS AS (TIMESTAMPDIFF(MONTH, fecha_nacimiento, CURRENT_DATE)) STORED,
    fecha_aretado DATE,
    tipo VARCHAR(20) NOT NULL DEFAULT 'venta',
    status VARCHAR(20) NOT NULL DEFAULT 'activa',
    folio_reemo VARCHAR(50),
    nota TEXT,
    madre_id BINARY(16),
    padre_id BINARY(16),
    rancho_id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    updated_by BINARY(16) NOT NULL,
    
    -- Campos para venta/muerte
    fecha_venta DATE,
    precio_venta DECIMAL(12,2),
    fecha_muerte DATE,
    motivo_muerte VARCHAR(500),
    
    CONSTRAINT chk_sexo CHECK (sexo IN ('macho', 'hembra')),
    CONSTRAINT chk_raza CHECK (raza IN ('charolais', 'angus', 'brahman', 'hereford', 
                                        'simmental', 'limousin', 'criollo', 'brangus', 
                                        'santa_gertrudis', 'cruzada')),
    CONSTRAINT chk_status CHECK (status IN ('activa', 'vendida', 'muerta', 'prestada', 
                                            'prenada', 'en_reposo')),
    CONSTRAINT chk_tipo CHECK (tipo IN ('venta', 'cria', 'engorda', 'semental', 'vientre')),
    
    INDEX idx_animals_tenant (tenant_id),
    INDEX idx_animals_rancho (rancho_id),
    INDEX idx_animals_arete (arete),
    INDEX idx_animals_status (status),
    INDEX idx_animals_madre (madre_id),
    INDEX idx_animals_padre (padre_id),
    INDEX idx_animals_folio_reemo (folio_reemo),
    
    CONSTRAINT fk_animals_madre FOREIGN KEY (madre_id) REFERENCES animals(animal_id),
    CONSTRAINT fk_animals_padre FOREIGN KEY (padre_id) REFERENCES animals(animal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de historial de movimientos entre potreros
CREATE TABLE pasture_history (
    history_id BINARY(16) PRIMARY KEY,
    animal_id BINARY(16) NOT NULL,
    potrero_id BINARY(16) NOT NULL,
    fecha_entrada TIMESTAMP(6) NOT NULL,
    fecha_salida TIMESTAMP(6),
    dias_permanencia INT GENERATED ALWAYS AS (
        CASE 
            WHEN fecha_salida IS NULL THEN DATEDIFF(CURRENT_TIMESTAMP, fecha_entrada)
            ELSE DATEDIFF(fecha_salida, fecha_entrada)
        END
    ) STORED,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    
    INDEX idx_pasture_history_animal (animal_id),
    INDEX idx_pasture_history_potrero (potrero_id),
    INDEX idx_pasture_history_current (animal_id, fecha_salida),
    
    CONSTRAINT fk_pasture_history_animal FOREIGN KEY (animal_id) REFERENCES animals(animal_id),
    UNIQUE KEY uk_animal_current_pasture (animal_id, fecha_salida)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de registros de peso
CREATE TABLE cattle_weights (
    weight_id BINARY(16) PRIMARY KEY,
    animal_id BINARY(16) NOT NULL,
    peso_kg DECIMAL(8,2) NOT NULL,
    fecha_pesaje DATE NOT NULL,
    notas TEXT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    
    INDEX idx_cattle_weights_animal (animal_id),
    INDEX idx_cattle_weights_fecha (animal_id, fecha_pesaje),
    
    CONSTRAINT fk_cattle_weights_animal FOREIGN KEY (animal_id) REFERENCES animals(animal_id),
    CONSTRAINT chk_peso_positivo CHECK (peso_kg > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de eventos de salud
CREATE TABLE health_events (
    event_id BINARY(16) PRIMARY KEY,
    animal_id BINARY(16) NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    fecha_evento DATE NOT NULL,
    descripcion TEXT NOT NULL,
    costo DECIMAL(10,2),
    veterinario_id BINARY(16),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    
    CONSTRAINT chk_event_type CHECK (event_type IN ('vaccination', 'treatment', 'birth', 'diagnosis')),
    
    INDEX idx_health_events_animal (animal_id),
    INDEX idx_health_events_fecha (animal_id, fecha_evento),
    INDEX idx_health_events_type (event_type),
    
    CONSTRAINT fk_health_events_animal FOREIGN KEY (animal_id) REFERENCES animals(animal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de auditoría de ganado
CREATE TABLE cattle_audit (
    audit_id BINARY(16) PRIMARY KEY,
    animal_id BINARY(16) NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    modified_by BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    old_values TEXT,
    new_values TEXT,
    reason VARCHAR(500),
    
    CONSTRAINT chk_operation_type CHECK (operation_type IN ('CREATE', 'UPDATE', 'CHANGE_STATUS', 
                                                             'MOVE_PASTURE', 'DELETE')),
    
    INDEX idx_cattle_audit_animal (animal_id),
    INDEX idx_cattle_audit_timestamp (timestamp),
    INDEX idx_cattle_audit_operation (operation_type),
    
    CONSTRAINT fk_cattle_audit_animal FOREIGN KEY (animal_id) REFERENCES animals(animal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## Error Handling

### Excepciones de Dominio

```java
package mx.vacapp.cattle.internal.domain.exceptions;

public class CattleDomainException extends RuntimeException {
    public CattleDomainException(String message) {
        super(message);
    }
}

public class DuplicateAreteException extends CattleDomainException {
    public DuplicateAreteException(String message) {
        super(message);
    }
}

public class InvalidGenealogyException extends CattleDomainException {
    public InvalidGenealogyException(String message) {
        super(message);
    }
}

public class InvalidPastureException extends CattleDomainException {
    public InvalidPastureException(String message) {
        super(message);
    }
}

public class SoldOrDeadAnimalException extends CattleDomainException {
    public SoldOrDeadAnimalException(String message) {
        super(message);
    }
}

public class AnimalNotFoundException extends CattleDomainException {
    public AnimalNotFoundException(String message) {
        super(message);
    }
}
```

## Testing Strategy

El módulo cattle-inventory requiere un enfoque de testing exhaustivo que incluye:

1. **Pruebas Unitarias**: Para lógica de dominio pura (Animal, AgeCalculator), casos de uso con mocks
2. **Pruebas de Integración**: Para controllers, repositorios JPA, integración con geographic-control
3. **Property-Based Testing**: Para validaciones críticas (arete único, genealogía válida, movimientos)
4. **Tests de Seguridad**: Para verificar filtrado multi-tenant en todas las operaciones

### Propiedades Críticas a Verificar

1. **Arete único global**: ∀ animales a1,a2: a1.arete = a2.arete ⇒ a1.id = a2.id
2. **Genealogía válida**: madre.sexo = HEMBRA ∧ padre.sexo = MACHO
3. **Ubicación única**: ∀ animal: COUNT(pasture_history WHERE fecha_salida IS NULL) ≤ 1
4. **Edad calculada correctamente**: meses = TIMESTAMPDIFF(MONTH, fecha_nacimiento, NOW())
5. **Estado vendido/muerto inmutable**: animal.status IN (Vendida, Muerta) ⇒ no permitir modificaciones

## Notes

- Este módulo es crítico y debe probarse exhaustivamente antes de producción
- La validación de arete único debe ser a nivel global (no por tenant) para escalabilidad futura
- El cálculo de meses debe ser en tiempo real (campo generado en BD) para precisión
- La integración con geographic-control es fundamental: siempre validar potrero antes de asignar
- Los eventos de parto (BIRTH) crean automáticamente un nuevo animal - implementar transacción atómica
- Considerar índices adicionales si las consultas de genealogía se vuelven lentas
- El historial de movimientos puede crecer considerablemente - implementar particionamiento si es necesario
- Los registros de auditoría deben retenerse por mínimo 2 años según regulaciones
