# Requirements Document

## Introduction

El módulo de historial clínico (clinical-history) proporciona funcionalidades completas para el registro, gestión y seguimiento de intervenciones veterinarias en el ganado de Vacapp. El sistema soporta dos tipos de registros: **vacunaciones** (con integración automática al módulo vaccination-management para descuento de stock y cálculo de próximas dosis) y **tratamientos médicos generales** (desparasitaciones, antibióticos, vitaminas, cirugías, curaciones). El módulo implementa control de acceso basado en roles (solo veterinarios pueden registrar intervenciones), integración con cattle-inventory para validación de animales, auditoría completa de cambios, y generación de reportes de salud por animal, por periodo, y por tipo de intervención.

## Glossary

- **Clinical_History_Module**: El módulo que gestiona el registro de intervenciones veterinarias (vacunaciones y tratamientos)
- **Clinical_Record**: Registro de una intervención veterinaria aplicada a un animal
- **Vaccination_Record**: Registro específico de aplicación de vacuna con descuento automático de stock
- **Treatment_Record**: Registro de tratamiento médico general (no vacuna)
- **Intervention_Type**: Tipo de intervención (VACCINATION, TREATMENT)
- **Treatment_Type**: Categoría de tratamiento médico definida por el usuario (ej: Desparasitación, Antibiótico, Vitaminas, Cirugía, Curación, etc.). Gestionada mediante CRUD
- **Via_Administracion**: Vía de administración del medicamento (INTRAMUSCULAR, SUBCUTANEA, INTRAVENOSA, ORAL, TOPICA)
- **Next_Dose**: Fecha calculada automáticamente para la próxima aplicación de vacuna
- **Stock_Decrement**: Descuento automático de stock de vacuna basado en dosis aplicada
- **Veterinarian**: Usuario con rol VETERINARIO autorizado para registrar intervenciones
- **Clinical_Report**: Reporte de historial clínico con filtros por animal, periodo, tipo
- **Audit_Trail**: Registro de auditoría de todas las intervenciones realizadas
- **Cattle_Validation**: Validación de existencia y estado activo del animal antes de registrar intervención
- **Vaccine_Lot**: Lote de vacuna con stock disponible y fecha de vencimiento

## Requirements

### Requirement 1: Registro de Vacunaciones con Descuento Automático de Stock

**User Story:** Como veterinario, quiero registrar la aplicación de vacunas a los animales, para que el sistema descuente automáticamente el stock y calcule la próxima dosis según el intervalo de refuerzo.

#### Acceptance Criteria

1. WHEN un veterinario autenticado registra una vacunación con animal_id válido, vacuna_id válido, lote válido, dosis aplicada, vía de administración, y fecha de aplicación, THE Clinical_History_Module SHALL crear el registro de vacunación con intervention_type = VACCINATION y retornar código HTTP 201
2. WHEN se registra una vacunación, THE Clinical_History_Module SHALL validar que el animal existe y está activo consultando cattle-inventory
3. WHEN se registra una vacunación, THE Clinical_History_Module SHALL validar que la vacuna existe y el lote tiene stock suficiente consultando vaccination-management
4. WHEN se registra una vacunación con dosis_aplicada = X, THE Clinical_History_Module SHALL decrementar el stock del lote de vacuna en X unidades de forma transaccional
5. IF el descuento de stock falla (stock insuficiente o lote vencido), THEN THE Clinical_History_Module SHALL revertir la transacción completa y retornar código HTTP 400 con mensaje "Stock insuficiente o lote vencido"
6. WHEN se registra una vacunación, THE Clinical_History_Module SHALL calcular automáticamente proxima_dosis = fecha_aplicacion + intervalo_refuerzo_dias de la vacuna
7. IF la vacuna tiene intervalo_refuerzo_dias = NULL (vacuna de dosis única), THEN proxima_dosis = NULL
8. WHEN se proporciona dosis_aplicada con valor numérico positivo, THE Clinical_History_Module SHALL aceptar el valor y usarlo para el descuento de stock
9. WHEN se proporciona dosis_aplicada <= 0 o NULL, THE Clinical_History_Module SHALL retornar código HTTP 400 con mensaje "La dosis aplicada debe ser un valor positivo"
10. WHEN un veterinario lista las vacunaciones de un animal, THE Clinical_History_Module SHALL retornar registros filtrados por animal_id y tenant_id con paginación (máximo 50 registros por página)
11. WHEN un veterinario consulta una vacunación por ID, THE Clinical_History_Module SHALL incluir en la respuesta: record_id, animal_id, vacuna_id, nombre_vacuna, lote, dosis_aplicada, via_administracion, fecha_aplicacion, proxima_dosis, notas, aplicado_por (nombre del veterinario), created_at
12. THE Clinical_History_Module SHALL registrar en auditoría: record_id, entity_type (VACCINATION), operation_type (CREATE), timestamp, modified_by (veterinario_id), tenant_id, new_values (JSON con datos completos)

### Requirement 2: Registro de Tratamientos Médicos Generales

**User Story:** Como veterinario, quiero registrar tratamientos médicos generales (desparasitaciones, antibióticos, cirugías, etc.) aplicados a los animales, para que pueda llevar un historial completo de salud.

#### Acceptance Criteria

1. WHEN un veterinario autenticado registra un tratamiento médico con animal_id válido, treatment_type_id válido, medicamento_nombre, dosis_aplicada, vía_administración, diagnóstico, duracion_tratamiento_dias, costo_tratamiento, fecha_aplicacion, y notas opcionales, THE Clinical_History_Module SHALL crear el registro con intervention_type = TREATMENT y retornar código HTTP 201
2. WHEN se registra un tratamiento, THE Clinical_History_Module SHALL validar que el animal existe y está activo consultando cattle-inventory
3. THE Clinical_History_Module SHALL validar que el treatment_type_id existe y tiene status = ACTIVO antes de crear el tratamiento
4. IF el treatment_type_id NO existe o está ARCHIVADO, THEN THE Clinical_History_Module SHALL retornar código HTTP 400 con mensaje "Tipo de tratamiento no encontrado o inactivo"
5. WHEN se proporciona medicamento_nombre con longitud entre 2 y 200 caracteres, THE Clinical_History_Module SHALL aceptar el valor
6. WHEN se proporciona diagnostico con longitud entre 5 y 1000 caracteres, THE Clinical_History_Module SHALL aceptar el valor
7. WHEN se proporciona duracion_tratamiento_dias >= 1 y <= 365, THE Clinical_History_Module SHALL aceptar el valor
8. IF duracion_tratamiento_dias > 1, THEN THE Clinical_History_Module SHALL calcular fecha_fin_tratamiento = fecha_aplicacion + duracion_tratamiento_dias
9. WHEN se proporciona costo_tratamiento >= 0 y <= 999,999.99, THE Clinical_History_Module SHALL aceptar el valor
10. WHEN un veterinario lista los tratamientos de un animal, THE Clinical_History_Module SHALL retornar registros filtrados por animal_id y tenant_id ordenados por fecha_aplicacion descendente
11. WHEN un veterinario consulta un tratamiento por ID, THE Clinical_History_Module SHALL incluir en la respuesta: record_id, animal_id, treatment_type_id, treatment_type_nombre, medicamento_nombre, dosis_aplicada, via_administracion, diagnostico, fecha_aplicacion, fecha_fin_tratamiento, duracion_tratamiento_dias, costo_tratamiento, notas, aplicado_por, created_at
12. THE Clinical_History_Module SHALL registrar en auditoría: record_id, entity_type (TREATMENT), operation_type (CREATE), timestamp, modified_by, tenant_id, new_values

### Requirement 3: Gestión de Tipos de Tratamiento (CRUD)

**User Story:** Como administrador o veterinario, quiero crear y gestionar tipos de tratamiento personalizados para mi organización, para que pueda categorizar tratamientos según mis necesidades específicas.

#### Acceptance Criteria

1. THE Clinical_History_Module SHALL exponer endpoint POST /api/v1/clinical-history/treatment-types para crear nuevos tipos de tratamiento
2. WHEN un usuario autenticado crea un tipo de tratamiento con nombre válido (2-100 caracteres), descripción opcional (máx 500 caracteres), THE Clinical_History_Module SHALL crear el registro con status = ACTIVO y retornar código HTTP 201
3. THE Clinical_History_Module SHALL validar que el nombre del tipo de tratamiento no esté duplicado (case-insensitive) dentro del mismo tenant y rancho
4. WHEN se intenta crear un tipo de tratamiento con nombre duplicado, THE Clinical_History_Module SHALL retornar código HTTP 409 con mensaje "Ya existe un tipo de tratamiento con ese nombre"
5. THE Clinical_History_Module SHALL aplicar UNIQUE constraint en columnas (tenant_id, rancho_id, LOWER(nombre))
6. THE Clinical_History_Module SHALL exponer endpoint GET /api/v1/clinical-history/treatment-types con filtros opcionales: status (ACTIVO/ARCHIVADO), rancho_id, search (búsqueda por nombre)
7. WHEN se lista tipos de tratamiento sin filtros, THE Clinical_History_Module SHALL retornar todos los tipos ACTIVOS del tenant ordenados por nombre ascendente
8. THE Clinical_History_Module SHALL exponer endpoint GET /api/v1/clinical-history/treatment-types/{id} para consultar un tipo específico
9. THE Clinical_History_Module SHALL exponer endpoint PUT /api/v1/clinical-history/treatment-types/{id} para actualizar nombre y descripción
10. WHEN se actualiza un tipo de tratamiento, THE Clinical_History_Module SHALL validar que el nuevo nombre no esté duplicado (excluyendo el registro actual)
11. THE Clinical_History_Module SHALL exponer endpoint DELETE /api/v1/clinical-history/treatment-types/{id} para archivar (soft delete) tipos de tratamiento
12. WHEN se archiva un tipo de tratamiento, THE Clinical_History_Module SHALL cambiar status a ARCHIVADO sin eliminar físicamente el registro
13. WHEN se intenta archivar un tipo de tratamiento que tiene tratamientos asociados activos, THE Clinical_History_Module SHALL retornar código HTTP 409 con mensaje "No se puede archivar. Existen tratamientos registrados con este tipo"
14. THE Clinical_History_Module SHALL permitir que usuarios con roles ADMIN y MANAGER puedan crear, actualizar y archivar tipos de tratamiento
15. THE Clinical_History_Module SHALL permitir que usuarios con rol VETERINARIO puedan solo LEER tipos de tratamiento
16. THE Clinical_History_Module SHALL aplicar filtro tenant_id automáticamente en todas las operaciones de tipos de tratamiento
17. THE Clinical_History_Module SHALL registrar en auditoría operaciones CREATE, UPDATE, ARCHIVE sobre tipos de tratamiento
18. WHEN se registra un tratamiento médico, THE Clinical_History_Module SHALL validar que el treatment_type_id existe y está ACTIVO
19. IF el treatment_type_id NO existe o está ARCHIVADO, THEN THE Clinical_History_Module SHALL retornar código HTTP 400 con mensaje "Tipo de tratamiento no encontrado o inactivo"
20. THE Clinical_History_Module SHALL incluir en respuesta de tipos de tratamiento: treatment_type_id, nombre, descripcion, status, rancho_id, uso_count (cantidad de tratamientos que lo usan), created_at, updated_at

### Requirement 4: Control de Acceso Basado en Roles

**User Story:** Como sistema, quiero que solo los usuarios con rol VETERINARIO puedan registrar intervenciones clínicas, para que garantice la integridad y veracidad de los registros médicos.

#### Acceptance Criteria

1. WHEN un usuario autenticado intenta registrar una vacunación o tratamiento, THE Clinical_History_Module SHALL validar que el usuario tiene rol VETERINARIO extrayéndolo del JWT_Token
2. IF el usuario NO tiene rol VETERINARIO, THEN THE Clinical_History_Module SHALL retornar código HTTP 403 con mensaje "Solo veterinarios pueden registrar intervenciones clínicas"
3. WHEN un usuario con rol VETERINARIO registra una intervención, THE Clinical_History_Module SHALL guardar aplicado_por = user_id del veterinario autenticado
4. THE Clinical_History_Module SHALL permitir que usuarios con roles ADMIN, MANAGER, y VETERINARIO puedan LEER (consultar) registros clínicos
5. THE Clinical_History_Module SHALL permitir que usuarios con rol WORKER puedan LEER registros clínicos pero NO crear nuevos
6. THE Clinical_History_Module SHALL prohibir ACTUALIZAR o ELIMINAR registros clínicos una vez creados (inmutabilidad de historial médico)
7. WHEN un usuario intenta modificar un registro clínico existente, THE Clinical_History_Module SHALL retornar código HTTP 405 con mensaje "Los registros clínicos son inmutables. Cree un nuevo registro si es necesario"
8. THE Clinical_History_Module SHALL aplicar filtro tenant_id automáticamente en todas las operaciones de lectura y escritura

### Requirement 5: Integración con Módulo Cattle-Inventory

**User Story:** Como sistema, quiero validar la existencia y estado del animal antes de registrar intervenciones, para que garantice consistencia de datos entre módulos.

#### Acceptance Criteria

1. WHEN se registra una vacunación o tratamiento, THE Clinical_History_Module SHALL invocar método CattleService.getAnimalById(animal_id) para validar existencia
2. IF el animal NO existe, THEN THE Clinical_History_Module SHALL retornar código HTTP 404 con mensaje "Animal no encontrado"
3. WHEN se obtiene información del animal, THE Clinical_History_Module SHALL validar que status = ACTIVA
4. IF el animal tiene status diferente a ACTIVA (VENDIDA, MUERTA, EN_CUARENTENA), THEN THE Clinical_History_Module SHALL retornar código HTTP 400 con mensaje "No se pueden registrar intervenciones en animales con status: {status}"
5. WHEN se registra una intervención, THE Clinical_History_Module SHALL incluir en el registro: animal_no_identificador, animal_nombre (si existe), animal_tipo (VACA_LECHERA, TORO, etc.)
6. THE Clinical_History_Module SHALL validar que animal.tenant_id = tenant_id del veterinario autenticado
7. IF tenant_id no coincide, THEN THE Clinical_History_Module SHALL retornar código HTTP 403 con mensaje "El animal no pertenece a su organización"
8. WHEN se consulta el historial clínico de un animal, THE Clinical_History_Module SHALL enriquecer la respuesta con datos actuales del animal (nombre, identificador, ubicación actual)

### Requirement 6: Integración con Módulo Vaccination-Management

**User Story:** Como sistema, quiero integrarme con el módulo de vacunación para decrementar stock automáticamente y obtener información de vacunas, para que garantice consistencia de inventarios.

#### Acceptance Criteria

1. WHEN se registra una vacunación, THE Clinical_History_Module SHALL invocar método VaccinationService.getVaccinaById(vacuna_id) para obtener información de la vacuna
2. IF la vacuna NO existe, THEN THE Clinical_History_Module SHALL retornar código HTTP 404 con mensaje "Vacuna no encontrada"
3. WHEN se obtiene información de la vacuna, THE Clinical_History_Module SHALL validar que status = ACTIVO
4. WHEN se registra una vacunación, THE Clinical_History_Module SHALL invocar método VaccinationService.validateLoteStock(lote, dosis_aplicada) para validar disponibilidad
5. IF el lote NO existe o está vencido, THEN THE Clinical_History_Module SHALL retornar código HTTP 400 con mensaje "Lote de vacuna no encontrado o vencido"
6. IF el stock del lote es insuficiente (stock_disponible < dosis_aplicada), THEN THE Clinical_History_Module SHALL retornar código HTTP 400 con mensaje "Stock insuficiente. Disponible: {stock_disponible} unidades"
7. WHEN la validación de stock es exitosa, THE Clinical_History_Module SHALL invocar método VaccinationService.decrementStock(lote, dosis_aplicada, clinical_record_id) de forma transaccional
8. IF el descuento de stock falla por cualquier razón, THEN THE Clinical_History_Module SHALL revertir la transacción completa (rollback del registro clínico)
9. WHEN se calcula proxima_dosis, THE Clinical_History_Module SHALL usar el campo intervalo_refuerzo_dias de la vacuna obtenida del módulo vaccination-management
10. THE Clinical_History_Module SHALL registrar en el registro clínico: vacuna_nombre_comercial, vacuna_laboratorio, vacuna_lote, vacuna_fecha_vencimiento (copia desnormalizada para historial inmutable)
11. THE Clinical_History_Module SHALL validar que vacuna.tenant_id = tenant_id del veterinario autenticado
12. WHEN se consulta historial de vacunaciones, THE Clinical_History_Module SHALL enriquecer con información actual de la vacuna (si aún existe en el sistema)

### Requirement 7: Generación de Reportes de Historial Clínico

**User Story:** Como veterinario o administrador, quiero generar reportes de historial clínico con filtros por animal, periodo, tipo de intervención, para que pueda analizar la salud del ganado y tomar decisiones informadas.

#### Acceptance Criteria

1. THE Clinical_History_Module SHALL exponer endpoint GET /api/v1/clinical-history/reports/by-animal con parámetros: animal_id (required), start_date (optional), end_date (optional), intervention_type (optional: VACCINATION, TREATMENT, ALL)
2. WHEN un usuario solicita reporte por animal, THE Clinical_History_Module SHALL retornar JSON con: animal_info (identificador, nombre, tipo, edad), total_vacunaciones, total_tratamientos, ultima_vacunacion, ultimo_tratamiento, proximas_dosis_pendientes[], historial[] (ordenado por fecha descendente)
3. THE Clinical_History_Module SHALL exponer endpoint GET /api/v1/clinical-history/reports/by-period con parámetros: start_date (required), end_date (required), rancho_id (optional), intervention_type (optional)
4. WHEN un usuario solicita reporte por periodo, THE Clinical_History_Module SHALL retornar JSON con: total_intervenciones, distribucion_por_tipo {VACCINATION: count, TREATMENT: count}, distribucion_por_via_administracion {INTRAMUSCULAR: count, ...}, costo_total_tratamientos, animales_tratados_unicos (count), top_tratamientos_aplicados[]
5. THE Clinical_History_Module SHALL exponer endpoint GET /api/v1/clinical-history/reports/upcoming-doses con parámetros: rancho_id (optional), days_ahead (default: 30)
6. WHEN un usuario solicita reporte de próximas dosis, THE Clinical_History_Module SHALL retornar vacunaciones con proxima_dosis BETWEEN today AND (today + days_ahead) ordenadas por proxima_dosis ascendente
7. THE Clinical_History_Module SHALL incluir en reporte de próximas dosis: animal_id, animal_identificador, vacuna_nombre, fecha_aplicacion_anterior, proxima_dosis, dias_restantes
8. THE Clinical_History_Module SHALL aplicar filtro tenant_id en todos los reportes
9. WHEN se solicita reporte con filtros que no retornan datos, THE Clinical_History_Module SHALL retornar JSON con estructura vacía (arrays vacíos) y código HTTP 200 (no 404)
10. THE Clinical_History_Module SHALL cachear reportes por animal durante 10 minutos con key = "report:animal:{animal_id}:tenant:{tenant_id}"
11. WHEN se registra una nueva intervención para un animal, THE Clinical_History_Module SHALL invalidar cache de reportes de ese animal usando @CacheEvict

### Requirement 7: API REST para Gestión de Historial Clínico

**User Story:** Como desarrollador móvil, quiero consumir una API REST para gestionar el historial clínico, para que pueda integrar la funcionalidad en la aplicación móvil.

#### Acceptance Criteria

1. THE Clinical_History_Module SHALL exponer endpoints REST bajo ruta base /api/v1/clinical-history/ con autenticación JWT obligatoria
2. THE Clinical_History_Module SHALL implementar endpoints: POST /vaccinations (crear vacunación), GET /vaccinations (listar vacunaciones), GET /vaccinations/{id} (consultar vacunación), GET /animals/{animalId}/vaccinations (historial de vacunaciones por animal)
3. THE Clinical_History_Module SHALL implementar endpoints: POST /treatments (crear tratamiento), GET /treatments (listar tratamientos), GET /treatments/{id} (consultar tratamiento), GET /animals/{animalId}/treatments (historial de tratamientos por animal)
4. THE Clinical_History_Module SHALL implementar endpoints: GET /animals/{animalId}/history (historial completo: vacunaciones + tratamientos), GET /reports/by-animal, GET /reports/by-period, GET /reports/upcoming-doses
5. THE Clinical_History_Module SHALL aceptar requests con header Content-Type: application/json; charset=UTF-8 y retornar responses con mismo Content-Type
6. WHEN un endpoint retorna colección (lista de registros), THE Clinical_History_Module SHALL incluir objeto pagination con campos: page (0-based), size (1-100), total (non-negative integer)
7. WHEN una operación completa exitosamente, THE Clinical_History_Module SHALL retornar código HTTP: 200 para lecturas, 201 para creaciones
8. WHEN un Request DTO falla validación Bean Validation, THE Clinical_History_Module SHALL retornar código HTTP 400 con JSON conteniendo array de errores (field, message)
9. WHEN un JWT_Token es inválido o ausente, THE Clinical_History_Module SHALL retornar código HTTP 401 con mensaje "Autenticación requerida"
10. WHEN un usuario sin rol VETERINARIO intenta crear registro, THE Clinical_History_Module SHALL retornar código HTTP 403 con mensaje "Acceso denegado. Rol requerido: VETERINARIO"
11. WHEN un recurso solicitado no existe (registro clínico con ID inexistente), THE Clinical_History_Module SHALL retornar código HTTP 404 con mensaje "Registro no encontrado"
12. THE Clinical_History_Module SHALL documentar todos los endpoints en archivo OpenAPI 3.0 YAML ubicado en src/main/resources/openapi/openapi-clinical-history.yaml

### Requirement 8: Interfaz Web para Visualización de Historial Clínico

**User Story:** Como veterinario o administrador web, quiero visualizar el historial clínico de los animales en una interfaz intuitiva, para que pueda consultar intervenciones pasadas y próximas dosis fácilmente.

#### Acceptance Criteria

1. THE Clinical_History_Module SHALL servir página HTML en ruta /clinical-history/animals/{animalId} con historial completo del animal mostrando: información del animal, timeline de intervenciones, próximas dosis pendientes
2. WHEN un usuario accede al historial de un animal, THE Clinical_History_Module SHALL mostrar timeline visual con iconos diferenciados para vacunaciones (jeringa) y tratamientos (cruz médica)
3. THE Clinical_History_Module SHALL renderizar formulario modal para registrar nueva vacunación con campos: animal (pre-seleccionado), vacuna (selector con búsqueda), lote (selector dependiente de vacuna), dosis_aplicada, via_administracion (dropdown), fecha_aplicacion (date picker), notas (textarea opcional)
4. THE Clinical_History_Module SHALL renderizar formulario modal para registrar nuevo tratamiento con campos: animal (pre-seleccionado), tipo_tratamiento (dropdown), medicamento_nombre, dosis_aplicada, via_administracion, diagnostico (textarea required), duracion_tratamiento_dias, costo_tratamiento, fecha_aplicacion, notas
5. WHEN validación JavaScript detecta campo inválido, THE Clinical_History_Module SHALL mostrar mensaje de error inline sin enviar request HTTP
6. THE Clinical_History_Module SHALL implementar búsqueda de animales en tiempo real usando JavaScript vanilla con debounce de 300ms
7. WHEN una petición Fetch API retorna código 4xx o 5xx, THE Clinical_History_Module SHALL mostrar notificación toast con mensaje de error en español y botón para cerrar
8. THE Clinical_History_Module SHALL incluir fragmentos Thymeleaf: fragments/clinical-card.html (card de registro), fragments/clinical-timeline.html (timeline de intervenciones), fragments/clinical-form.html (formularios)
9. THE Clinical_History_Module SHALL usar CSS Grid para layout responsivo que se adapte a tablets (mínimo 768px)
10. THE Clinical_History_Module SHALL mostrar badge de alerta cuando un animal tiene próxima dosis vencida (proxima_dosis < fecha_actual)

### Requirement 9: Auditoría de Cambios en Historial Clínico

**User Story:** Como administrador, quiero un registro completo de todas las intervenciones clínicas registradas, para que pueda rastrear quién aplicó qué tratamiento y cuándo.

#### Acceptance Criteria

1. THE Clinical_History_Module SHALL registrar en tabla clinical_history_audit cada operación CREATE sobre registros clínicos (vacunaciones y tratamientos)
2. WHEN se crea un registro clínico, THE Clinical_History_Module SHALL insertar en clinical_history_audit: audit_id (UUID), entity_type (VACCINATION/TREATMENT), entity_id (UUID del registro), operation_type (CREATE), timestamp (UTC), modified_by (veterinario_id), tenant_id, new_values (JSON con datos completos)
3. THE Clinical_History_Module SHALL incluir en new_values: animal_id, intervention_type, fecha_aplicacion, aplicado_por, dosis_aplicada, via_administracion, + campos específicos (vacuna_id/tipo_tratamiento)
4. WHEN un administrador consulta historial de auditoría via GET /api/v1/clinical-history/audit con filtros opcionales: animal_id, veterinario_id, start_date, end_date, intervention_type, THE Clinical_History_Module SHALL retornar filas paginadas (máximo 500 por página)
5. THE Clinical_History_Module SHALL retener registros de auditoría por mínimo 1095 días (3 años) antes de permitir purga manual por super_admin
6. WHEN se revierte una operación (rollback de transacción por fallo en descuento de stock), THE Clinical_History_Module SHALL NO registrar entrada en clinical_history_audit (solo operaciones confirmadas)
7. THE Clinical_History_Module SHALL incluir en audit log el resultado del descuento de stock: stock_antes, stock_despues, lote_id (para vacunaciones)
8. WHEN se consulta audit log, THE Clinical_History_Module SHALL enriquecer respuesta con nombre del veterinario y nombre del animal (joins con user-management y cattle-inventory)

### Requirement 10: Validación y Serialización de DTOs

**User Story:** Como desarrollador, quiero que los DTOs sean Records Java con validación automática, para que garantice calidad y consistencia de datos clínicos.

#### Acceptance Criteria

1. THE Clinical_History_Module SHALL definir Request DTOs como Java Records en infrastructure/controllers/*/dtos/ con anotaciones Bean Validation (@NotNull, @NotBlank, @Positive, @DecimalMin, @DecimalMax, @Size, @Pattern)
2. THE Clinical_History_Module SHALL definir Response DTOs como Java Records sin anotaciones Bean Validation
3. WHEN un Request DTO inválido es recibido, THE Clinical_History_Module SHALL retornar código HTTP 400 con JSON: {"errors": [{"field": "dosis_aplicada", "message": "validation message"}]}
4. THE Clinical_History_Module SHALL mapear entre DTOs y objetos de dominio usando clases XxxMapper con métodos toEntity() y toDomain()
5. FOR ALL Response DTOs conteniendo campos de tipo BigDecimal (dosis_aplicada, costo_tratamiento), THE Clinical_History_Module SHALL formatear con máximo 2 decimales
6. FOR ALL Response DTOs con campos LocalDate o Instant, THE Clinical_History_Module SHALL formatear como String en formato ISO 8601: yyyy-MM-dd para LocalDate, yyyy-MM-dd'T'HH:mm:ss'Z' para Instant con timezone UTC
7. THE Clinical_History_Module SHALL validar que notas no contenga solo espacios en blanco (trim antes de validar)
8. WHEN validación de dosis_aplicada falla (valor negativo o cero), THE Clinical_History_Module SHALL incluir en errors: {"field": "dosis_aplicada", "message": "La dosis aplicada debe ser mayor que 0"}
9. THE Clinical_History_Module SHALL validar que fecha_aplicacion no sea futura (fecha_aplicacion <= fecha_actual)
10. IF fecha_aplicacion es futura, THEN THE Clinical_History_Module SHALL retornar HTTP 400 con mensaje "La fecha de aplicación no puede ser futura"

### Requirement 11: Multi-tenancy y Seguridad

**User Story:** Como sistema multitenant, quiero que todos los datos clínicos estén aislados por tenant, para que cada organización solo acceda a sus propios registros médicos.

#### Acceptance Criteria

1. THE Clinical_History_Module SHALL aplicar filtro WHERE tenant_id = :currentTenantId automáticamente en todas las operaciones de lectura sobre registros clínicos
2. WHEN un veterinario con Business_Role ejecuta cualquier operación clínica, THE Clinical_History_Module SHALL extraer tenant_id del JWT_Token y aplicar filtro en queries
3. THE Clinical_History_Module SHALL validar en cada creación que tenant_id del registro coincida con tenant_id del JWT_Token del veterinario autenticado
4. WHEN un veterinario intenta crear registro clínico especificando tenant_id diferente al de su JWT, THE Clinical_History_Module SHALL retornar código HTTP 403 con mensaje "No autorizado para modificar datos de otro tenant"
5. THE Clinical_History_Module SHALL aplicar filtro tenant_id en operaciones de conteo (COUNT), agregación (SUM de costo_tratamiento), y paginación
6. THE Clinical_History_Module SHALL validar que animal.tenant_id = tenant_id del veterinario antes de permitir registro
7. THE Clinical_History_Module SHALL validar que vacuna.tenant_id = tenant_id del veterinario antes de permitir registro de vacunación
8. THE Clinical_History_Module SHALL prohibir modificar tenant_id de cualquier registro clínico después de la creación
9. WHEN se consulta historial de un animal, THE Clinical_History_Module SHALL validar que el animal pertenece al tenant del usuario autenticado antes de retornar datos

### Requirement 12: Performance y Optimización

**User Story:** Como sistema, quiero optimizar consultas y cálculos de reportes, para que garantice tiempos de respuesta aceptables incluso con grandes volúmenes de datos.

#### Acceptance Criteria

1. THE Clinical_History_Module SHALL crear índices en columnas: tenant_id, animal_id, veterinario_id, fecha_aplicacion, intervention_type, proxima_dosis en tabla clinical_records
2. THE Clinical_History_Module SHALL utilizar queries JOIN optimizadas al listar historial con información de animal y veterinario
3. WHEN un usuario consulta historial de un animal, THE Clinical_History_Module SHALL ejecutar máximo 2 queries SQL (1 para registros, 1 para información del animal) usando proyección de solo campos necesarios
4. THE Clinical_History_Module SHALL cachear resultados de reportes por animal durante 10 minutos usando @Cacheable con key = "report:animal:{animal_id}:tenant:{tenant_id}"
5. WHEN se crea un nuevo registro clínico, THE Clinical_History_Module SHALL invalidar cache de reportes del animal usando @CacheEvict
6. THE Clinical_History_Module SHALL implementar paginación usando LIMIT y OFFSET con máximo 100 registros por página para prevenir queries lentas
7. WHEN se genera reporte de próximas dosis, THE Clinical_History_Module SHALL usar índice en proxima_dosis para búsqueda optimizada
8. THE Clinical_History_Module SHALL usar @Transactional(readOnly = true) en operaciones de solo lectura para optimizar conexiones de base de datos
9. WHEN se calcula reporte por periodo con agregaciones, THE Clinical_History_Module SHALL usar queries con GROUP BY optimizadas en lugar de procesamiento en memoria
10. THE Clinical_History_Module SHALL implementar paginación cursor-based en lugar de offset-based para listas grandes (> 1000 registros)

### Requirement 13: Manejo de Casos Especiales y Edge Cases

**User Story:** Como sistema, quiero manejar correctamente casos especiales y edge cases, para que prevenga estados inconsistentes y errores inesperados.

#### Acceptance Criteria

1. WHEN un animal es marcado como MUERTA o VENDIDA en cattle-inventory, THE Clinical_History_Module SHALL permitir consultar su historial clínico pasado pero prohibir nuevos registros
2. WHEN una vacuna es desactivada en vaccination-management, THE Clinical_History_Module SHALL permitir consultar registros históricos de esa vacuna pero prohibir nuevas vacunaciones con ella
3. WHEN un lote de vacuna vence (fecha_vencimiento < fecha_actual), THE Clinical_History_Module SHALL rechazar nuevas vacunaciones con ese lote retornando HTTP 400 con mensaje "Lote vencido"
4. WHEN se intenta registrar vacunación y el descuento de stock falla por timeout o error de conexión, THE Clinical_History_Module SHALL revertir transacción completa y retornar HTTP 503 con mensaje "Servicio de vacunación temporalmente no disponible"
5. WHEN se calcula proxima_dosis y el resultado es una fecha más de 10 años en el futuro, THE Clinical_History_Module SHALL marcar proxima_dosis como NULL (caso de vacuna con intervalo muy largo o error de datos)
6. WHEN un veterinario es desactivado en user-management, THE Clinical_History_Module SHALL permitir consultar registros históricos creados por ese veterinario pero incluir flag "veterinario_inactivo": true
7. WHEN se consulta historial de un animal con más de 1000 registros, THE Clinical_History_Module SHALL implementar paginación obligatoria y retornar warning en header: X-Warning: "Historial extenso, use paginación"
8. WHEN dosis_aplicada contiene decimales, THE Clinical_History_Module SHALL almacenar con precisión de 2 decimales y redondear hacia arriba (CEILING) en descuento de stock
9. WHEN un animal tiene múltiples vacunaciones con la misma vacuna, THE Clinical_History_Module SHALL calcular proxima_dosis basándose en la última aplicación
10. WHEN se intenta registrar intervención con fecha_aplicacion más de 1 año en el pasado, THE Clinical_History_Module SHALL mostrar warning pero permitir la operación: "Advertencia: Registro retroactivo de más de 1 año"

### Requirement 14: Integración con Módulo User-Management

**User Story:** Como sistema, quiero validar el rol del veterinario y obtener información de usuarios, para que garantice autorización correcta y enriquecimiento de datos.

#### Acceptance Criteria

1. WHEN se registra una intervención clínica, THE Clinical_History_Module SHALL invocar método UserService.getUserById(veterinario_id) para validar que el usuario existe y está activo
2. IF el usuario NO existe o está inactivo, THEN THE Clinical_History_Module SHALL retornar código HTTP 403 con mensaje "Usuario no autorizado"
3. WHEN se obtiene información del usuario, THE Clinical_History_Module SHALL validar que roles contiene VETERINARIO
4. IF el usuario NO tiene rol VETERINARIO, THEN THE Clinical_History_Module SHALL retornar código HTTP 403 con mensaje "Solo veterinarios pueden registrar intervenciones clínicas"
5. WHEN se registra una intervención, THE Clinical_History_Module SHALL almacenar: veterinario_id, veterinario_nombre, veterinario_cedula_profesional (copia desnormalizada para historial inmutable)
6. WHEN se consulta historial clínico, THE Clinical_History_Module SHALL enriquecer respuesta con información actual del veterinario (nombre, cedula_profesional, status)
7. THE Clinical_History_Module SHALL validar que veterinario.tenant_id = tenant_id del contexto antes de permitir operación
8. WHEN se genera reporte de intervenciones por periodo, THE Clinical_History_Module SHALL incluir estadística: intervenciones_por_veterinario[] con nombre y count

### Requirement 15: Notificaciones y Alertas de Próximas Dosis

**User Story:** Como sistema, quiero generar alertas automáticas de próximas dosis vencidas o próximas a vencer, para que los veterinarios puedan planificar vacunaciones oportunamente.

#### Acceptance Criteria

1. THE Clinical_History_Module SHALL exponer endpoint GET /api/v1/clinical-history/alerts/overdue-doses que retorne vacunaciones con proxima_dosis < fecha_actual
2. THE Clinical_History_Module SHALL exponer endpoint GET /api/v1/clinical-history/alerts/upcoming-doses con parámetro days_ahead (default: 7) que retorne vacunaciones con proxima_dosis BETWEEN fecha_actual AND (fecha_actual + days_ahead)
3. WHEN se consulta dosis vencidas, THE Clinical_History_Module SHALL incluir en respuesta: animal_id, animal_identificador, vacuna_nombre, proxima_dosis, dias_vencidos (fecha_actual - proxima_dosis)
4. WHEN se consulta dosis próximas, THE Clinical_History_Module SHALL incluir: animal_id, animal_identificador, vacuna_nombre, proxima_dosis, dias_restantes (proxima_dosis - fecha_actual)
5. THE Clinical_History_Module SHALL ordenar dosis vencidas por dias_vencidos descendente (más urgentes primero)
6. THE Clinical_History_Module SHALL ordenar dosis próximas por dias_restantes ascendente (más cercanas primero)
7. THE Clinical_History_Module SHALL incluir en respuesta contador: total_animales_con_dosis_vencidas, total_animales_con_dosis_proximas
8. THE Clinical_History_Module SHALL aplicar filtro tenant_id en alertas
9. WHEN se consulta alertas con filtro rancho_id, THE Clinical_History_Module SHALL filtrar solo animales de ese rancho consultando cattle-inventory
10. THE Clinical_History_Module SHALL cachear alertas durante 1 hora con key = "alerts:tenant:{tenant_id}:type:{overdue|upcoming}"

## Notes

- Este módulo debe implementarse después de cattle-inventory y vaccination-management ya que depende de ambos
- El historial clínico es INMUTABLE: no se permite actualizar ni eliminar registros una vez creados
- La integración transaccional con vaccination-management es crítica para garantizar consistencia de stock
- El cálculo de próximas dosis es automático y no debe ser modificado manualmente por usuarios
- Solo usuarios con rol VETERINARIO pueden crear registros clínicos
- Implementar soft integration: si vaccination-management está temporalmente no disponible, rechazar vacunaciones pero permitir tratamientos
- La desnormalización de datos (copias de nombre_vacuna, nombre_veterinario) es intencional para preservar historial inmutable
- Los reportes deben ser eficientes incluso con miles de registros por animal
- Considerar futura integración con sistema de notificaciones push/email para alertas de dosis vencidas
- El módulo debe soportar registro retroactivo (fecha_aplicacion en el pasado) con advertencias
- La auditoría debe incluir resultado del descuento de stock para trazabilidad completa
