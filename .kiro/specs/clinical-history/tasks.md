# Tasks Document: Clinical History Module

## Overview

Este documento desglosa la implementación del módulo **clinical-history** en tareas específicas organizadas por fase de desarrollo. El módulo implementa registro y gestión de intervenciones veterinarias (vacunaciones con descuento automático de stock + tratamientos médicos generales) siguiendo Spring Modulith y Clean Architecture.

---

## Phase 1: Module Structure & Domain Layer

### Task 1.1: Create Module Base Structure

**Description:** Crear la estructura base del módulo clinical-history siguiendo Spring Modulith.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/ClinicalHistoryService.java` (API pública)
- `src/main/java/mx/vacapp/clinicalhistory/internal/` (directorio interno)
- `src/main/java/mx/vacapp/clinicalhistory/README.md` (documentación del módulo)

**Dependencies:** None

**Acceptance Criteria:**
- La API pública `ClinicalHistoryService.java` debe estar en la raíz del paquete `mx.vacapp.clinicalhistory`
- Todo el código interno debe estar bajo `internal/` package
- El README.md debe documentar la responsabilidad del módulo y su API pública
- Documentar integración con cattle-inventory, vaccination-management, y user-management

---

### Task 1.2: Create Domain Model - ClinicalRecord Base Entity

**Description:** Implementar la entidad base de dominio `ClinicalRecord` como POJO puro abstracto sin anotaciones JPA.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/model/ClinicalRecord.java`

**Dependencies:** Task 1.1

**Acceptance Criteria:**
- La clase debe ser abstracta e inmutable con patrón Builder genérico
- Debe incluir campos comunes: recordId, animalId, animalNoIdentificador, animalNombre, interventionType, dosisAplicada, viaAdministracion, fechaAplicacion, notas, aplicadoPor, veterinarioNombre, veterinarioCedula, tenantId, createdAt
- Debe incluir validación: `validateFechaAplicacion()` que rechace fechas futuras
- Debe incluir método: `isOlderThanOneYear()` para detectar registros retroactivos
- Sin anotaciones de infraestructura (JPA, Spring)
- Usar `BigDecimal` para dosisAplicada, `LocalDate` para fechaAplicacion, `Instant` para createdAt

---

### Task 1.3: Create Domain Model - VaccinationRecord Entity

**Description:** Implementar la entidad de dominio `VaccinationRecord` que extiende `ClinicalRecord`.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/model/VaccinationRecord.java`

**Dependencies:** Task 1.2

**Acceptance Criteria:**
- Clase que extiende `ClinicalRecord` con campos específicos: vacunaId, nombreVacuna, laboratorio, lote, fechaVencimientoLote, proximaDosis
- Factory method `create()` que calcule automáticamente proximaDosis usando NextDoseCalculator
- Métodos de negocio: `isOverdue()`, `getDaysUntilNextDose()`, `hasNextDose()`
- Builder específico que extienda Builder de ClinicalRecord
- Validación que proximaDosis no sea más de 10 años en el futuro (marcarlo como null si lo es)

---

### Task 1.4: Create Domain Model - TreatmentRecord Entity

**Description:** Implementar la entidad de dominio `TreatmentRecord` que extiende `ClinicalRecord`.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/model/TreatmentRecord.java`

**Dependencies:** Task 1.2

**Acceptance Criteria:**
- Clase que extiende `ClinicalRecord` con campos: tipoTratamiento, medicamentoNombre, diagnostico, duracionTratamientoDias, fechaFinTratamiento, costoTratamiento
- Factory method `create()` que calcule fechaFinTratamiento = fechaAplicacion + duracionTratamientoDias
- Métodos: `isTreatmentFinished()`, `getDaysRemaining()`
- Validación: `validateDiagnostico()` que requiera notas detalladas si tipoTratamiento = OTRO
- Campo costoTratamiento como `BigDecimal` con default 0.00

---

### Task 1.5: Create Domain Enums and Value Objects

**Description:** Crear enums y value object para cálculos de próximas dosis.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/model/InterventionType.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/model/TreatmentType.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/model/ViaAdministracion.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/model/NextDoseCalculator.java`

**Dependencies:** Task 1.1

**Acceptance Criteria:**
- `InterventionType`: valores VACCINATION, TREATMENT
- `TreatmentType`: valores DESPARASITACION, ANTIBIOTICO, VITAMINAS, CIRUGIA, CURACION, OTRO
- `ViaAdministracion`: valores INTRAMUSCULAR, SUBCUTANEA, INTRAVENOSA, ORAL, TOPICA
- `NextDoseCalculator`: métodos estáticos `calculate(fechaAplicacion, intervaloRefuerzoDias)`, `calculateDaysRemaining(proximaDosis)`, `isOverdue(proximaDosis)`
- Lógica en calculate: si intervalo null o <= 0, retornar null (dosis única)
- Lógica en calculate: si resultado > 10 años en futuro, retornar null (inválido)

---

### Task 1.6: Create Domain Exceptions

**Description:** Implementar excepciones de dominio para validaciones de negocio.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/model/exceptions/AnimalNotFoundException.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/model/exceptions/AnimalInactiveException.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/model/exceptions/VaccineNotFoundException.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/model/exceptions/InsufficientStockException.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/model/exceptions/ExpiredLotException.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/model/exceptions/UnauthorizedRoleException.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/model/exceptions/FutureDateException.java`

**Dependencies:** Task 1.1

**Acceptance Criteria:**
- Todas las excepciones deben extender `RuntimeException`
- Cada excepción debe tener constructor con mensaje descriptivo
- Incluir constructor con mensaje y causa (Throwable)
- `InsufficientStockException` debe incluir campo stockDisponible (Integer)

---

### Task 1.7: Create Domain Repository Ports

**Description:** Definir interfaces de repositorio (puertos de salida) en capa de dominio.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/repository/ClinicalRecordRepository.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/repository/VaccinationRecordRepository.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/repository/TreatmentRecordRepository.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/repository/ClinicalAuditRepository.java`

**Dependencies:** Tasks 1.2, 1.3, 1.4

**Acceptance Criteria:**
- Interfaces puras sin anotaciones de Spring
- Métodos con nombres descriptivos: `save()`, `findById()`, `findByAnimalId()`, `countByAnimalId()`
- Usar tipos de dominio (`VaccinationRecord`, `TreatmentRecord`), no entidades JPA
- Retornar `Optional<T>` para búsquedas que pueden no existir
- Incluir métodos para reportes: `findByAnimalIdAndDateRange()`, `findUpcomingDoses()`
- ClinicalAuditRepository: métodos `logVaccinationCreation()`, `logTreatmentCreation()`

---

### Task 1.8: Create Integration Ports

**Description:** Definir interfaces para integración con otros módulos (puertos de salida).

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/repository/CattleServicePort.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/repository/VaccinationServicePort.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/domain/repository/UserServicePort.java`

**Dependencies:** Task 1.1

**Acceptance Criteria:**
- `CattleServicePort`: métodos `getAnimalById(UUID)`, `validateAnimalActive(UUID)`, definir record `AnimalInfo`
- `VaccinationServicePort`: métodos `getVaccinaById(UUID)`, `validateLoteStock(String, BigDecimal)`, `decrementStock(String, BigDecimal, UUID)`, definir record `VaccineInfo`
- `UserServicePort`: métodos `getVeterinarianById(UUID)`, `hasRole(UUID, String)`, definir record `VeterinarianInfo`
- Records deben incluir campos necesarios para desnormalización

---

## Phase 2: Persistence Layer (Infrastructure)

### Task 2.1: Create JPA Entities

**Description:** Implementar entidades JPA con anotaciones de persistencia usando herencia de tablas.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/entities/ClinicalRecordEntity.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/entities/VaccinationRecordEntity.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/entities/TreatmentRecordEntity.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/entities/ClinicalAuditEntity.java`

**Dependencies:** Tasks 1.2, 1.3, 1.4, 1.5

**Acceptance Criteria:**
- `ClinicalRecordEntity`: usar `@Entity`, `@Table("clinical_records")`, sin herencia JPA (tabla independiente)
- `VaccinationRecordEntity`: usar `@Entity`, `@Table("vaccination_records")`, FK a clinical_records con `@ManyToOne`
- `TreatmentRecordEntity`: usar `@Entity`, `@Table("treatment_records")`, FK a clinical_records con `@ManyToOne`
- IDs como UUID con `@GeneratedValue(strategy = GenerationType.UUID)`
- Incluir columna `tenant_id` (UUID) con índice en todas las entidades
- Usar `@Enumerated(EnumType.STRING)` para enums
- Incluir timestamps: `created_at` con `@Column(name = "created_at")`
- `ClinicalAuditEntity` debe incluir: audit_id, entity_type, entity_id, operation_type (solo CREATE), timestamp, modified_by, tenant_id, new_values (JSON), stock_antes, stock_despues, lote_id

---

### Task 2.2: Create JPA Repositories

**Description:** Crear interfaces Spring Data JPA Repository.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/repositories/ClinicalRecordJpaRepository.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/repositories/VaccinationRecordJpaRepository.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/repositories/TreatmentRecordJpaRepository.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/repositories/ClinicalAuditJpaRepository.java`

**Dependencies:** Task 2.1

**Acceptance Criteria:**
- Extender `JpaRepository<EntityType, UUID>`
- Incluir métodos de consulta derivados: `findByAnimalIdAndTenantId()`, `findByTenantId()`, `countByAnimalIdAndTenantId()`
- VaccinationRecordJpaRepository: métodos `findByProximaDosisBeforeAndTenantId()`, `findByProximaDosisBetweenAndTenantId()`
- Incluir queries con `@Query` para reportes: historial por animal con filtros de fecha
- Usar `Page<T>` para métodos con paginación
- Aplicar filtro `tenant_id` en todas las queries personalizadas

---

### Task 2.3: Create Entity Mappers

**Description:** Implementar clases Mapper para transformar entre entidades JPA y objetos de dominio.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/mappers/ClinicalRecordMapper.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/mappers/VaccinationRecordMapper.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/mappers/TreatmentRecordMapper.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/mappers/AuditMapper.java`

**Dependencies:** Tasks 2.1, 1.2, 1.3, 1.4

**Acceptance Criteria:**
- Clases con anotación `@Component`
- Métodos `toEntity(DomainModel domain)` y `toDomain(EntityJPA entity)`
- Usar Lombok `@RequiredArgsConstructor` si hay dependencias
- Manejar conversión de tipos: `Instant` ↔ timestamps, `BigDecimal` ↔ numeric columns
- Mappers de VaccinationRecord y TreatmentRecord deben componer con ClinicalRecordMapper

---

### Task 2.4: Implement Repository Adapters

**Description:** Implementar adaptadores que conectan puertos de dominio con repositorios JPA.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/impl/ClinicalRecordRepositoryImpl.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/impl/VaccinationRecordRepositoryImpl.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/impl/TreatmentRecordRepositoryImpl.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/impl/ClinicalAuditRepositoryImpl.java`

**Dependencies:** Tasks 1.7, 2.2, 2.3

**Acceptance Criteria:**
- Implementar interfaces de `domain/repository/`
- Usar anotación `@Repository` y `@RequiredArgsConstructor` (Lombok)
- Inyectar `JpaRepository` correspondiente y `Mapper`
- Aplicar filtro `tenant_id` extrayéndolo del contexto de seguridad en todos los métodos
- Lanzar excepciones de dominio cuando no se encuentren recursos
- Incluir logs con SLF4J para trazabilidad
- ClinicalAuditRepositoryImpl: serializar new_values como JSON usando Jackson

---

### Task 2.5: Implement Integration Adapters

**Description:** Implementar adaptadores para integración con otros módulos de Vacapp.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/integration/CattleServiceAdapter.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/integration/VaccinationServiceAdapter.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/integration/UserServiceAdapter.java`

**Dependencies:** Task 1.8

**Acceptance Criteria:**
- Implementar interfaces `CattleServicePort`, `VaccinationServicePort`, `UserServicePort`
- Usar anotación `@Component` y `@RequiredArgsConstructor`
- Inyectar servicios públicos de otros módulos: `CattleInventoryService`, `VaccinationService`, `UserManagementService`
- CattleServiceAdapter: mapear entre domain records y DTOs de cattle-inventory
- VaccinationServiceAdapter: implementar lógica de descuento de stock transaccional con `@Transactional`
- UserServiceAdapter: validar rol VETERINARIO
- Manejar excepciones de integración y lanzar excepciones de dominio apropiadas
- Incluir fallback resiliente: si vaccination-management no disponible, lanzar ServiceUnavailableException

---

## Phase 3: Application Layer (Use Cases)

### Task 3.1: Create Command and Result Records

**Description:** Definir Commands y Results como Java Records para casos de uso.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/commands/RegisterVaccinationCommand.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/commands/VaccinationResult.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/commands/RegisterTreatmentCommand.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/commands/TreatmentResult.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/commands/AnimalReportResult.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/commands/PeriodReportResult.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/commands/UpcomingDoseResult.java`

**Dependencies:** Task 1.1

**Acceptance Criteria:**
- Todos deben ser Java Records (inmutables)
- `RegisterVaccinationCommand`: animalId, vacunaId, lote, dosisAplicada, viaAdministracion, fechaAplicacion, notas, veterinarioId, tenantId
- `RegisterTreatmentCommand`: animalId, tipoTratamiento, medicamentoNombre, dosisAplicada, viaAdministracion, diagnostico, duracionTratamientoDias, costoTratamiento, fechaAplicacion, notas, veterinarioId, tenantId
- `VaccinationResult`: incluir proximaDosis calculada
- `AnimalReportResult`: incluir totalVacunaciones, totalTratamientos, proximasDosis[], historial[]
- `PeriodReportResult`: incluir distribucionPorTipo, costoTotal, animalesTratados

---

### Task 3.2: Implement Vaccination Use Cases

**Description:** Crear casos de uso para gestión de vacunaciones.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/vaccination/RegisterVaccinationUseCase.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/vaccination/GetVaccinationUseCase.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/vaccination/ListVaccinationsByAnimalUseCase.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/vaccination/GetUpcomingDosesUseCase.java`

**Dependencies:** Tasks 1.3, 1.7, 1.8, 3.1

**Acceptance Criteria:**
- Clases con anotación `@Service` y `@RequiredArgsConstructor`
- `RegisterVaccinationUseCase`: usar `@Transactional` con propagación REQUIRED, validar animal activo, validar vacuna, validar stock, crear VaccinationRecord, persistir, decrementar stock, registrar auditoría, retornar VaccinationResult
- Flujo transaccional: validaciones → creación → persistencia → descuento stock → auditoría (todo atómico)
- Si descuento de stock falla, rollback completo
- `GetUpcomingDosesUseCase`: filtrar por proxima_dosis BETWEEN fechaActual AND (fechaActual + daysAhead)
- `ListVaccinationsByAnimalUseCase`: soportar paginación con `Pageable`

---

### Task 3.3: Implement Treatment Use Cases

**Description:** Crear casos de uso para gestión de tratamientos médicos.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/treatment/RegisterTreatmentUseCase.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/treatment/GetTreatmentUseCase.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/treatment/ListTreatmentsByAnimalUseCase.java`

**Dependencies:** Tasks 1.4, 1.7, 1.8, 3.1

**Acceptance Criteria:**
- Anotaciones `@Service`, `@RequiredArgsConstructor`, `@Transactional` en RegisterTreatmentUseCase
- `RegisterTreatmentUseCase`: validar animal activo, validar veterinario, crear TreatmentRecord con fechaFinTratamiento calculada, persistir, registrar auditoría
- No requiere integración con vaccination-management (no descuento de stock)
- `ListTreatmentsByAnimalUseCase`: ordenar por fechaAplicacion descendente

---

### Task 3.4: Implement Report Use Cases

**Description:** Crear casos de uso para generación de reportes de salud.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/report/GenerateAnimalReportUseCase.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/report/GeneratePeriodReportUseCase.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/report/GetOverdueDosesUseCase.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/application/usecases/report/GetUpcomingDosesAlertsUseCase.java`

**Dependencies:** Tasks 1.7, 3.1

**Acceptance Criteria:**
- Anotaciones `@Service`, `@RequiredArgsConstructor`
- `GenerateAnimalReportUseCase`: aplicar `@Cacheable` con TTL 10 minutos, obtener info animal, vacunaciones, tratamientos, calcular métricas, construir AnimalReportResult
- `GeneratePeriodReportUseCase`: agregaciones con GROUP BY, calcular costo total, contar animales únicos
- `GetOverdueDosesUseCase`: filtrar proxima_dosis < fecha_actual, ordenar por dias_vencidos desc
- `GetUpcomingDosesAlertsUseCase`: filtrar proxima_dosis BETWEEN fecha_actual AND (fecha_actual + days_ahead), ordenar por dias_restantes asc

---

## Phase 4: REST API Controllers & DTOs

### Task 4.1: Define OpenAPI Specification (YAML)

**Description:** Crear especificación OpenAPI 3.0 con todos los endpoints REST del módulo.

**Files to Create:**
- `src/main/resources/openapi/openapi-clinical-history.yaml`

**Dependencies:** None

**Acceptance Criteria:**
- Definir ruta base: `/api/v1/clinical-history`
- Endpoints de Vacunaciones: `POST /vaccinations`, `GET /vaccinations`, `GET /vaccinations/{id}`, `GET /animals/{animalId}/vaccinations`
- Endpoints de Tratamientos: `POST /treatments`, `GET /treatments`, `GET /treatments/{id}`, `GET /animals/{animalId}/treatments`
- Endpoints de Historial: `GET /animals/{animalId}/history` (vacunaciones + tratamientos combinados)
- Endpoints de Reportes: `GET /reports/by-animal`, `GET /reports/by-period`
- Endpoints de Alertas: `GET /alerts/overdue-doses`, `GET /alerts/upcoming-doses`
- Definir schemas para Request/Response DTOs en `components/schemas`
- Incluir validaciones: dosis_aplicada > 0, fecha_aplicacion <= hoy, duracion_tratamiento_dias 1-365
- Definir responses: 200, 201, 400, 401, 403, 404, 503
- Incluir security requirement: `bearerAuth` (JWT) con scope VETERINARIO para POST

---

### Task 4.2: Create REST Request DTOs

**Description:** Crear DTOs de entrada como Java Records con Bean Validation.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/dtos/RegisterVaccinationRequest.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/dtos/RegisterTreatmentRequest.java`

**Dependencies:** Task 4.1

**Acceptance Criteria:**
- Todos deben ser Java Records
- `RegisterVaccinationRequest`: incluir `@NotNull` en animalId, vacunaId, lote, dosisAplicada; `@NotBlank` en lote; `@Positive` en dosisAplicada; `@PastOrPresent` en fechaAplicacion; `@NotNull` en viaAdministracion
- `RegisterTreatmentRequest`: incluir `@NotNull` en animalId, tipoTratamiento; `@Size(min=2, max=200)` en medicamentoNombre; `@Size(min=5, max=1000)` en diagnostico; `@Min(1) @Max(365)` en duracionTratamientoDias; `@PositiveOrZero` en costoTratamiento
- No incluir `@Schema` (documentación vive en YAML)

---

### Task 4.3: Create REST Response DTOs

**Description:** Crear DTOs de salida como Java Records sin validaciones.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/dtos/VaccinationResponse.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/dtos/TreatmentResponse.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/dtos/AnimalReportResponse.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/dtos/PeriodReportResponse.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/dtos/UpcomingDoseResponse.java`

**Dependencies:** Task 3.1

**Acceptance Criteria:**
- Todos deben ser Java Records inmutables
- `VaccinationResponse`: incluir recordId, animalId, animalIdentificador, vacunaNombre, lote, dosisAplicada, viaAdministracion, fechaAplicacion, proximaDosis, aplicadoPor, veterinarioNombre, createdAt
- `TreatmentResponse`: incluir recordId, animalId, tipoTratamiento, medicamentoNombre, diagnostico, fechaAplicacion, fechaFinTratamiento, duracionTratamientoDias, costoTratamiento, aplicadoPor
- Usar `String` para timestamps en formato ISO 8601
- `AnimalReportResponse`: incluir animalInfo, totalVacunaciones, totalTratamientos, ultimaVacunacion, proximasDosis[]
- `UpcomingDoseResponse`: incluir animalId, animalIdentificador, vacunaNombre, fechaAplicacionAnterior, proximaDosis, diasRestantes

---

### Task 4.4: Create DTO Mappers for REST Layer

**Description:** Crear mappers para transformar entre Commands/Results y DTOs REST.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/mappers/VaccinationDtoMapper.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/mappers/TreatmentDtoMapper.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/mappers/ReportDtoMapper.java`

**Dependencies:** Tasks 3.1, 4.2, 4.3

**Acceptance Criteria:**
- Clases con anotación `@Component`
- Métodos `toCommand(Request)` y `toResponse(Result)`
- Formatear timestamps como ISO 8601 UTC en respuestas
- Formatear `BigDecimal` con 2 decimales en respuestas
- Inyectar `SecurityContext` para extraer `tenantId` y `veterinarioId` al crear Commands

---

### Task 4.5: Implement Vaccination REST Controller

**Description:** Implementar controlador REST para endpoints de vacunaciones.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/VaccinationRestController.java`

**Dependencies:** Tasks 3.2, 4.1, 4.2, 4.3, 4.4

**Acceptance Criteria:**
- Clase con `@RestController`, `@RequestMapping("/api/v1/clinical-history/vaccinations")`, `@RequiredArgsConstructor`
- Implementar interfaz generada por `openapi-generator` a partir del YAML
- Inyectar Use Cases de Vacunación
- Validar Request DTOs con `@Valid`
- Verificar rol VETERINARIO antes de permitir POST: usar `@PreAuthorize("hasRole('VETERINARIO')")`
- Retornar `ResponseEntity<T>` con códigos HTTP: 200, 201, 400, 403, 404, 503
- Usar `Pageable` para endpoints con paginación

---

### Task 4.6: Implement Treatment REST Controller

**Description:** Implementar controlador REST para endpoints de tratamientos médicos.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/TreatmentRestController.java`

**Dependencies:** Tasks 3.3, 4.1, 4.2, 4.3, 4.4

**Acceptance Criteria:**
- Anotaciones: `@RestController`, `@RequestMapping("/api/v1/clinical-history/treatments")`, `@RequiredArgsConstructor`
- Implementar interfaz generada por OpenAPI
- Inyectar Use Cases de Tratamientos
- Validar Request DTOs con `@Valid`
- Verificar rol VETERINARIO para POST con `@PreAuthorize`
- Retornar códigos HTTP semánticos

---

### Task 4.7: Implement Clinical Report REST Controller

**Description:** Implementar controlador REST para endpoints de reportes.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/ClinicalReportRestController.java`

**Dependencies:** Tasks 3.4, 4.1, 4.3, 4.4

**Acceptance Criteria:**
- Anotaciones: `@RestController`, `@RequestMapping("/api/v1/clinical-history/reports")`, `@RequiredArgsConstructor`
- Endpoints: `/by-animal`, `/by-period`
- Inyectar Use Cases de Reportes
- Validar parámetros con `@Valid`
- Permitir lectura a roles: ADMIN, MANAGER, VETERINARIO, WORKER

---

### Task 4.8: Implement Clinical Alert REST Controller

**Description:** Implementar controlador REST para endpoints de alertas.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/ClinicalAlertRestController.java`

**Dependencies:** Tasks 3.4, 4.1, 4.3, 4.4

**Acceptance Criteria:**
- Anotaciones: `@RestController`, `@RequestMapping("/api/v1/clinical-history/alerts")`, `@RequiredArgsConstructor`
- Endpoints: `/overdue-doses`, `/upcoming-doses`
- Inyectar GetOverdueDosesUseCase y GetUpcomingDosesAlertsUseCase
- Aplicar caching de 1 hora para alertas
- Permitir lectura a roles business

---

### Task 4.9: Create Global Exception Handler

**Description:** Implementar manejador global de excepciones para controladores REST.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/GlobalExceptionHandler.java`

**Dependencies:** Task 1.6

**Acceptance Criteria:**
- Clase con `@RestControllerAdvice` y `@RequiredArgsConstructor`
- Métodos `@ExceptionHandler` para: `AnimalNotFoundException` (404), `AnimalInactiveException` (400), `VaccineNotFoundException` (404), `InsufficientStockException` (400), `ExpiredLotException` (400), `UnauthorizedRoleException` (403), `FutureDateException` (400)
- Manejar `MethodArgumentNotValidException` (400) con formato de errores
- Incluir logs con SLF4J para trazabilidad
- Retornar formato consistente con ErrorResponse

---

## Phase 5: Web Controllers (Thymeleaf)

### Task 5.1: Create Web Form DTOs

**Description:** Crear DTOs para formularios web como Java Records.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/web/dtos/VaccinationFormDto.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/web/dtos/TreatmentFormDto.java`

**Dependencies:** None

**Acceptance Criteria:**
- Todos deben ser Java Records
- Incluir anotaciones Bean Validation para validación del lado del servidor
- Campos coincidentes con Request DTOs de REST API

---

### Task 5.2: Implement Clinical History Web Controller

**Description:** Crear controlador MVC para páginas web de historial clínico con Thymeleaf.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/web/ClinicalHistoryWebController.java`

**Dependencies:** Tasks 3.2, 3.3, 3.4, 5.1

**Acceptance Criteria:**
- Anotaciones: `@Controller`, `@RequestMapping("/clinical-history")`, `@RequiredArgsConstructor`
- Endpoint `GET /clinical-history/animals/{animalId}` → vista `clinical-history/animal-history`
- Endpoint `GET /clinical-history/vaccinations/new` → vista con formulario de vacunación
- Endpoint `POST /clinical-history/vaccinations` → procesar formulario, redirigir
- Endpoint `GET /clinical-history/treatments/new` → vista con formulario de tratamiento
- Endpoint `POST /clinical-history/treatments` → procesar formulario
- Usar `Model` para pasar datos a vistas
- Manejar errores de validación con `BindingResult`

---

### Task 5.3: Create Thymeleaf Templates - Animal History

**Description:** Crear plantillas Thymeleaf para visualización de historial clínico.

**Files to Create:**
- `src/main/resources/templates/clinical-history/animal-history.html`
- `src/main/resources/templates/clinical-history/vaccination-form.html`
- `src/main/resources/templates/clinical-history/treatment-form.html`

**Dependencies:** Task 5.2

**Acceptance Criteria:**
- Vista de historial: timeline visual con iconos diferenciados (jeringa para vacunas, cruz médica para tratamientos)
- Incluir información del animal en header
- Mostrar próximas dosis pendientes en panel de alertas
- Formulario de vacunación: selector de vacuna con búsqueda, selector de lote dependiente, validación HTML5
- Formulario de tratamiento: selector de tipo_tratamiento, textarea para diagnóstico
- JavaScript vanilla para interactividad (búsqueda de vacunas, cálculo de fecha_fin_tratamiento)

---

### Task 5.4: Create Thymeleaf Fragments

**Description:** Crear fragmentos reutilizables para componentes clínicos.

**Files to Create:**
- `src/main/resources/templates/clinical-history/fragments/clinical-card.html`
- `src/main/resources/templates/clinical-history/fragments/clinical-timeline.html`
- `src/main/resources/templates/clinical-history/fragments/clinical-form.html`

**Dependencies:** Task 5.3

**Acceptance Criteria:**
- `clinical-card.html`: componente card para mostrar vacunación o tratamiento
- `clinical-timeline.html`: timeline con orden cronológico y iconos
- `clinical-form.html`: fragmentos reutilizables de formulario
- Usar parámetros Thymeleaf para personalización

---

### Task 5.5: Create CSS Styles for Clinical History Views

**Description:** Crear estilos CSS para vistas de historial clínico.

**Files to Create:**
- `src/main/resources/static/css/clinical-history/animal-history.css`
- `src/main/resources/static/css/clinical-history/clinical-timeline.css`
- `src/main/resources/static/css/clinical-history/clinical-forms.css`

**Dependencies:** Tasks 5.3, 5.4

**Acceptance Criteria:**
- CSS vanilla sin preprocesadores
- Usar variables CSS custom para colores
- Layout responsivo con CSS Grid
- Estilos para timeline con línea conectora vertical
- Estilos para badges de alertas (dosis vencidas en rojo, próximas en amarillo)
- Estilos diferenciados para vacunaciones (azul) y tratamientos (verde)

---

## Phase 6: Configuration & Caching

### Task 6.1: Create Cache Configuration

**Description:** Configurar caching con Caffeine para reportes y alertas.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/cache/CacheConfig.java`
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/cache/CacheNames.java`

**Dependencies:** None

**Acceptance Criteria:**
- Clase `CacheConfig` con `@Configuration` y `@EnableCaching`
- Configurar bean `CacheManager` usando Caffeine
- Cache `animal-report` con TTL 10 minutos
- Cache `alerts` con TTL 1 hora
- Clase `CacheNames` con constantes: `ANIMAL_REPORT = "animal-report"`, `ALERTS = "alerts"`

---

### Task 6.2: Apply Caching to Use Cases

**Description:** Aplicar anotaciones de caching en casos de uso de reportes y alertas.

**Files to Modify:**
- `GenerateAnimalReportUseCase.java`
- `GetOverdueDosesUseCase.java`
- `GetUpcomingDosesAlertsUseCase.java`

**Dependencies:** Tasks 3.4, 6.1

**Acceptance Criteria:**
- `GenerateAnimalReportUseCase`: `@Cacheable(value = CacheNames.ANIMAL_REPORT, key = "#animalId + ':' + #tenantId")`
- Agregar `@CacheEvict` en `RegisterVaccinationUseCase` y `RegisterTreatmentUseCase` para invalidar cache cuando se crea nuevo registro
- Alertas: `@Cacheable(value = CacheNames.ALERTS, key = "#tenantId + ':' + #type")`

---

### Task 6.3: Create Module Configuration Class

**Description:** Crear clase de configuración principal del módulo.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/config/ClinicalHistoryModuleConfig.java`

**Dependencies:** None

**Acceptance Criteria:**
- Clase con anotación `@Configuration`
- Definir beans necesarios para integración
- Configurar Jackson ObjectMapper para serialización JSON de audit logs
- Documentar beans públicos del módulo

---

### Task 6.4: Configure OpenAPI Documentation

**Description:** Configurar generación automática de documentación OpenAPI.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/internal/infrastructure/config/OpenApiConfig.java`

**Dependencies:** Task 4.1

**Acceptance Criteria:**
- Clase con anotación `@Configuration`
- Configurar `@OpenAPIDefinition` con información del módulo
- Configurar security scheme para JWT Bearer token con scope VETERINARIO
- Documentar título: "Clinical History API", versión: "1.0"

---

### Task 6.5: Create Database Migration Scripts

**Description:** Crear scripts de migración de base de datos (Flyway).

**Files to Create:**
- `src/main/resources/db/migration/V1.6__create_clinical_history_tables.sql`

**Dependencies:** Task 2.1

**Acceptance Criteria:**
- Script SQL para crear tablas: `clinical_records`, `vaccination_records`, `treatment_records`, `clinical_history_audit`
- clinical_records: campos base con tenant_id, animal_id, intervention_type
- vaccination_records: FK a clinical_records, campos vacuna_id, lote, proxima_dosis
- treatment_records: FK a clinical_records, campos tipo_tratamiento, diagnostico, costo_tratamiento
- Crear índices en: tenant_id, animal_id, fecha_aplicacion, proxima_dosis, aplicado_por, intervention_type
- Crear índice compuesto: (animal_id, tenant_id), (fecha_aplicacion, tenant_id), (proxima_dosis, tenant_id)
- Definir FKs con ON DELETE RESTRICT
- Constraints: CHECK (dosis_aplicada > 0), CHECK (duracion_tratamiento_dias BETWEEN 1 AND 365)

---

## Phase 7: Testing & Validation

### Task 7.1: Create Unit Tests - Domain Layer

**Description:** Crear tests unitarios para entidades y value objects de dominio.

**Files to Create:**
- `src/test/java/mx/vacapp/clinicalhistory/internal/domain/model/VaccinationRecordTest.java`
- `src/test/java/mx/vacapp/clinicalhistory/internal/domain/model/TreatmentRecordTest.java`
- `src/test/java/mx/vacapp/clinicalhistory/internal/domain/model/NextDoseCalculatorTest.java`

**Dependencies:** Tasks 1.3, 1.4, 1.5

**Acceptance Criteria:**
- Usar JUnit 5 y AssertJ para assertions
- Probar factory methods `create()`
- Probar cálculo automático de proximaDosis
- Probar métodos: `isOverdue()`, `getDaysUntilNextDose()`, `isTreatmentFinished()`
- Probar validación de fechaAplicacion futura (debe lanzar FutureDateException)
- Probar que proximaDosis > 10 años retorna null
- Cobertura mínima: 80% en capa de dominio

---

### Task 7.2: Create Unit Tests - Use Cases

**Description:** Crear tests unitarios para casos de uso con mocks.

**Files to Create:**
- `src/test/java/mx/vacapp/clinicalhistory/internal/application/usecases/vaccination/RegisterVaccinationUseCaseTest.java`
- `src/test/java/mx/vacapp/clinicalhistory/internal/application/usecases/treatment/RegisterTreatmentUseCaseTest.java`
- `src/test/java/mx/vacapp/clinicalhistory/internal/application/usecases/report/GenerateAnimalReportUseCaseTest.java`

**Dependencies:** Tasks 3.2, 3.3, 3.4

**Acceptance Criteria:**
- Usar Mockito para mockear repositorios y puertos de integración
- RegisterVaccinationUseCaseTest: probar flujo completo exitoso, probar rollback cuando descuento de stock falla, probar excepción cuando animal no existe, probar excepción cuando lote vencido
- RegisterTreatmentUseCaseTest: probar creación exitosa, probar validación de diagnóstico para tipo OTRO
- GenerateAnimalReportUseCaseTest: probar cálculo de métricas, probar caching
- Verificar interacciones con mocks: `verify(repository).save(...)`, `verify(vaccinationService).decrementStock(...)`
- Usar `@ExtendWith(MockitoExtension.class)`

---

### Task 7.3: Create Integration Tests - Persistence Layer

**Description:** Crear tests de integración para repositorios con base de datos H2 en memoria.

**Files to Create:**
- `src/test/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/impl/VaccinationRecordRepositoryImplTest.java`
- `src/test/java/mx/vacapp/clinicalhistory/internal/infrastructure/persistence/impl/TreatmentRecordRepositoryImplTest.java`

**Dependencies:** Tasks 2.4, 2.2, 2.1

**Acceptance Criteria:**
- Usar `@DataJpaTest` para configuración de tests de persistencia
- Usar H2 en memoria como base de datos de test
- Probar filtrado por `tenant_id`
- Probar queries de búsqueda por animal_id
- VaccinationRecordRepositoryImplTest: probar búsqueda por próxima dosis vencida, búsqueda por próximas dosis en rango
- Probar paginación

---

### Task 7.4: Create Integration Tests - REST Controllers

**Description:** Crear tests de integración para controladores REST con MockMvc.

**Files to Create:**
- `src/test/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/VaccinationRestControllerTest.java`
- `src/test/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/TreatmentRestControllerTest.java`
- `src/test/java/mx/vacapp/clinicalhistory/internal/infrastructure/controllers/mobile/ClinicalReportRestControllerTest.java`

**Dependencies:** Tasks 4.5, 4.6, 4.7

**Acceptance Criteria:**
- Usar `@WebMvcTest` para tests de controladores
- Mockear Use Cases con `@MockBean`
- Usar `MockMvc` para simular requests HTTP
- Probar validación Bean Validation: enviar requests inválidos y verificar HTTP 400
- Probar control de acceso: usuario sin rol VETERINARIO → HTTP 403
- Probar códigos HTTP: 200, 201, 400, 403, 404, 503
- Probar autenticación JWT con `@WithMockUser(roles="VETERINARIO")`

---

### Task 7.5: Create End-to-End Tests

**Description:** Crear tests end-to-end que validan flujos completos con descuento de stock.

**Files to Create:**
- `src/test/java/mx/vacapp/clinicalhistory/e2e/VaccinationWithStockDecrementE2ETest.java`
- `src/test/java/mx/vacapp/clinicalhistory/e2e/AnimalHealthReportE2ETest.java`
- `src/test/java/mx/vacapp/clinicalhistory/e2e/UpcomingDosesAlertsE2ETest.java`

**Dependencies:** All previous tasks

**Acceptance Criteria:**
- Usar `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)`
- Usar `TestRestTemplate` para hacer requests HTTP reales
- VaccinationWithStockDecrementE2ETest: crear vacuna con stock → crear animal → registrar vacunación → verificar stock decrementado → verificar próxima dosis calculada
- Probar transaccionalidad: si descuento de stock falla, verificar que registro clínico no se creó
- AnimalHealthReportE2ETest: crear animal → registrar vacunaciones + tratamientos → consultar reporte → verificar métricas
- Limpiar base de datos después de cada test

---

### Task 7.6: Create Transactional Tests

**Description:** Crear tests específicos para validar transaccionalidad de descuento de stock.

**Files to Create:**
- `src/test/java/mx/vacapp/clinicalhistory/transaction/VaccinationStockTransactionTest.java`

**Dependencies:** Tasks 3.2, 2.5

**Acceptance Criteria:**
- Simular fallo en VaccinationServicePort.decrementStock() y verificar rollback completo
- Verificar que registro clínico NO se persiste si descuento falla
- Verificar que auditoría NO se registra si transacción falla
- Usar `@Transactional` y `@Rollback` en tests
- Probar aislamiento de transacciones

---

### Task 7.7: Validate Multi-Tenancy Isolation

**Description:** Crear tests específicos para validar aislamiento de datos por tenant.

**Files to Create:**
- `src/test/java/mx/vacapp/clinicalhistory/security/MultiTenancyIsolationTest.java`

**Dependencies:** Tasks 2.4, 4.5, 4.6

**Acceptance Criteria:**
- Crear vacunaciones para 2 tenants diferentes
- Verificar que Tenant A no puede acceder a registros de Tenant B
- Verificar HTTP 403 al intentar acceder a registro de otro tenant
- Verificar que listados filtran correctamente por tenant_id
- Verificar que reportes solo incluyen datos del tenant autenticado

---

### Task 7.8: Performance Tests

**Description:** Crear tests de performance para reportes con grandes volúmenes de datos.

**Files to Create:**
- `src/test/java/mx/vacapp/clinicalhistory/performance/AnimalReportPerformanceTest.java`

**Dependencies:** Task 3.4

**Acceptance Criteria:**
- Crear dataset grande: 1 animal con 500 vacunaciones + 500 tratamientos
- Medir tiempo de generación de reporte
- Validar que primera consulta (sin cache) < 2 segundos
- Validar que consulta desde cache < 100ms
- Usar `StopWatch` para medir tiempos
- Marcar como `@Tag("performance")`

---

### Task 7.9: Create Test Data Builders

**Description:** Crear builders de objetos de test para facilitar creación de datos.

**Files to Create:**
- `src/test/java/mx/vacapp/clinicalhistory/testdata/VaccinationRecordTestDataBuilder.java`
- `src/test/java/mx/vacapp/clinicalhistory/testdata/TreatmentRecordTestDataBuilder.java`

**Dependencies:** Tasks 1.3, 1.4

**Acceptance Criteria:**
- Implementar patrón Builder para objetos de test
- Proveer valores por defecto válidos
- Permitir personalización con métodos fluent
- Facilitar creación de registros con próximas dosis vencidas para testing de alertas

---

## Phase 8: Documentation & Finalization

### Task 8.1: Update Module README

**Description:** Actualizar README del módulo con documentación completa.

**Files to Modify:**
- `src/main/java/mx/vacapp/clinicalhistory/README.md`

**Dependencies:** All implementation tasks

**Acceptance Criteria:**
- Documentar API pública `ClinicalHistoryService`
- Documentar integración con cattle-inventory, vaccination-management, user-management
- Documentar flujo transaccional de registro de vacunación con descuento de stock
- Incluir ejemplos de uso del módulo desde otros módulos
- Documentar inmutabilidad de registros clínicos
- Incluir diagrama de flujo de transacción

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
- Probar endpoint de vacunación desde Swagger UI con autenticación JWT
- Verificar schemas de Request/Response
- Verificar documentación de control de acceso (rol VETERINARIO requerido para POST)

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
- Ejecutar SonarQube y resolver code smells
- Verificar que no hay warnings del compilador

---

### Task 8.4: Security Review

**Description:** Revisar implementación de seguridad, multi-tenancy y control de acceso.

**Files to Review:**
- Todos los repositorios JPA
- Todos los controladores REST
- Casos de uso que modifican datos
- Adaptadores de integración

**Dependencies:** All implementation tasks

**Acceptance Criteria:**
- Verificar que todos los repositorios filtran por `tenant_id`
- Verificar control de acceso: solo VETERINARIO puede crear registros
- Verificar que validación de input está correctamente implementada
- Verificar que excepciones no exponen información sensible
- Verificar que logs no registran datos sensibles (desnormalización está OK)
- Verificar que JWT es validado en todos los endpoints protegidos
- Verificar que transacción de descuento de stock es atómica

---

### Task 8.5: Create Deployment Checklist

**Description:** Crear checklist de despliegue y configuración en producción.

**Files to Create:**
- `docs/deployment/clinical-history-deployment.md`

**Dependencies:** All tasks

**Acceptance Criteria:**
- Documentar dependencias de otros módulos (cattle-inventory, vaccination-management, user-management)
- Documentar scripts de migración de base de datos
- Documentar configuración de cache (Caffeine)
- Documentar índices de base de datos requeridos
- Documentar permisos y roles necesarios (VETERINARIO)
- Documentar smoke tests post-deployment
- Incluir verificación de integración transaccional
- Incluir rollback plan

---

### Task 8.6: Integration with Other Modules

**Description:** Implementar integración API pública del módulo.

**Files to Create:**
- `src/main/java/mx/vacapp/clinicalhistory/ClinicalHistoryServiceImpl.java`

**Dependencies:** Tasks 1.1, 3.2, 3.3, 3.4

**Acceptance Criteria:**
- Implementar métodos de `ClinicalHistoryService` interface
- `getVaccinationCount()`: contar vacunaciones del animal
- `getTreatmentCount()`: contar tratamientos del animal
- `hasOverdueDoses()`: verificar si tiene dosis vencidas
- `getLastInterventionDate()`: obtener fecha de última intervención
- `getUpcomingDoses()`: obtener próximas dosis pendientes
- Anotar con `@Service` y `@RequiredArgsConstructor`
- Inyectar repositorios necesarios

---

### Task 8.7: Final Manual Testing

**Description:** Ejecutar pruebas manuales end-to-end en entorno de desarrollo.

**Test Scenarios:**
1. Veterinario crea vacunación → verificar stock decrementado → verificar próxima dosis calculada
2. Intentar crear vacunación con stock insuficiente → validar error 400
3. Intentar crear vacunación con lote vencido → validar error 400
4. Usuario sin rol VETERINARIO intenta crear vacunación → validar error 403
5. Crear tratamiento médico → verificar fecha_fin_tratamiento calculada
6. Consultar historial de animal → verificar vacunaciones + tratamientos ordenados
7. Consultar alertas de dosis vencidas → verificar ordenamiento por urgencia
8. Simular fallo de vaccination-management → verificar rollback de vacunación
9. Probar Swagger UI → ejecutar todos los endpoints con JWT

**Dependencies:** All tasks

**Acceptance Criteria:**
- Todos los escenarios pasan exitosamente
- No hay errores 500 en logs
- Transaccionalidad funciona correctamente (rollback en fallos)
- Validaciones funcionan
- Control de acceso funciona
- UI web renderiza correctamente

---

### Task 8.8: Create User Acceptance Test (UAT) Plan

**Description:** Crear plan de pruebas de aceptación de usuario.

**Files to Create:**
- `docs/testing/clinical-history-uat-plan.md`

**Dependencies:** All tasks

**Acceptance Criteria:**
- Definir casos de uso de negocio veterinario a probar
- Definir criterios de aceptación: registro de vacunación con descuento automático, cálculo de próximas dosis, reportes de salud
- Incluir datos de prueba realistas (animales, vacunas con stock, veterinarios)
- Definir roles de usuarios para testing
- Incluir checklist de features: inmutabilidad de registros, control de acceso, transaccionalidad, alertas
- Definir métricas de éxito (tiempo de respuesta, precisión de cálculos)

---

## Summary

### Total Tasks: 52

**Phase 1 - Module Structure & Domain:** 8 tasks  
**Phase 2 - Persistence & Integration:** 5 tasks  
**Phase 3 - Application Layer:** 4 tasks  
**Phase 4 - REST API:** 9 tasks  
**Phase 5 - Web Controllers:** 5 tasks  
**Phase 6 - Configuration:** 5 tasks  
**Phase 7 - Testing:** 9 tasks  
**Phase 8 - Documentation:** 7 tasks  

### Critical Path

1. Task 1.1 → 1.2 → 1.3, 1.4 → 1.7, 1.8 (Domain foundation + integration ports)
2. Task 2.1 → 2.2 → 2.3 → 2.4, 2.5 (Persistence + integration adapters)
3. Task 3.1 → 3.2, 3.3, 3.4 (Use cases)
4. Task 4.1 → 4.2, 4.3 → 4.4 → 4.5, 4.6, 4.7, 4.8 (REST API)
5. Task 5.1 → 5.2 → 5.3, 5.4, 5.5 (Web UI)
6. Testing tasks can run in parallel after implementation is complete

### Estimated Timeline

- **Phase 1-2 (Domain + Persistence):** 4-5 days
- **Phase 3 (Application Layer):** 3-4 days
- **Phase 4 (REST API):** 4-5 days
- **Phase 5 (Web UI):** 3-4 days
- **Phase 6 (Configuration):** 1-2 days
- **Phase 7 (Testing):** 5-6 days
- **Phase 8 (Documentation):** 2-3 days

**Total Estimated Time:** 22-29 working days (approximately 4.5-6 weeks)

---

## Notes

- **Dependencias críticas**: Este módulo DEBE implementarse después de cattle-inventory, vaccination-management, y user-management
- **Inmutabilidad**: Los registros clínicos NO se pueden modificar ni eliminar una vez creados
- **Transaccionalidad**: Registro de vacunación + descuento de stock debe ser atómico (rollback si cualquiera falla)
- **Control de acceso**: Solo veterinarios crean registros, otros roles solo leen
- **Desnormalización**: Copias de datos (nombre_vacuna, nombre_veterinario) son intencionales para preservar historial inmutable
- **Cálculo automático**: proximaDosis se calcula automáticamente y no debe ser modificable manualmente
- **Integración resiliente**: Si vaccination-management está temporalmente no disponible, rechazar vacunaciones pero permitir tratamientos
- **Caching**: Reportes por animal cacheados 10 minutos, alertas cacheadas 1 hora
- **Performance**: Índices en tenant_id, animal_id, fecha_aplicacion, proxima_dosis son críticos
- **Validaciones**: fecha_aplicacion no puede ser futura, dosis_aplicada debe ser > 0, lote no puede estar vencido
- **Auditoría**: Incluir resultado de descuento de stock (stock_antes, stock_despues) para trazabilidad completa
- **Herencia de tablas**: clinical_records es tabla base, vaccination_records y treatment_records tienen FK a ella
- Seguir convenciones de AGENTS.md para nombres de clases, métodos y estructura de código
