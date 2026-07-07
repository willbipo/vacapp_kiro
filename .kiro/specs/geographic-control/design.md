# Design Document: Geographic Control Module

## Overview

El módulo **geographic-control** es un componente core de Vacapp que proporciona funcionalidades completas para la gestión jerárquica de terrenos ganaderos. Este módulo implementa una estructura de tres niveles (Rancho → Sección → Potrero) con validación automática de integridad de superficies, soporte multitenant, y cálculo de métricas en tiempo real.

### Contexto

Vacapp es un monolito modular construido con Spring Modulith siguiendo Clean Architecture. El módulo geographic-control debe implementarse después de user-management ya que depende de autenticación JWT y contexto de tenant. Este módulo es fundamental ya que otros módulos (gestión de ganado, producción, salud) dependen de la estructura geográfica para asociar animales y eventos a ubicaciones específicas.

### Objetivos del Módulo

1. **Gestión jerárquica**: Permitir configuración simple (Rancho → Potreros) o compleja (Rancho → Secciones → Potreros)
2. **Validación de integridad**: Garantizar que suma de superficies de hijos ≤ superficie del padre
3. **Multi-tenancy**: Aislamiento completo de datos geográficos por organización
4. **Cálculo de métricas**: Superficie usada, disponible, porcentaje de uso en tiempo real
5. **Auditoría completa**: Registro de todos los cambios en estructura geográfica
6. **Performance**: Optimización de queries y caching de estadísticas

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

El módulo geographic-control sigue estrictamente la arquitectura hexagonal con separación de responsabilidades en tres capas principales:

```
mx.vacapp.geography/
│
├── GeographyService.java               ← API PÚBLICA (único punto de entrada para otros módulos)
│
└── internal/                           ← TODO lo demás es PRIVADO (inaccesible desde otros módulos)
    │
    ├── domain/                         ← Capa de Dominio (lógica de negocio pura)
    │   ├── model/                      ← Entidades de negocio (POJOs sin JPA)
    │   │   ├── Rancho.java              ← Entidad principal de rancho
    │   │   ├── Seccion.java            ← Entidad sección (opcional)
    │   │   ├── Potrero.java            ← Entidad potrero
    │   │   ├── GeographicStatus.java   ← Enum de estados (ACTIVE, ARCHIVED)
    │   │   ├── SurfaceCalculator.java  ← Value Object para cálculos de superficie
    │   │   └── exceptions/             ← Excepciones de dominio
    │   │       ├── EntityNotFoundException.java
    │   │       ├── DuplicateNameException.java
    │   │       ├── SurfaceExceededException.java
    │   │       ├── CannotArchiveWithChildrenException.java
    │   │       └── CattleAssignedException.java
    │   │
    │   └── repository/                 ← Puertos de salida (interfaces)
    │       ├── RanchoRepository.java   ← Puerto de persistencia de ranchos
    │       ├── SeccionRepository.java  ← Puerto de persistencia de secciones
    │       ├── PotreroRepository.java  ← Puerto de persistencia de potreros
    │       └── GeographyAuditRepository.java  ← Puerto de auditoría
    │
    ├── application/                    ← Capa de Aplicación (casos de uso)
    │   └── usecases/
    │       ├── rancho/                 ← Casos de uso de ranchos
    │       │   ├── CreateRanchoUseCase.java
    │       │   ├── UpdateRanchoUseCase.java
    │       │   ├── GetRanchoUseCase.java
    │       │   ├── ListRanchosUseCase.java
    │       │   ├── ArchiveRanchoUseCase.java
    │       │   └── GetRanchoStatsUseCase.java
    │       ├── seccion/                ← Casos de uso de secciones
    │       │   ├── CreateSeccionUseCase.java
    │       │   ├── UpdateSeccionUseCase.java
    │       │   ├── GetSeccionUseCase.java
    │       │   ├── ListSeccionesUseCase.java
    │       │   └── ArchiveSeccionUseCase.java
    │       ├── potrero/                ← Casos de uso de potreros
    │       │   ├── CreatePotreroUseCase.java
    │       │   ├── UpdatePotreroUseCase.java
    │       │   ├── GetPotreroUseCase.java
    │       │   ├── ListPotrerosUseCase.java
    │       │   └── ArchivePotreroUseCase.java
    │       └── commands/               ← Comandos y resultados (records)
    │           ├── CreateRanchoCommand.java
    │           ├── UpdateRanchoCommand.java
    │           ├── RanchoResult.java
    │           ├── CreateSeccionCommand.java
    │           ├── UpdateSeccionCommand.java
    │           ├── SeccionResult.java
    │           ├── CreatePotreroCommand.java
    │           ├── UpdatePotreroCommand.java
    │           └── PotreroResult.java
    │
    └── infrastructure/                 ← Capa de Infraestructura (adaptadores)
        │
        ├── controllers/                ← Adaptadores de entrada HTTP
        │   ├── mobile/                 ← Controladores REST (API JSON)
        │   │   ├── RanchoRestController.java
        │   │   ├── SeccionRestController.java
        │   │   ├── PotreroRestController.java
        │   │   └── dtos/               ← DTOs Request/Response (Records)
        │   │       ├── CreateRanchoRequest.java
        │   │       ├── UpdateRanchoRequest.java
        │   │       ├── RanchoResponse.java
        │   │       ├── RanchoStatsResponse.java
        │   │       ├── CreateSeccionRequest.java
        │   │       ├── UpdateSeccionRequest.java
        │   │       ├── SeccionResponse.java
        │   │       ├── CreatePotreroRequest.java
        │   │       ├── UpdatePotreroRequest.java
        │   │       └── PotreroResponse.java
        │   │
        │   └── web/                    ← Controladores MVC (Thymeleaf)
        │       ├── RanchoWebController.java
        │       ├── SeccionWebController.java
        │       ├── PotreroWebController.java
        │       └── dtos/               ← Form DTOs (Records)
        │           ├── RanchoFormDto.java
        │           ├── SeccionFormDto.java
        │           └── PotreroFormDto.java
        │
        ├── persistence/                ← Adaptador de salida (JPA)
        │   ├── entities/               ← Entidades JPA
        │   │   ├── RanchoEntity.java   ← @Entity con @Table("ranchos")
        │   │   ├── SeccionEntity.java  ← @Entity con @Table("secciones")
        │   │   ├── PotreroEntity.java  ← @Entity con @Table("potreros")
        │   │   └── GeographyAuditEntity.java ← @Entity con @Table("geography_audit")
        │   ├── repositories/           ← Repositorios JPA
        │   │   ├── RanchoJpaRepository.java
        │   │   ├── SeccionJpaRepository.java
        │   │   ├── PotreroJpaRepository.java
        │   │   └── GeographyAuditJpaRepository.java
        │   ├── impl/                   ← Implementaciones de puertos
        │   │   ├── RanchoRepositoryImpl.java
        │   │   ├── SeccionRepositoryImpl.java
        │   │   ├── PotreroRepositoryImpl.java
        │   │   └── GeographyAuditRepositoryImpl.java
        │   └── mappers/                ← Transformación entre capas
        │       ├── RanchoMapper.java
        │       ├── SeccionMapper.java
        │       ├── PotreroMapper.java
        │       └── AuditMapper.java
        │
        ├── cache/                      ← Configuración de caching
        │   ├── CacheConfig.java        ← Configuración Caffeine
        │   └── CacheNames.java         ← Constantes de nombres de cache
        │
        └── config/                     ← Configuración del módulo
            ├── GeographyModuleConfig.java  ← Beans del módulo
            └── OpenApiConfig.java          ← Configuración Swagger
```


### Flujo de Datos

#### Flujo de Creación de Rancho

```
1. Usuario envía POST /api/v1/geography/ranchos con {nombre, superficie_total}
   ↓
2. RanchoRestController recibe CreateRanchoRequest, valida con @Valid
   ↓
3. RanchoRestController mapea a CreateRanchoCommand
   ↓
4. CreateRanchoUseCase.execute(CreateRanchoCommand)
   ↓
5. RanchoRepository.existsByNombreAndTenantId(nombre, tenantId) → valida unicidad
   ↓
6. Rancho.create(...) → crea entidad de dominio con status ACTIVE
   ↓
7. RanchoRepository.save(rancho) → persiste (filtra automáticamente por tenant_id)
   ↓
8. GeographyAuditRepository.logRanchoCreation(rancho, createdBy) → auditoría
   ↓
9. Retorna RanchoResult con superficie_disponible = superficie_total
   ↓
10. RanchoRestController mapea a RanchoResponse
    ↓
11. ResponseEntity.status(201).body(RanchoResponse) → HTTP 201
```

#### Flujo de Creación de Potrero con Validación de Superficie

```
1. Usuario envía POST /api/v1/geography/potreros con {nombre, superficie, rancho_id, seccion_id (opcional)}
   ↓
2. PotreroRestController recibe CreatePotreroRequest, valida con @Valid
   ↓
3. PotreroRestController mapea a CreatePotreroCommand
   ↓
4. CreatePotreroUseCase.execute(CreatePotreroCommand)
   ↓
5. RanchoRepository.findById(ranchoId) → valida que rancho existe
   ↓
6. IF seccion_id NOT NULL:
   |  SeccionRepository.findById(seccionId) → valida que sección existe
   |  Validar: seccion.rancho_id = rancho_id
   |  Calcular: superficieDisponible = seccion.superficie - SUM(potreros de sección)
   ELSE:
   |  Calcular: superficieDisponible = rancho.superficie_total - SUM(potreros directos de rancho)
   ↓
7. IF superficie_nueva > superficieDisponible:
   |  THROW SurfaceExceededException("La superficie excede la disponible")
   ↓
8. Potrero.create(...) → crea entidad de dominio con cattle_count = 0
   ↓
9. PotreroRepository.save(potrero) → persiste
   ↓
10. Actualizar superficie_disponible del contenedor padre (Rancho o Sección)
    ↓
11. GeographyAuditRepository.logPotreroCreation(potrero, createdBy) → auditoría
    ↓
12. Retorna PotreroResult
    ↓
13. ResponseEntity.status(201).body(PotreroResponse) → HTTP 201
```

#### Flujo de Cálculo de Estadísticas con Cache

```
1. Usuario envía GET /api/v1/geography/ranchos/{id}/estadisticas
   ↓
2. RanchoRestController.getEstadisticas(id)
   ↓
3. GetRanchoStatsUseCase.execute(id)
   ↓
4. Spring Cache verifica: existe en cache "stats:rancho:{id}:tenant:{tenantId}"?
   ↓
5. IF cache hit:
   |  Retornar RanchoStatsResult desde cache (sin query DB)
   ELSE:
   |  RanchoRepository.findById(id) → obtener rancho
   |  SeccionRepository.findByRanchoId(id) → obtener secciones
   |  PotreroRepository.findByRanchoId(id) → obtener potreros
   |  SurfaceCalculator.calculate(...) → calcular métricas
   |  Construir RanchoStatsResult con: total_secciones, total_potreros,
   |    superficie_usada, superficie_disponible, porcentaje_uso,
   |    distribucion_por_seccion[]
   |  Almacenar en cache con TTL 5 minutos
   ↓
6. Retornar RanchoStatsResult
   ↓
7. RanchoRestController mapea a RanchoStatsResponse
   ↓
8. ResponseEntity.ok(RanchoStatsResponse) → HTTP 200
```

### Principios de Diseño

1. **Encapsulamiento estricto**: Solo `GeographyService.java` es público, todo bajo `internal/` es privado
2. **Separación de capas**: Dominio no depende de infraestructura (sin anotaciones JPA en domain/model)
3. **Dependency Inversion**: Domain define interfaces (puertos), Infrastructure las implementa
4. **Single Responsibility**: Cada UseCase tiene una única responsabilidad
5. **Immutability**: Records para DTOs y comandos (inmutables por defecto)
6. **Fail-fast**: Validación temprana en controllers con Bean Validation
7. **Integridad jerárquica**: Validación automática de superficies en cada operación



## Components and Interfaces

### 1. API Pública del Módulo

#### GeographyService.java

```java
package mx.vacapp.geography;

import java.util.Optional;
import java.util.UUID;

/**
 * API pública del módulo de geografía.
 * Único punto de entrada para otros módulos de Vacapp.
 */
public interface GeographyService {
    
    /**
     * Valida si un potrero existe y está activo.
     * 
     * @param potreroId UUID del potrero
     * @return true si el potrero existe y está activo
     */
    boolean isPotreroActive(UUID potreroId);
    
    /**
     * Obtiene el tenant_id de un rancho.
     * 
     * @param ranchoId UUID del rancho
     * @return Optional con el tenant_id, o empty si no existe
     */
    Optional<UUID> getRanchoTenantId(UUID ranchoId);
    
    /**
     * Verifica si un potrero tiene espacio disponible para asignar ganado.
     * 
     * @param potreroId UUID del potrero
     * @param cantidadGanado cantidad de animales a asignar
     * @return true si hay capacidad
     */
    boolean hasCapacity(UUID potreroId, int cantidadGanado);
    
    /**
     * Obtiene la superficie de un potrero.
     * 
     * @param potreroId UUID del potrero
     * @return Optional con la superficie en metros cuadrados
     */
    Optional<Long> getPotreroSurface(UUID potreroId);
}
```

### 2. Capa de Dominio

#### Rancho.java (Entidad de Dominio)

```java
package mx.vacapp.geography.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad de dominio que representa un rancho (terreno total).
 * POJO puro sin anotaciones de Spring/JPA.
 */
public class Rancho {
    private final UUID ranchoId;
    private final String nombre;
    private final BigDecimal superficieTotal;  // En metros cuadrados
    private final String descripcion;
    private final GeographicStatus status;
    private final UUID tenantId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID createdBy;
    private final UUID updatedBy;
    
    // Constructor privado para forzar uso de factory methods
    private Rancho(Builder builder) {
        this.ranchoId = builder.ranchoId;
        this.nombre = builder.nombre;
        this.superficieTotal = builder.superficieTotal;
        this.descripcion = builder.descripcion;
        this.status = builder.status;
        this.tenantId = builder.tenantId;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
    }
    
    // Factory method para crear nuevo rancho
    public static Rancho create(String nombre, BigDecimal superficieTotal, 
                               String descripcion, UUID tenantId, UUID createdBy) {
        Instant now = Instant.now();
        return new Builder()
            .ranchoId(UUID.randomUUID())
            .nombre(nombre.trim())
            .superficieTotal(superficieTotal)
            .descripcion(descripcion)
            .status(GeographicStatus.ACTIVE)
            .tenantId(tenantId)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
    }
    
    // Métodos de negocio
    public boolean isActive() {
        return this.status == GeographicStatus.ACTIVE;
    }
    
    public Rancho updateSuperficie(BigDecimal newSuperficie, UUID updatedBy) {
        return new Builder()
            .from(this)
            .superficieTotal(newSuperficie)
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();
    }
    
    public Rancho archive(UUID archivedBy) {
        return new Builder()
            .from(this)
            .status(GeographicStatus.ARCHIVED)
            .updatedAt(Instant.now())
            .updatedBy(archivedBy)
            .build();
    }
    
    // Getters (sin setters - inmutabilidad)
    public UUID getRanchoId() { return ranchoId; }
    public String getNombre() { return nombre; }
    public BigDecimal getSuperficieTotal() { return superficieTotal; }
    public String getDescripcion() { return descripcion; }
    public GeographicStatus getStatus() { return status; }
    public UUID getTenantId() { return tenantId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    
    // Builder pattern para inmutabilidad
    public static class Builder {
        private UUID ranchoId;
        private String nombre;
        private BigDecimal superficieTotal;
        private String descripcion;
        private GeographicStatus status;
        private UUID tenantId;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;
        
        public Builder ranchoId(UUID ranchoId) { this.ranchoId = ranchoId; return this; }
        public Builder nombre(String nombre) { this.nombre = nombre; return this; }
        public Builder superficieTotal(BigDecimal superficieTotal) { 
            this.superficieTotal = superficieTotal; return this; 
        }
        public Builder descripcion(String descripcion) { 
            this.descripcion = descripcion; return this; 
        }
        public Builder status(GeographicStatus status) { this.status = status; return this; }
        public Builder tenantId(UUID tenantId) { this.tenantId = tenantId; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder createdBy(UUID createdBy) { this.createdBy = createdBy; return this; }
        public Builder updatedBy(UUID updatedBy) { this.updatedBy = updatedBy; return this; }
        
        public Builder from(Rancho rancho) {
            this.ranchoId = rancho.ranchoId;
            this.nombre = rancho.nombre;
            this.superficieTotal = rancho.superficieTotal;
            this.descripcion = rancho.descripcion;
            this.status = rancho.status;
            this.tenantId = rancho.tenantId;
            this.createdAt = rancho.createdAt;
            this.updatedAt = rancho.updatedAt;
            this.createdBy = rancho.createdBy;
            this.updatedBy = rancho.updatedBy;
            return this;
        }
        
        public Rancho build() {
            return new Rancho(this);
        }
    }
}
```


#### Seccion.java (Entidad de Dominio)

```java
package mx.vacapp.geography.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad de dominio que representa una sección de rancho.
 */
public class Seccion {
    private final UUID seccionId;
    private final String nombre;
    private final BigDecimal superficie;
    private final UUID ranchoId;
    private final String descripcion;
    private final GeographicStatus status;
    private final UUID tenantId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID createdBy;
    private final UUID updatedBy;
    
    private Seccion(Builder builder) {
        this.seccionId = builder.seccionId;
        this.nombre = builder.nombre;
        this.superficie = builder.superficie;
        this.ranchoId = builder.ranchoId;
        this.descripcion = builder.descripcion;
        this.status = builder.status;
        this.tenantId = builder.tenantId;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
    }
    
    public static Seccion create(String nombre, BigDecimal superficie, UUID ranchoId,
                                String descripcion, UUID tenantId, UUID createdBy) {
        Instant now = Instant.now();
        return new Builder()
            .seccionId(UUID.randomUUID())
            .nombre(nombre.trim())
            .superficie(superficie)
            .ranchoId(ranchoId)
            .descripcion(descripcion)
            .status(GeographicStatus.ACTIVE)
            .tenantId(tenantId)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
    }
    
    public boolean isActive() {
        return this.status == GeographicStatus.ACTIVE;
    }
    
    public Seccion archive(UUID archivedBy) {
        return new Builder()
            .from(this)
            .status(GeographicStatus.ARCHIVED)
            .updatedAt(Instant.now())
            .updatedBy(archivedBy)
            .build();
    }
    
    // Getters
    public UUID getSeccionId() { return seccionId; }
    public String getNombre() { return nombre; }
    public BigDecimal getSuperficie() { return superficie; }
    public UUID getRanchoId() { return ranchoId; }
    public String getDescripcion() { return descripcion; }
    public GeographicStatus getStatus() { return status; }
    public UUID getTenantId() { return tenantId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    
    // Builder
    public static class Builder {
        private UUID seccionId;
        private String nombre;
        private BigDecimal superficie;
        private UUID ranchoId;
        private String descripcion;
        private GeographicStatus status;
        private UUID tenantId;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;
        
        public Builder seccionId(UUID seccionId) { this.seccionId = seccionId; return this; }
        public Builder nombre(String nombre) { this.nombre = nombre; return this; }
        public Builder superficie(BigDecimal superficie) { 
            this.superficie = superficie; return this; 
        }
        public Builder ranchoId(UUID ranchoId) { this.ranchoId = ranchoId; return this; }
        public Builder descripcion(String descripcion) { 
            this.descripcion = descripcion; return this; 
        }
        public Builder status(GeographicStatus status) { this.status = status; return this; }
        public Builder tenantId(UUID tenantId) { this.tenantId = tenantId; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder createdBy(UUID createdBy) { this.createdBy = createdBy; return this; }
        public Builder updatedBy(UUID updatedBy) { this.updatedBy = updatedBy; return this; }
        
        public Builder from(Seccion seccion) {
            this.seccionId = seccion.seccionId;
            this.nombre = seccion.nombre;
            this.superficie = seccion.superficie;
            this.ranchoId = seccion.ranchoId;
            this.descripcion = seccion.descripcion;
            this.status = seccion.status;
            this.tenantId = seccion.tenantId;
            this.createdAt = seccion.createdAt;
            this.updatedAt = seccion.updatedAt;
            this.createdBy = seccion.createdBy;
            this.updatedBy = seccion.updatedBy;
            return this;
        }
        
        public Seccion build() {
            return new Seccion(this);
        }
    }
}
```


#### Potrero.java (Entidad de Dominio)

```java
package mx.vacapp.geography.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidad de dominio que representa un potrero (unidad mínima de pastoreo).
 */
public class Potrero {
    private final UUID potreroId;
    private final String nombre;
    private final BigDecimal superficie;
    private final UUID ranchoId;
    private final UUID seccionId;  // Nullable - null si está vinculado directamente al rancho
    private final Integer cattleCount;  // Cantidad de ganado asignado
    private final String descripcion;
    private final GeographicStatus status;
    private final UUID tenantId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID createdBy;
    private final UUID updatedBy;
    
    private Potrero(Builder builder) {
        this.potreroId = builder.potreroId;
        this.nombre = builder.nombre;
        this.superficie = builder.superficie;
        this.ranchoId = builder.ranchoId;
        this.seccionId = builder.seccionId;
        this.cattleCount = builder.cattleCount;
        this.descripcion = builder.descripcion;
        this.status = builder.status;
        this.tenantId = builder.tenantId;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
    }
    
    public static Potrero create(String nombre, BigDecimal superficie, UUID ranchoId,
                                UUID seccionId, String descripcion, UUID tenantId, 
                                UUID createdBy) {
        Instant now = Instant.now();
        return new Builder()
            .potreroId(UUID.randomUUID())
            .nombre(nombre.trim())
            .superficie(superficie)
            .ranchoId(ranchoId)
            .seccionId(seccionId)
            .cattleCount(0)  // Inicialmente sin ganado
            .descripcion(descripcion)
            .status(GeographicStatus.ACTIVE)
            .tenantId(tenantId)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
    }
    
    // Métodos de negocio
    public boolean isActive() {
        return this.status == GeographicStatus.ACTIVE;
    }
    
    public boolean hasDirectRanchoLink() {
        return this.seccionId == null;
    }
    
    public boolean hasCattle() {
        return this.cattleCount > 0;
    }
    
    public Potrero assignCattle(int count, UUID updatedBy) {
        return new Builder()
            .from(this)
            .cattleCount(this.cattleCount + count)
            .updatedAt(Instant.now())
            .updatedBy(updatedBy)
            .build();
    }
    
    public Potrero archive(UUID archivedBy) {
        return new Builder()
            .from(this)
            .status(GeographicStatus.ARCHIVED)
            .updatedAt(Instant.now())
            .updatedBy(archivedBy)
            .build();
    }
    
    // Getters
    public UUID getPotreroId() { return potreroId; }
    public String getNombre() { return nombre; }
    public BigDecimal getSuperficie() { return superficie; }
    public UUID getRanchoId() { return ranchoId; }
    public UUID getSeccionId() { return seccionId; }
    public Integer getCattleCount() { return cattleCount; }
    public String getDescripcion() { return descripcion; }
    public GeographicStatus getStatus() { return status; }
    public UUID getTenantId() { return tenantId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    
    // Builder
    public static class Builder {
        private UUID potreroId;
        private String nombre;
        private BigDecimal superficie;
        private UUID ranchoId;
        private UUID seccionId;
        private Integer cattleCount;
        private String descripcion;
        private GeographicStatus status;
        private UUID tenantId;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;
        
        public Builder potreroId(UUID potreroId) { this.potreroId = potreroId; return this; }
        public Builder nombre(String nombre) { this.nombre = nombre; return this; }
        public Builder superficie(BigDecimal superficie) { 
            this.superficie = superficie; return this; 
        }
        public Builder ranchoId(UUID ranchoId) { this.ranchoId = ranchoId; return this; }
        public Builder seccionId(UUID seccionId) { this.seccionId = seccionId; return this; }
        public Builder cattleCount(Integer cattleCount) { 
            this.cattleCount = cattleCount; return this; 
        }
        public Builder descripcion(String descripcion) { 
            this.descripcion = descripcion; return this; 
        }
        public Builder status(GeographicStatus status) { this.status = status; return this; }
        public Builder tenantId(UUID tenantId) { this.tenantId = tenantId; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder createdBy(UUID createdBy) { this.createdBy = createdBy; return this; }
        public Builder updatedBy(UUID updatedBy) { this.updatedBy = updatedBy; return this; }
        
        public Builder from(Potrero potrero) {
            this.potreroId = potrero.potreroId;
            this.nombre = potrero.nombre;
            this.superficie = potrero.superficie;
            this.ranchoId = potrero.ranchoId;
            this.seccionId = potrero.seccionId;
            this.cattleCount = potrero.cattleCount;
            this.descripcion = potrero.descripcion;
            this.status = potrero.status;
            this.tenantId = potrero.tenantId;
            this.createdAt = potrero.createdAt;
            this.updatedAt = potrero.updatedAt;
            this.createdBy = potrero.createdBy;
            this.updatedBy = potrero.updatedBy;
            return this;
        }
        
        public Potrero build() {
            return new Potrero(this);
        }
    }
}
```
