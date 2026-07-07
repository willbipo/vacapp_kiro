# Tasks Document: Geographic Control Module

## Overview

Este documento desglosa la implementación del módulo **geographic-control** en tareas específicas organizadas por fase de desarrollo. El módulo implementa gestión jerárquica de terrenos ganaderos (Rancho → Sección → Potrero) siguiendo Spring Modulith y Clean Architecture.

---

## Phase 1: Module Structure & Domain Layer

### Task 1.1: Create Module Base Structure

**Description:** Crear la estructura base del módulo geographic-control siguiendo Spring Modulith.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/GeographyService.java` (API pública)
- `src/main/java/mx/vacapp/geography/internal/` (directorio interno)
- `src/main/java/mx/vacapp/geography/README.md` (documentación del módulo)

**Dependencies:** None

**Acceptance Criteria:**
- La API pública `GeographyService.java` debe estar en la raíz del paquete `mx.vacapp.geography`
- Todo el código interno debe estar bajo `internal/` package
- El README.md debe documentar la responsabilidad del módulo y su API pública

---

### Task 1.2: Create Domain Model - Rancho Entity

**Description:** Implementar la entidad de dominio `Rancho` como POJO puro sin anotaciones JPA.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/domain/model/Rancho.java`

**Dependencies:** Task 1.1

**Acceptance Criteria:**
- La clase debe ser un POJO inmutable con patrón Builder
- Debe incluir factory method `create()` para nuevas instancias
- Debe incluir métodos de negocio: `isActive()`, `updateSuperficie()`, `archive()`
- Todos los campos deben tener getters, sin setters (inmutabilidad)
- Debe usar `BigDecimal` para superficieTotal
- Debe usar `Instant` para timestamps

---

### Task 1.3: Create Domain Model - Seccion Entity

**Description:** Implementar la entidad de dominio `Seccion` como POJO puro.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/domain/model/Seccion.java`

**Dependencies:** Task 1.1

**Acceptance Criteria:**
- Clase inmutable con patrón Builder
- Factory method `create()` y método `archive()`
- Debe incluir referencia a `ranchoId` (UUID)
- Campo `superficie` como `BigDecimal`
- Sin anotaciones de infraestructura (JPA, Spring)

---

### Task 1.4: Create Domain Model - Potrero Entity

**Description:** Implementar la entidad de dominio `Potrero` como POJO puro.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/domain/model/Potrero.java`

**Dependencies:** Task 1.1

**Acceptance Criteria:**
- Clase inmutable con patrón Builder
- Factory method `create()` con `cattleCount` inicializado en 0
- Debe incluir métodos: `isActive()`, `hasDirectRanchoLink()`, `hasCattle()`, `assignCattle()`, `archive()`
- Debe incluir `seccionId` como UUID nullable
- Incluir `ranchoId` y `cattleCount` (Integer)

---

### Task 1.5: Create Domain Enums and Value Objects

**Description:** Crear enum `GeographicStatus` y value object `SurfaceCalculator`.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/domain/model/GeographicStatus.java`
- `src/main/java/mx/vacapp/geography/internal/domain/model/SurfaceCalculator.java`

**Dependencies:** Task 1.1

**Acceptance Criteria:**
- `GeographicStatus` debe tener valores: `ACTIVE`, `ARCHIVED`
- `SurfaceCalculator` debe implementar métodos estáticos para cálculos de superficie
- Métodos en `SurfaceCalculator`: `calculateAvailable()`, `calculateUsagePercentage()`, `validateSurfaceSum()`

---

### Task 1.6: Create Domain Exceptions

**Description:** Implementar excepciones de dominio para validaciones de negocio.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/domain/model/exceptions/EntityNotFoundException.java`
- `src/main/java/mx/vacapp/geography/internal/domain/model/exceptions/DuplicateNameException.java`
- `src/main/java/mx/vacapp/geography/internal/domain/model/exceptions/SurfaceExceededException.java`
- `src/main/java/mx/vacapp/geography/internal/domain/model/exceptions/CannotArchiveWithChildrenException.java`
- `src/main/java/mx/vacapp/geography/internal/domain/model/exceptions/CattleAssignedException.java`

**Dependencies:** Task 1.1

**Acceptance Criteria:**
- Todas las excepciones deben extender `RuntimeException`
- Cada excepción debe tener constructor con mensaje descriptivo
- Incluir constructor con mensaje y causa (Throwable)

---

### Task 1.7: Create Domain Repository Ports

**Description:** Definir interfaces de repositorio (puertos de salida) en capa de dominio.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/domain/repository/RanchoRepository.java`
- `src/main/java/mx/vacapp/geography/internal/domain/repository/SeccionRepository.java`
- `src/main/java/mx/vacapp/geography/internal/domain/repository/PotreroRepository.java`
- `src/main/java/mx/vacapp/geography/internal/domain/repository/GeographyAuditRepository.java`

**Dependencies:** Tasks 1.2, 1.3, 1.4

**Acceptance Criteria:**
- Interfaces puras sin anotaciones de Spring
- Métodos con nombres descriptivos: `save()`, `findById()`, `findByRanchoId()`, `existsByNombreAndTenantId()`
- Usar tipos de dominio (`Rancho`, `Seccion`, `Potrero`), no entidades JPA
- Retornar `Optional<T>` para búsquedas que pueden no existir
- Incluir métodos para cálculos: `sumSuperficieByRanchoId()`, `countByRanchoId()`

---

## Phase 2: Persistence Layer (Infrastructure)

### Task 2.1: Create JPA Entities

**Description:** Implementar entidades JPA con anotaciones de persistencia.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/entities/RanchoEntity.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/entities/SeccionEntity.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/entities/PotreroEntity.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/entities/GeographyAuditEntity.java`

**Dependencies:** Tasks 1.2, 1.3, 1.4, 1.5

**Acceptance Criteria:**
- Usar anotaciones JPA: `@Entity`, `@Table`, `@Id`, `@Column`, `@Enumerated`
- IDs como UUID con `@GeneratedValue(strategy = GenerationType.UUID)`
- Incluir columna `tenant_id` (UUID) con índice en todas las entidades
- Nombres de tablas: `ranchos`, `secciones`, `potreros`, `geography_audit`
- Usar `@Enumerated(EnumType.STRING)` para `GeographicStatus`
- Incluir timestamps: `created_at`, `updated_at` con `@Column(name = "...")`
- `RanchoEntity` debe incluir: `rancho_id`, `nombre`, `superficie_total`, `descripcion`, `status`, `tenant_id`, `created_at`, `updated_at`, `created_by`, `updated_by`
- `SeccionEntity` debe incluir FK `rancho_id` con `@ManyToOne(fetch = FetchType.LAZY)`
- `PotreroEntity` debe incluir FK `rancho_id` y FK nullable `seccion_id`
- `GeographyAuditEntity` debe incluir: `audit_id`, `entity_type`, `entity_id`, `operation_type`, `timestamp`, `modified_by`, `tenant_id`, `old_values`, `new_values`, `reason`

---

### Task 2.2: Create JPA Repositories

**Description:** Crear interfaces Spring Data JPA Repository.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/repositories/RanchoJpaRepository.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/repositories/SeccionJpaRepository.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/repositories/PotreroJpaRepository.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/repositories/GeographyAuditJpaRepository.java`

**Dependencies:** Task 2.1

**Acceptance Criteria:**
- Extender `JpaRepository<EntityType, UUID>`
- Incluir métodos de consulta derivados: `existsByNombreAndTenantIdIgnoreCase()`, `findByTenantId()`, `findByRanchoIdAndTenantId()`
- Incluir queries con `@Query` para cálculos: `@Query("SELECT SUM(e.superficie) FROM SeccionEntity e WHERE e.ranchoId = :ranchoId AND e.status = 'ACTIVE'")`
- Usar `Page<T>` para métodos con paginación
- Aplicar filtro `tenant_id` en todas las queries personalizadas

---

### Task 2.3: Create Entity Mappers

**Description:** Implementar clases Mapper para transformar entre entidades JPA y objetos de dominio.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/mappers/RanchoMapper.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/mappers/SeccionMapper.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/mappers/PotreroMapper.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/mappers/AuditMapper.java`

**Dependencies:** Tasks 2.1, 1.2, 1.3, 1.4

**Acceptance Criteria:**
- Clases con anotación `@Component`
- Métodos `toEntity(DomainModel domain)` y `toDomain(EntityJPA entity)`
- Usar Lombok `@RequiredArgsConstructor` si hay dependencias
- Manejar conversión de tipos: `Instant` ↔ timestamps, `BigDecimal` ↔ numeric columns
- No exponer lógica de negocio en mappers (transformación pura)

---

### Task 2.4: Implement Repository Adapters

**Description:** Implementar adaptadores que conectan puertos de dominio con repositorios JPA.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/impl/RanchoRepositoryImpl.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/impl/SeccionRepositoryImpl.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/impl/PotreroRepositoryImpl.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/persistence/impl/GeographyAuditRepositoryImpl.java`

**Dependencies:** Tasks 1.7, 2.2, 2.3

**Acceptance Criteria:**
- Implementar interfaces de `domain/repository/`
- Usar anotación `@Repository` y `@RequiredArgsConstructor` (Lombok)
- Inyectar `JpaRepository` correspondiente y `Mapper`
- Aplicar filtro `tenant_id` extrayéndolo del contexto de seguridad en todos los métodos
- Lanzar excepciones de dominio (`EntityNotFoundException`) cuando no se encuentren recursos
- Incluir logs con SLF4J para trazabilidad

---

## Phase 3: Application Layer (Use Cases)

### Task 3.1: Create Command and Result Records

**Description:** Definir Commands y Results como Java Records para casos de uso.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/application/usecases/commands/CreateRanchoCommand.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/commands/UpdateRanchoCommand.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/commands/RanchoResult.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/commands/CreateSeccionCommand.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/commands/UpdateSeccionCommand.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/commands/SeccionResult.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/commands/CreatePotreroCommand.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/commands/UpdatePotreroCommand.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/commands/PotreroResult.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/commands/RanchoStatsResult.java`

**Dependencies:** Task 1.1

**Acceptance Criteria:**
- Todos deben ser Java Records (inmutables)
- Commands incluyen parámetros necesarios: `nombre`, `superficie`, `ranchoId`, `tenantId`, `userId`
- Results incluyen datos de salida: ID, nombre, superficie, estadísticas calculadas
- `RanchoStatsResult` debe incluir: `totalSecciones`, `totalPotreros`, `superficieTotal`, `superficieUsada`, `superficieDisponible`, `porcentajeUso`, `distribucionPorSeccion[]`

---

### Task 3.2: Implement Rancho Use Cases

**Description:** Crear casos de uso para gestión de ranchos.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/application/usecases/rancho/CreateRanchoUseCase.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/rancho/UpdateRanchoUseCase.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/rancho/GetRanchoUseCase.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/rancho/ListRanchosUseCase.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/rancho/ArchiveRanchoUseCase.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/rancho/GetRanchoStatsUseCase.java`

**Dependencies:** Tasks 1.7, 3.1

**Acceptance Criteria:**
- Clases con anotación `@Service` y `@RequiredArgsConstructor`
- Método principal `execute(Command)` retornando `Result`
- `CreateRanchoUseCase`: validar unicidad de nombre, crear Rancho con status ACTIVE, registrar auditoría
- `UpdateRanchoUseCase`: validar que nueva superficie >= superficie usada por hijos
- `ArchiveRanchoUseCase`: validar que no existan secciones/potreros activos antes de archivar
- `GetRanchoStatsUseCase`: calcular métricas usando `SurfaceCalculator`
- `ListRanchosUseCase`: soportar paginación con `Pageable`
- Usar anotación `@Transactional` en operaciones de escritura

---

### Task 3.3: Implement Seccion Use Cases

**Description:** Crear casos de uso para gestión de secciones.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/application/usecases/seccion/CreateSeccionUseCase.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/seccion/UpdateSeccionUseCase.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/seccion/GetSeccionUseCase.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/seccion/ListSeccionesUseCase.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/seccion/ArchiveSeccionUseCase.java`

**Dependencies:** Tasks 1.7, 3.1

**Acceptance Criteria:**
- Anotaciones `@Service`, `@RequiredArgsConstructor`, `@Transactional` en operaciones de escritura
- `CreateSeccionUseCase`: validar que suma de superficies de secciones <= rancho.superficieTotal
- `UpdateSeccionUseCase`: validar superficie >= superficie usada por potreros Y suma total <= rancho.superficieTotal
- `ArchiveSeccionUseCase`: validar que no existan potreros activos en la sección
- `ListSeccionesUseCase`: filtrar por `ranchoId` y `tenantId`

---

### Task 3.4: Implement Potrero Use Cases

**Description:** Crear casos de uso para gestión de potreros.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/application/usecases/potrero/CreatePotreroUseCase.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/potrero/UpdatePotreroUseCase.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/potrero/GetPotreroUseCase.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/potrero/ListPotrerosUseCase.java`
- `src/main/java/mx/vacapp/geography/internal/application/usecases/potrero/ArchivePotreroUseCase.java`

**Dependencies:** Tasks 1.7, 3.1

**Acceptance Criteria:**
- Anotaciones `@Service`, `@RequiredArgsConstructor`, `@Transactional`
- `CreatePotreroUseCase`: validar si potrero vinculado a rancho o sección, calcular superficie disponible, validar que nueva superficie <= disponible
- Si rancho tiene configuración compleja (con secciones), prohibir crear potreros directamente vinculados al rancho
- `ArchivePotreroUseCase`: validar que `cattleCount == 0` antes de archivar
- `ListPotrerosUseCase`: soportar filtrado por `ranchoId` o `seccionId`

---

## Phase 4: REST API Controllers & DTOs

### Task 4.1: Define OpenAPI Specification (YAML)

**Description:** Crear especificación OpenAPI 3.0 con todos los endpoints REST del módulo.

**Files to Create:**
- `src/main/resources/openapi/openapi-geography.yaml`

**Dependencies:** None

**Acceptance Criteria:**
- Definir ruta base: `/api/v1/geography`
- Endpoints de Ranchos: `POST /ranchos`, `GET /ranchos`, `GET /ranchos/{id}`, `PUT /ranchos/{id}`, `DELETE /ranchos/{id}`, `GET /ranchos/{id}/estadisticas`
- Endpoints de Secciones: `POST /secciones`, `GET /secciones`, `GET /secciones/{id}`, `PUT /secciones/{id}`, `DELETE /secciones/{id}`, `GET /ranchos/{ranchoId}/secciones`
- Endpoints de Potreros: `POST /potreros`, `GET /potreros`, `GET /potreros/{id}`, `PUT /potreros/{id}`, `DELETE /potreros/{id}`, `GET /ranchos/{ranchoId}/potreros`, `GET /secciones/{seccionId}/potreros`
- Definir schemas para Request/Response DTOs en `components/schemas`
- Incluir validaciones en schemas: `minLength`, `maxLength`, `minimum`, `maximum`, `pattern`
- Definir responses: 200, 201, 204, 400, 401, 403, 404, 409
- Incluir security requirement: `bearerAuth` (JWT)

---

### Task 4.2: Create REST Request DTOs

**Description:** Crear DTOs de entrada como Java Records con Bean Validation.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/dtos/CreateRanchoRequest.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/dtos/UpdateRanchoRequest.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/dtos/CreateSeccionRequest.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/dtos/UpdateSeccionRequest.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/dtos/CreatePotreroRequest.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/dtos/UpdatePotreroRequest.java`

**Dependencies:** Task 4.1

**Acceptance Criteria:**
- Todos deben ser Java Records
- Incluir anotaciones Bean Validation: `@NotNull`, `@NotBlank`, `@Size`, `@DecimalMin`, `@DecimalMax`, `@Positive`
- `CreateRanchoRequest`: campos `nombre` (2-100 chars), `superficieTotal` (> 0), `descripcion` (optional, max 500)
- `CreateSeccionRequest`: incluir `ranchoId` (UUID), `nombre`, `superficie`
- `CreatePotreroRequest`: incluir `ranchoId`, `seccionId` (nullable), `nombre`, `superficie`
- No incluir `@Schema` (documentación vive en YAML)

---

### Task 4.3: Create REST Response DTOs

**Description:** Crear DTOs de salida como Java Records sin validaciones.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/dtos/RanchoResponse.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/dtos/RanchoStatsResponse.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/dtos/SeccionResponse.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/dtos/PotreroResponse.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/dtos/PaginationMeta.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/dtos/ErrorResponse.java`

**Dependencies:** Task 3.1

**Acceptance Criteria:**
- Todos deben ser Java Records inmutables
- `RanchoResponse`: incluir `ranchoId`, `nombre`, `superficieTotal`, `superficieDisponible`, `superficieUsada`, `porcentajeUso`, `status`, `tenantId`, `createdAt`, `updatedAt`
- `RanchoStatsResponse`: incluir `totalSecciones`, `totalPotreros`, distribución por sección
- Usar `String` para timestamps en formato ISO 8601 (conversión en mapper)
- `PaginationMeta`: incluir `page`, `size`, `total`
- `ErrorResponse`: incluir `timestamp`, `status`, `error`, `message`, `path`

---

### Task 4.4: Create DTO Mappers for REST Layer

**Description:** Crear mappers para transformar entre Commands/Results y DTOs REST.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/mappers/RanchoDtoMapper.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/mappers/SeccionDtoMapper.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/mappers/PotreroDto Mapper.java`

**Dependencies:** Tasks 3.1, 4.2, 4.3

**Acceptance Criteria:**
- Clases con anotación `@Component`
- Métodos `toCommand(Request)` y `toResponse(Result)`
- Formatear timestamps como ISO 8601 UTC en respuestas
- Formatear `BigDecimal` con 2 decimales en respuestas
- Inyectar `TenantContext` para extraer `tenantId` y `userId` al crear Commands

---

### Task 4.5: Implement Rancho REST Controller

**Description:** Implementar controlador REST para endpoints de ranchos.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/RanchoRestController.java`

**Dependencies:** Tasks 3.2, 4.1, 4.2, 4.3, 4.4

**Acceptance Criteria:**
- Clase con `@RestController`, `@RequestMapping("/api/v1/geography/ranchos")`, `@RequiredArgsConstructor`
- Implementar interfaz generada por `openapi-generator` a partir del YAML
- Inyectar todos los Use Cases de Rancho
- Validar Request DTOs con `@Valid`
- Retornar `ResponseEntity<T>` con códigos HTTP correctos: 200, 201, 204, 400, 404, 409
- Incluir manejo de excepciones con `@ExceptionHandler` o usar `@ControllerAdvice` global
- Usar `Pageable` para endpoints con paginación

---

### Task 4.6: Implement Seccion REST Controller

**Description:** Implementar controlador REST para endpoints de secciones.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/SeccionRestController.java`

**Dependencies:** Tasks 3.3, 4.1, 4.2, 4.3, 4.4

**Acceptance Criteria:**
- Anotaciones: `@RestController`, `@RequestMapping("/api/v1/geography/secciones")`, `@RequiredArgsConstructor`
- Implementar interfaz generada por OpenAPI
- Inyectar Use Cases de Seccion
- Validar Request DTOs con `@Valid`
- Retornar códigos HTTP semánticos

---

### Task 4.7: Implement Potrero REST Controller

**Description:** Implementar controlador REST para endpoints de potreros.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/PotreroRestController.java`

**Dependencies:** Tasks 3.4, 4.1, 4.2, 4.3, 4.4

**Acceptance Criteria:**
- Anotaciones: `@RestController`, `@RequestMapping("/api/v1/geography/potreros")`, `@RequiredArgsConstructor`
- Implementar interfaz generada por OpenAPI
- Inyectar Use Cases de Potrero
- Validar Request DTOs con `@Valid`
- Incluir endpoints para listar por rancho y por sección

---

### Task 4.8: Create Global Exception Handler

**Description:** Implementar manejador global de excepciones para controladores REST.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/GlobalExceptionHandler.java`

**Dependencies:** Task 1.6

**Acceptance Criteria:**
- Clase con anotaciones `@RestControllerAdvice` y `@RequiredArgsConstructor`
- Métodos `@ExceptionHandler` para: `EntityNotFoundException` (404), `DuplicateNameException` (409), `SurfaceExceededException` (400), `CannotArchiveWithChildrenException` (400), `CattleAssignedException` (400), `MethodArgumentNotValidException` (400)
- Retornar `ErrorResponse` con formato consistente
- Incluir logs con SLF4J para trazabilidad
- Manejar `ConstraintViolationException` de Bean Validation

---

## Phase 5: Web Controllers (Thymeleaf)

### Task 5.1: Create Web Form DTOs

**Description:** Crear DTOs para formularios web como Java Records.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/web/dtos/RanchoFormDto.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/web/dtos/SeccionFormDto.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/web/dtos/PotreroFormDto.java`

**Dependencies:** None

**Acceptance Criteria:**
- Todos deben ser Java Records
- Incluir anotaciones Bean Validation para validación del lado del servidor
- Campos coincidentes con Request DTOs de REST API
- Sin lógica de negocio (solo datos)

---

### Task 5.2: Implement Rancho Web Controller

**Description:** Crear controlador MVC para páginas web de ranchos con Thymeleaf.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/web/RanchoWebController.java`

**Dependencies:** Tasks 3.2, 5.1

**Acceptance Criteria:**
- Anotaciones: `@Controller`, `@RequestMapping("/geography/ranchos")`, `@RequiredArgsConstructor`
- Métodos que retornan nombres de vistas Thymeleaf (Strings)
- Endpoint `GET /geography/ranchos` → vista `geography/ranchos/lista`
- Endpoint `GET /geography/ranchos/{id}` → vista `geography/ranchos/detalle`
- Endpoint `GET /geography/ranchos/nuevo` → vista con formulario
- Endpoint `POST /geography/ranchos` → procesar formulario, redirigir con `redirect:/geography/ranchos`
- Usar `Model` para pasar datos a vistas
- Manejar errores de validación con `BindingResult`

---

### Task 5.3: Implement Seccion Web Controller

**Description:** Crear controlador MVC para páginas web de secciones.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/web/SeccionWebController.java`

**Dependencies:** Tasks 3.3, 5.1

**Acceptance Criteria:**
- Anotaciones: `@Controller`, `@RequestMapping("/geography/secciones")`, `@RequiredArgsConstructor`
- Vistas: `geography/secciones/lista`, `geography/secciones/detalle`, formulario de creación
- Procesar formularios con validación del lado del servidor

---

### Task 5.4: Implement Potrero Web Controller

**Description:** Crear controlador MVC para páginas web de potreros.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/controllers/web/PotreroWebController.java`

**Dependencies:** Tasks 3.4, 5.1

**Acceptance Criteria:**
- Anotaciones: `@Controller`, `@RequestMapping("/geography/potreros")`, `@RequiredArgsConstructor`
- Vistas: `geography/potreros/lista`, `geography/potreros/detalle`, formulario
- Soportar creación de potreros vinculados a rancho o sección

---

### Task 5.5: Create Thymeleaf Templates - Rancho

**Description:** Crear plantillas Thymeleaf para vistas de ranchos.

**Files to Create:**
- `src/main/resources/templates/geography/ranchos/lista.html`
- `src/main/resources/templates/geography/ranchos/detalle.html`
- `src/main/resources/templates/geography/ranchos/formulario.html`

**Dependencies:** Task 5.2

**Acceptance Criteria:**
- Usar `xmlns:th="http://www.thymeleaf.org"` en etiqueta `<html>`
- Incluir fragmentos reutilizables con `th:replace` o `th:insert`
- Vista de lista: cards con nombre, superficie, porcentaje de uso, botón "Ver Detalle"
- Vista de detalle: tabs para Información General, Secciones, Potreros, Estadísticas
- Formulario: campos nombre, superficie_total, descripción con validación HTML5
- Nombres de archivos y carpetas en español
- JavaScript vanilla integrado en `<script>` al final del HTML
- Consumir API REST con Fetch API para operaciones CRUD

---

### Task 5.6: Create Thymeleaf Templates - Seccion

**Description:** Crear plantillas Thymeleaf para vistas de secciones.

**Files to Create:**
- `src/main/resources/templates/geography/secciones/lista.html`
- `src/main/resources/templates/geography/secciones/detalle.html`
- `src/main/resources/templates/geography/secciones/formulario.html`

**Dependencies:** Task 5.3

**Acceptance Criteria:**
- Estructura similar a templates de Rancho
- Vista de lista: mostrar secciones agrupadas por rancho
- Formulario: incluir selector de rancho (dropdown)
- JavaScript vanilla para interactividad

---

### Task 5.7: Create Thymeleaf Templates - Potrero

**Description:** Crear plantillas Thymeleaf para vistas de potreros.

**Files to Create:**
- `src/main/resources/templates/geography/potreros/lista.html`
- `src/main/resources/templates/geography/potreros/detalle.html`
- `src/main/resources/templates/geography/potreros/formulario.html`

**Dependencies:** Task 5.4

**Acceptance Criteria:**
- Vista de lista: tabla con columnas nombre, superficie, rancho, sección, cantidad ganado
- Formulario: selectores para rancho y sección (condicional: si rancho tiene secciones)
- Mostrar warning si potrero tiene ganado asignado

---

### Task 5.8: Create Thymeleaf Fragments

**Description:** Crear fragmentos reutilizables para componentes geográficos.

**Files to Create:**
- `src/main/resources/templates/geography/fragments/geo-card.html`
- `src/main/resources/templates/geography/fragments/geo-tree.html`
- `src/main/resources/templates/geography/fragments/geo-stats.html`

**Dependencies:** Tasks 5.5, 5.6, 5.7

**Acceptance Criteria:**
- `geo-card.html`: componente card reutilizable para mostrar rancho/sección/potrero
- `geo-tree.html`: árbol jerárquico expandible (Rancho → Secciones → Potreros)
- `geo-stats.html`: panel de estadísticas con métricas visuales
- Usar parámetros Thymeleaf para personalización: `th:fragment="geo-card(entity, type)"`

---

### Task 5.9: Create CSS Styles for Geography Views

**Description:** Crear estilos CSS para vistas geográficas.

**Files to Create:**
- `src/main/resources/static/css/geography/ranchos.css`
- `src/main/resources/static/css/geography/secciones.css`
- `src/main/resources/static/css/geography/potreros.css`
- `src/main/resources/static/css/geography/geo-tree.css`
- `src/main/resources/static/css/geography/geo-stats.css`

**Dependencies:** Tasks 5.5, 5.6, 5.7, 5.8

**Acceptance Criteria:**
- CSS vanilla sin preprocesadores
- Usar variables CSS custom (`:root { --color-primary: ...; }`)
- Layout responsivo con CSS Grid para adaptar a tablets (min-width: 768px)
- Estilos para árbol jerárquico expandible/colapsable
- Estilos para panel de estadísticas con gráficos simples (barras con CSS)
- Nombres de archivos en español

---

## Phase 6: Configuration & Caching

### Task 6.1: Create Cache Configuration

**Description:** Configurar caching con Caffeine para estadísticas.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/cache/CacheConfig.java`
- `src/main/java/mx/vacapp/geography/internal/infrastructure/cache/CacheNames.java`

**Dependencies:** None

**Acceptance Criteria:**
- Clase `CacheConfig` con anotación `@Configuration` y `@EnableCaching`
- Configurar bean `CacheManager` usando Caffeine
- Cache `stats:rancho:{id}:tenant:{tenantId}` con TTL 5 minutos
- Clase `CacheNames` con constantes: `RANCHO_STATS = "rancho-stats"`
- Configurar `maximumSize` y `expireAfterWrite` para cada cache

---

### Task 6.2: Apply Caching to Use Cases

**Description:** Aplicar anotaciones de caching en casos de uso de estadísticas.

**Files to Modify:**
- `src/main/java/mx/vacapp/geography/internal/application/usecases/rancho/GetRanchoStatsUseCase.java`

**Dependencies:** Tasks 3.2, 6.1

**Acceptance Criteria:**
- Agregar `@Cacheable(value = CacheNames.RANCHO_STATS, key = "#ranchoId + ':' + #tenantId")` en método `execute()`
- Agregar `@CacheEvict` en `UpdateRanchoUseCase`, `CreateSeccionUseCase`, `UpdateSeccionUseCase`, `CreatePotreroUseCase`, `UpdatePotreroUseCase` para invalidar cache cuando cambia superficie

---

### Task 6.3: Create Module Configuration Class

**Description:** Crear clase de configuración principal del módulo.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/config/GeographyModuleConfig.java`

**Dependencies:** None

**Acceptance Criteria:**
- Clase con anotación `@Configuration`
- Definir beans necesarios si hay dependencias externas
- Configurar properties específicas del módulo
- Documentar beans públicos del módulo

---

### Task 6.4: Configure OpenAPI Documentation

**Description:** Configurar generación automática de documentación OpenAPI.

**Files to Create:**
- `src/main/java/mx/vacapp/geography/internal/infrastructure/config/OpenApiConfig.java`

**Dependencies:** Task 4.1

**Acceptance Criteria:**
- Clase con anotación `@Configuration`
- Configurar `@OpenAPIDefinition` con información del módulo
- Configurar security scheme para JWT Bearer token
- Documentar título: "Geographic Control API", versión: "1.0", descripción
- Agregar servidor: `/api/v1/geography`

---

### Task 6.5: Create Database Migration Scripts

**Description:** Crear scripts de migración de base de datos (Flyway o Liquibase).

**Files to Create:**
- `src/main/resources/db/migration/V1.3__create_geography_tables.sql`

**Dependencies:** Task 2.1

**Acceptance Criteria:**
- Script SQL para crear tablas: `ranchos`, `secciones`, `potreros`, `geography_audit`
- Definir columnas con tipos correctos: UUID para IDs, DECIMAL(15,2) para superficies, ENUM para status
- Crear índices en: `tenant_id`, `rancho_id`, `seccion_id`, `status`
- Crear índice compuesto: `(rancho_id, tenant_id)`, `(seccion_id, tenant_id)`
- Definir FKs: `secciones.rancho_id → ranchos.rancho_id`, `potreros.rancho_id → ranchos.rancho_id`, `potreros.seccion_id → secciones.seccion_id`
- Constraints: `CHECK (superficie_total > 0)`, `CHECK (status IN ('ACTIVE', 'ARCHIVED'))`

---

## Phase 7: Testing & Validation

### Task 7.1: Create Unit Tests - Domain Layer

**Description:** Crear tests unitarios para entidades y value objects de dominio.

**Files to Create:**
- `src/test/java/mx/vacapp/geography/internal/domain/model/RanchoTest.java`
- `src/test/java/mx/vacapp/geography/internal/domain/model/SeccionTest.java`
- `src/test/java/mx/vacapp/geography/internal/domain/model/PotreroTest.java`
- `src/test/java/mx/vacapp/geography/internal/domain/model/SurfaceCalculatorTest.java`

**Dependencies:** Tasks 1.2, 1.3, 1.4, 1.5

**Acceptance Criteria:**
- Usar JUnit 5 y AssertJ para assertions
- Probar factory methods `create()`
- Probar métodos de negocio: `archive()`, `updateSuperficie()`, `assignCattle()`
- Probar inmutabilidad (cambios generan nuevas instancias)
- Probar `SurfaceCalculator` con casos límite: superficie = 0, superficie negativa, suma excediendo total
- Cobertura mínima: 80% en capa de dominio

---

### Task 7.2: Create Unit Tests - Use Cases

**Description:** Crear tests unitarios para casos de uso con mocks.

**Files to Create:**
- `src/test/java/mx/vacapp/geography/internal/application/usecases/rancho/CreateRanchoUseCaseTest.java`
- `src/test/java/mx/vacapp/geography/internal/application/usecases/rancho/ArchiveRanchoUseCaseTest.java`
- `src/test/java/mx/vacapp/geography/internal/application/usecases/seccion/CreateSeccionUseCaseTest.java`
- `src/test/java/mx/vacapp/geography/internal/application/usecases/potrero/CreatePotreroUseCaseTest.java`
- `src/test/java/mx/vacapp/geography/internal/application/usecases/rancho/GetRanchoStatsUseCaseTest.java`

**Dependencies:** Tasks 3.2, 3.3, 3.4

**Acceptance Criteria:**
- Usar Mockito para mockear repositorios
- Probar escenarios exitosos y excepciones
- Probar validaciones: unicidad de nombre, superficie disponible, estado de hijos al archivar
- Verificar interacciones con repositorios: `verify(repository).save(...)`
- Usar `@ExtendWith(MockitoExtension.class)`

---

### Task 7.3: Create Integration Tests - Persistence Layer

**Description:** Crear tests de integración para repositorios con base de datos H2 en memoria.

**Files to Create:**
- `src/test/java/mx/vacapp/geography/internal/infrastructure/persistence/impl/RanchoRepositoryImplTest.java`
- `src/test/java/mx/vacapp/geography/internal/infrastructure/persistence/impl/SeccionRepositoryImplTest.java`
- `src/test/java/mx/vacapp/geography/internal/infrastructure/persistence/impl/PotreroRepositoryImplTest.java`

**Dependencies:** Tasks 2.4, 2.2, 2.1

**Acceptance Criteria:**
- Usar `@DataJpaTest` para configuración de tests de persistencia
- Usar H2 en memoria como base de datos de test
- Probar filtrado por `tenant_id`
- Probar queries personalizadas: `existsByNombreAndTenantIdIgnoreCase()`, `sumSuperficieByRanchoId()`
- Probar paginación
- Usar `@AutoConfigureTestDatabase(replace = Replace.ANY)`

---

### Task 7.4: Create Integration Tests - REST Controllers

**Description:** Crear tests de integración para controladores REST con MockMvc.

**Files to Create:**
- `src/test/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/RanchoRestControllerTest.java`
- `src/test/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/SeccionRestControllerTest.java`
- `src/test/java/mx/vacapp/geography/internal/infrastructure/controllers/mobile/PotreroRestControllerTest.java`

**Dependencies:** Tasks 4.5, 4.6, 4.7

**Acceptance Criteria:**
- Usar `@WebMvcTest` para tests de controladores
- Mockear Use Cases con `@MockBean`
- Usar `MockMvc` para simular requests HTTP
- Probar validación Bean Validation: enviar requests inválidos y verificar HTTP 400
- Probar códigos HTTP: 200, 201, 404, 409, 400
- Probar autenticación JWT con `@WithMockUser` o configuración de SecurityContext
- Verificar formato de respuestas JSON

---

### Task 7.5: Create End-to-End Tests

**Description:** Crear tests end-to-end que validan flujos completos.

**Files to Create:**
- `src/test/java/mx/vacapp/geography/e2e/RanchoLifecycleE2ETest.java`
- `src/test/java/mx/vacapp/geography/e2e/HierarchicalStructureE2ETest.java`
- `src/test/java/mx/vacapp/geography/e2e/SurfaceValidationE2ETest.java`

**Dependencies:** All previous tasks

**Acceptance Criteria:**
- Usar `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)`
- Usar `TestRestTemplate` para hacer requests HTTP reales
- Test `RanchoLifecycleE2ETest`: crear rancho → crear sección → crear potrero → archivar potrero → archivar sección → archivar rancho
- Test `HierarchicalStructureE2ETest`: validar jerarquía Rancho → Secciones → Potreros
- Test `SurfaceValidationE2ETest`: validar que suma de superficies no excede total, intentar crear potrero con superficie excedida y verificar HTTP 400
- Limpiar base de datos después de cada test con `@Transactional` o `@DirtiesContext`

---

### Task 7.6: Create Property-Based Tests

**Description:** Crear tests basados en propiedades con jqwik para validaciones matemáticas.

**Files to Create:**
- `src/test/java/mx/vacapp/geography/properties/SurfaceCalculatorPropertiesTest.java`

**Dependencies:** Task 1.5

**Acceptance Criteria:**
- Usar jqwik para generar datos aleatorios
- Property: "La suma de superficies de hijos siempre debe ser <= superficie del padre"
- Property: "Porcentaje de uso siempre debe estar entre 0 y 100"
- Property: "Superficie disponible = superficie total - suma de superficies usadas"
- Generar superficies aleatorias válidas (> 0, < 999,999,999)
- Usar `@Property` annotation

---

### Task 7.7: Validate Multi-Tenancy Isolation

**Description:** Crear tests específicos para validar aislamiento de datos por tenant.

**Files to Create:**
- `src/test/java/mx/vacapp/geography/security/MultiTenancyIsolationTest.java`

**Dependencies:** Tasks 2.4, 4.5, 4.6, 4.7

**Acceptance Criteria:**
- Crear ranchos para 2 tenants diferentes
- Verificar que Tenant A no puede acceder a recursos de Tenant B
- Verificar HTTP 403 al intentar acceder a rancho de otro tenant
- Verificar que listados filtran correctamente por tenant_id
- Usar `SecurityContextHolder` para simular diferentes usuarios con diferentes tenants

---

### Task 7.8: Performance Tests

**Description:** Crear tests de performance para queries y cálculos de estadísticas.

**Files to Create:**
- `src/test/java/mx/vacapp/geography/performance/StatsCalculationPerformanceTest.java`

**Dependencies:** Task 3.2

**Acceptance Criteria:**
- Crear dataset grande: 1 rancho con 50 secciones y 500 potreros
- Medir tiempo de cálculo de estadísticas
- Validar que cálculo toma < 500ms sin cache
- Validar que consulta desde cache toma < 50ms
- Usar `StopWatch` o similar para medir tiempos
- Marcar como `@Tag("performance")` para ejecutar separadamente

---

### Task 7.9: Create Test Data Builders

**Description:** Crear builders de objetos de test para facilitar creación de datos.

**Files to Create:**
- `src/test/java/mx/vacapp/geography/testdata/RanchoTestDataBuilder.java`
- `src/test/java/mx/vacapp/geography/testdata/SeccionTestDataBuilder.java`
- `src/test/java/mx/vacapp/geography/testdata/PotreroTestDataBuilder.java`

**Dependencies:** Tasks 1.2, 1.3, 1.4

**Acceptance Criteria:**
- Implementar patrón Builder para objetos de test
- Proveer valores por defecto válidos
- Permitir personalización con métodos fluent: `.withNombre(...)`, `.withSuperficie(...)`
- Facilitar creación de objetos relacionados: `rancho.withSeccion(...).withPotrero(...)`

---

## Phase 8: Documentation & Finalization

### Task 8.1: Update Module README

**Description:** Actualizar README del módulo con documentación completa.

**Files to Modify:**
- `src/main/java/mx/vacapp/geography/README.md`

**Dependencies:** All implementation tasks

**Acceptance Criteria:**
- Documentar API pública `GeographyService`
- Documentar endpoints REST principales
- Incluir ejemplos de uso del módulo desde otros módulos
- Documentar estructura jerárquica y reglas de validación
- Incluir diagramas: jerarquía de entidades, flujo de creación
- Documentar configuración de cache

---

### Task 8.2: Generate API Documentation

**Description:** Generar documentación OpenAPI y verificar Swagger UI.

**Files to Verify:**
- Swagger UI disponible en `/swagger-ui/index.html`
- OpenAPI JSON en `/v3/api-docs`

**Dependencies:** Tasks 4.1, 6.4

**Acceptance Criteria:**
- Ejecutar `mvn compile` para generar interfaces desde YAML
- Verificar que Swagger UI carga correctamente
- Verificar que todos los endpoints están documentados
- Probar endpoints desde Swagger UI con autenticación JWT
- Verificar schemas de Request/Response

---

### Task 8.3: Code Quality & Static Analysis

**Description:** Ejecutar análisis estático de código y corregir issues.

**Files to Verify:**
- Todos los archivos Java del módulo

**Dependencies:** All implementation tasks

**Acceptance Criteria:**
- Ejecutar Checkstyle y corregir violaciones
- Ejecutar SpotBugs y resolver bugs detectados
- Verificar cobertura de tests >= 80% con JaCoCo
- Ejecutar SonarQube (si está configurado) y resolver code smells
- Verificar que no hay warnings del compilador

---

### Task 8.4: Security Review

**Description:** Revisar implementación de seguridad y multi-tenancy.

**Files to Review:**
- Todos los repositorios JPA
- Todos los controladores REST
- Casos de uso que modifican datos

**Dependencies:** All implementation tasks

**Acceptance Criteria:**
- Verificar que todos los repositorios filtran por `tenant_id`
- Verificar que no hay SQL injection vulnerabilities
- Verificar que validación de input está correctamente implementada
- Verificar que excepciones no exponen información sensible
- Verificar que logs no registran datos sensibles
- Verificar que JWT es validado en todos los endpoints protegidos

---

### Task 8.5: Create Deployment Checklist

**Description:** Crear checklist de despliegue y configuración en producción.

**Files to Create:**
- `docs/deployment/geographic-control-deployment.md`

**Dependencies:** All tasks

**Acceptance Criteria:**
- Documentar variables de entorno necesarias
- Documentar scripts de migración de base de datos
- Documentar configuración de cache (Caffeine)
- Documentar índices de base de datos requeridos
- Documentar permisos y roles necesarios
- Documentar smoke tests post-deployment
- Incluir rollback plan

---

### Task 8.6: Integration with Other Modules

**Description:** Implementar integración con módulos relacionados (si existen).

**Files to Create:**
- `src/main/java/mx/vacapp/geography/GeographyServiceImpl.java` (implementación de API pública)

**Dependencies:** Tasks 1.1, 3.4

**Acceptance Criteria:**
- Implementar métodos de `GeographyService` interface
- `isPotreroActive()`: consultar estado del potrero
- `getRanchoTenantId()`: obtener tenant_id de un rancho
- `hasCapacity()`: validar capacidad del potrero (placeholder para futura lógica)
- `getPotreroSurface()`: obtener superficie del potrero
- Anotar con `@Service` y `@RequiredArgsConstructor`
- Inyectar repositorios necesarios

---

### Task 8.7: Final Manual Testing

**Description:** Ejecutar pruebas manuales end-to-end en entorno de desarrollo.

**Test Scenarios:**
1. Crear rancho → crear secciones → crear potreros → validar estadísticas
2. Intentar crear sección con superficie que excede rancho → validar error 400
3. Intentar archivar sección con potreros activos → validar error 400
4. Crear potrero directamente en rancho (sin secciones) → debe funcionar
5. Crear sección en rancho con potreros directos → debe prohibir futuros potreros directos
6. Listar ranchos con paginación → validar metadatos de paginación
7. Buscar rancho por ID de otro tenant → validar error 403
8. Probar Swagger UI → ejecutar todos los endpoints con JWT

**Dependencies:** All tasks

**Acceptance Criteria:**
- Todos los escenarios pasan exitosamente
- No hay errores 500 en logs
- Validaciones funcionan correctamente
- Mensajes de error son descriptivos
- UI web renderiza correctamente

---

### Task 8.8: Create User Acceptance Test (UAT) Plan

**Description:** Crear plan de pruebas de aceptación de usuario.

**Files to Create:**
- `docs/testing/geographic-control-uat-plan.md`

**Dependencies:** All tasks

**Acceptance Criteria:**
- Definir casos de uso de negocio a probar
- Definir criterios de aceptación para cada caso de uso
- Incluir datos de prueba realistas
- Definir roles de usuarios para testing
- Incluir checklist de features implementadas vs requirements
- Definir métricas de éxito (tiempo de respuesta, usabilidad)

---

## Summary

### Total Tasks: 54

**Phase 1 - Module Structure & Domain:** 7 tasks  
**Phase 2 - Persistence Layer:** 4 tasks  
**Phase 3 - Application Layer:** 4 tasks  
**Phase 4 - REST API:** 8 tasks  
**Phase 5 - Web Controllers:** 9 tasks  
**Phase 6 - Configuration:** 5 tasks  
**Phase 7 - Testing:** 9 tasks  
**Phase 8 - Documentation:** 8 tasks  

### Critical Path

1. Task 1.1 → 1.2, 1.3, 1.4 → 1.7 (Domain foundation)
2. Task 2.1 → 2.2 → 2.3 → 2.4 (Persistence layer)
3. Task 3.1 → 3.2, 3.3, 3.4 (Use cases)
4. Task 4.1 → 4.2, 4.3 → 4.4 → 4.5, 4.6, 4.7 (REST API)
5. Task 5.1 → 5.2, 5.3, 5.4 → 5.5, 5.6, 5.7 (Web UI)
6. Testing tasks can run in parallel after implementation is complete

### Estimated Timeline

- **Phase 1-2 (Domain + Persistence):** 3-4 days
- **Phase 3 (Application Layer):** 2-3 days
- **Phase 4 (REST API):** 3-4 days
- **Phase 5 (Web UI):** 4-5 days
- **Phase 6 (Configuration):** 1-2 days
- **Phase 7 (Testing):** 4-5 days
- **Phase 8 (Documentation):** 2-3 days

**Total Estimated Time:** 19-26 working days (approximately 4-5 weeks)

---

## Notes

- Todas las tareas de implementación deben seguir las convenciones de AGENTS.md
- Los nombres de clases, métodos y variables deben estar en inglés (backend)
- Los nombres de archivos HTML, CSS y carpetas frontend deben estar en español
- Usar Lombok para reducir boilerplate code
- Aplicar DTOs como Records en toda la aplicación
- Mantener separación estricta entre capas (Clean Architecture)
- Implementar multi-tenancy en todos los repositorios
- La validación de superficie es crítica y debe ser exhaustiva
- El caching de estadísticas es importante para performance
- Los tests deben cubrir casos límite y escenarios de error

