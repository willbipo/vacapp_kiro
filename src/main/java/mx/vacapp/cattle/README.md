# Módulo Cattle Inventory

## Descripción

El módulo `cattle-inventory` es el componente central de Vacapp que gestiona el inventario completo de ganado bovino. Proporciona funcionalidades para registro único de identificación, genealogía, trazabilidad de movimientos, historial de pesos, eventos de salud y cumplimiento regulatorio mexicano.

## Arquitectura Spring Modulith + Clean Architecture

Este módulo sigue estrictamente la arquitectura modular con encapsulamiento interno:

### API Pública

```
mx.vacapp.cattle/
├── CattleService.java          ← API PÚBLICA (único punto de entrada)
└── package-info.java
```

**CattleService** es la única interfaz pública del módulo. Otros módulos de Vacapp SOLO pueden acceder a través de esta interfaz.

### Estructura Internal (Privada)

Todo bajo `internal/` es **privado** e inaccesible desde otros módulos:

```
internal/
├── domain/                          ← Capa de Dominio
│   ├── model/                       ← Entidades de negocio puras
│   │   ├── Animal.java              ← Entidad principal
│   │   ├── Sex.java                 ← Enum (Macho, Hembra)
│   │   ├── Breed.java               ← Enum de razas
│   │   ├── CattleStatus.java        ← Enum de estados
│   │   ├── CattleType.java          ← Enum de tipos
│   │   ├── PastureAssignment.java   ← Asignación a potrero
│   │   ├── PastureHistory.java      ← Historial de movimientos
│   │   ├── WeightRecord.java        ← Registro de peso
│   │   ├── HealthEvent.java         ← Evento de salud
│   │   ├── AgeCalculator.java       ← Cálculo de edad
│   │   └── exceptions/              ← Excepciones de dominio
│   │       ├── DuplicateAreteException.java
│   │       ├── InvalidGenealogyException.java
│   │       ├── InvalidPastureException.java
│   │       ├── SoldOrDeadAnimalException.java
│   │       └── AnimalNotFoundException.java
│   │
│   └── repository/                  ← Puertos de salida (interfaces)
│       ├── AnimalRepository.java
│       ├── PastureHistoryRepository.java
│       ├── WeightRepository.java
│       ├── HealthEventRepository.java
│       └── CattleAuditRepository.java
│
├── application/                     ← Capa de Aplicación
│   └── usecases/
│       ├── animal/                  ← Casos de uso de animales
│       │   ├── CreateAnimalUseCase.java
│       │   ├── UpdateAnimalUseCase.java
│       │   ├── GetAnimalUseCase.java
│       │   ├── ListAnimalsUseCase.java
│       │   ├── ChangeStatusUseCase.java
│       │   ├── GetAnimalStatsUseCase.java
│       │   └── SearchAnimalsByAreteUseCase.java
│       │
│       ├── movement/                ← Casos de uso de movimientos
│       │   ├── MoveAnimalUseCase.java
│       │   ├── GetMovementHistoryUseCase.java
│       │   └── ListAnimalsInPastureUseCase.java
│       │
│       ├── weight/                  ← Casos de uso de pesos
│       │   ├── RecordWeightUseCase.java
│       │   ├── GetWeightHistoryUseCase.java
│       │   └── CalculateDailyGainUseCase.java
│       │
│       ├── health/                  ← Casos de uso de salud
│       │   ├── RecordHealthEventUseCase.java
│       │   ├── GetHealthHistoryUseCase.java
│       │   ├── RecordBirthEventUseCase.java
│       │   └── GetUpcomingVaccinationsUseCase.java
│       │
│       └── commands/                ← Comandos y resultados (records)
│           ├── CreateAnimalCommand.java
│           ├── UpdateAnimalCommand.java
│           ├── AnimalResult.java
│           ├── MoveAnimalCommand.java
│           ├── RecordWeightCommand.java
│           ├── WeightResult.java
│           ├── RecordHealthEventCommand.java
│           └── HealthEventResult.java
│
└── infrastructure/                  ← Capa de Infraestructura
    │
    ├── controllers/                 ← Adaptadores de entrada HTTP
    │   ├── mobile/                  ← REST API (JSON/JWT)
    │   │   ├── dtos/                ← Request/Response DTOs (Records)
    │   │   │   ├── CreateAnimalRequest.java
    │   │   │   ├── UpdateAnimalRequest.java
    │   │   │   ├── AnimalResponse.java
    │   │   │   ├── MoveAnimalRequest.java
    │   │   │   ├── PastureHistoryResponse.java
    │   │   │   ├── RecordWeightRequest.java
    │   │   │   ├── WeightResponse.java
    │   │   │   ├── RecordHealthEventRequest.java
    │   │   │   ├── HealthEventResponse.java
    │   │   │   └── CattleStatsResponse.java
    │   │   │
    │   │   ├── mappers/             ← Mappers DTO ↔ Commands/Results
    │   │   │
    │   │   ├── AnimalRestController.java
    │   │   ├── MovementRestController.java
    │   │   ├── WeightRestController.java
    │   │   └── HealthRestController.java
    │   │
    │   └── web/                     ← Controladores MVC (Thymeleaf)
    │       ├── dtos/                ← Form DTOs (Records)
    │       │   ├── AnimalFormDto.java
    │       │   ├── MoveAnimalFormDto.java
    │       │   └── HealthEventFormDto.java
    │       │
    │       ├── CattleWebController.java
    │       ├── MovementWebController.java
    │       └── HealthWebController.java
    │
    ├── persistence/                 ← Adaptador de persistencia (JPA)
    │   ├── entities/                ← Entidades JPA
    │   │   ├── AnimalEntity.java
    │   │   ├── PastureHistoryEntity.java
    │   │   ├── WeightEntity.java
    │   │   ├── HealthEventEntity.java
    │   │   └── CattleAuditEntity.java
    │   │
    │   ├── repositories/            ← Repositorios JPA
    │   │   ├── AnimalJpaRepository.java
    │   │   ├── PastureHistoryJpaRepository.java
    │   │   ├── WeightJpaRepository.java
    │   │   ├── HealthEventJpaRepository.java
    │   │   └── CattleAuditJpaRepository.java
    │   │
    │   ├── impl/                    ← Implementaciones de puertos
    │   │   ├── AnimalRepositoryImpl.java
    │   │   ├── PastureHistoryRepositoryImpl.java
    │   │   ├── WeightRepositoryImpl.java
    │   │   ├── HealthEventRepositoryImpl.java
    │   │   └── CattleAuditRepositoryImpl.java
    │   │
    │   └── mappers/                 ← Mappers Domain ↔ Entity
    │       ├── AnimalMapper.java
    │       ├── PastureHistoryMapper.java
    │       ├── WeightMapper.java
    │       ├── HealthEventMapper.java
    │       └── AuditMapper.java
    │
    ├── integration/                 ← Integración con otros módulos
    │   └── GeographyServiceClient.java
    │
    ├── cache/                       ← Configuración de caché
    │   ├── CacheConfig.java
    │   └── CacheNames.java
    │
    └── config/                      ← Configuración del módulo
        ├── CattleModuleConfig.java
        └── OpenApiConfig.java
```

## Flujo de Datos

### Ejemplo: Crear Animal

```
HTTP POST /api/v1/cattle
  ↓
AnimalRestController (infrastructure/controllers/mobile/)
  ↓ valida con @Valid
  ↓ mapea a CreateAnimalCommand
  ↓
CreateAnimalUseCase (application/usecases/animal/)
  ↓ valida arete único
  ↓ valida potrero activo (GeographyService)
  ↓ crea Animal (domain/model/)
  ↓
AnimalRepository (domain/repository/)
  ↓
AnimalRepositoryImpl (infrastructure/persistence/impl/)
  ↓ mapea a AnimalEntity
  ↓ filtra por tenant_id
  ↓
AnimalJpaRepository → MySQL
```

## Principios de Diseño

1. **Encapsulamiento estricto**: Solo `CattleService` es público
2. **Separación de capas**: Domain no depende de Infrastructure (sin JPA en domain)
3. **Dependency Inversion**: Domain define interfaces, Infrastructure implementa
4. **Single Responsibility**: Cada UseCase tiene una única responsabilidad
5. **Immutability**: Records para DTOs y comandos
6. **Fail-fast**: Validación temprana en controllers con Bean Validation

## Reglas de Integración

### Para otros módulos de Vacapp

- ✅ **Permitido**: Importar `mx.vacapp.cattle.CattleService`
- ❌ **Prohibido**: Importar cualquier cosa bajo `mx.vacapp.cattle.internal.*`

### Ejemplo correcto

```java
package mx.vacapp.othermodule;

import mx.vacapp.cattle.CattleService;  // ✅ Permitido

public class OtherModuleService {
    private final CattleService cattleService;  // ✅ Correcto
    
    public void someMethod(UUID animalId) {
        boolean isActive = cattleService.isAnimalActive(animalId);  // ✅ Correcto
    }
}
```

### Ejemplo incorrecto

```java
package mx.vacapp.othermodule;

// ❌ ERROR: No se puede importar clases internas
import mx.vacapp.cattle.internal.domain.model.Animal;  // ❌ PROHIBIDO
import mx.vacapp.cattle.internal.application.usecases.animal.CreateAnimalUseCase;  // ❌ PROHIBIDO

// Spring Modulith detectará esta violación en tiempo de compilación
```

## Tecnologías

- **Java 21**
- **Spring Boot 4.1.0**
- **Spring Modulith** (encapsulamiento)
- **Spring Data JPA** (persistencia)
- **MySQL 8** (base de datos)
- **Bean Validation** (validación)
- **OpenAPI 3.0** (documentación API)
- **Thymeleaf** (vistas web)
- **Lombok** (reducción boilerplate)

## Referencias

- Ver `requirements.md` para requisitos funcionales completos
- Ver `design.md` para decisiones de arquitectura detalladas
- Ver `tasks.md` para plan de implementación
