# Requirements Document

## Introduction

El módulo de control geográfico (geographic-control) proporciona funcionalidades completas para la gestión jerárquica de terrenos ganaderos en Vacapp. El sistema implementa una estructura de tres niveles: Rancho (terreno total), Sección (división opcional para terrenos grandes), y Potrero (unidad mínima de pastoreo donde se alberga el ganado). El módulo soporta tanto configuraciones simples (Rancho → Potreros directamente) como configuraciones jerárquicas (Rancho → Secciones → Potreros). Cada entidad mantiene información de superficie en metros cuadrados y valida que la suma de las partes no exceda el total del contenedor padre.

## Glossary

- **Geographic_Control_Module**: El módulo que gestiona la estructura jerárquica de terrenos (Rancho, Sección, Potrero)
- **Rancho**: Terreno total del usuario, nivel superior de la jerarquía geográfica
- **Seccion**: División opcional del rancho para terrenos grandes, nivel intermedio de la jerarquía
- **Potrero**: Unidad mínima de pastoreo donde se alberga el ganado, nivel inferior de la jerarquía
- **Hierarchical_Structure**: Estructura de árbol donde Rancho contiene Secciones o Potreros, y Secciones contienen Potreros
- **Surface_Validation**: Validación que asegura que la suma de superficies de hijos no excede la superficie del padre
- **Geographic_Repository**: Puerto de salida para acceder y persistir entidades geográficas
- **Simple_Configuration**: Configuración donde Rancho contiene directamente Potreros sin Secciones intermedias
- **Complex_Configuration**: Configuración donde Rancho contiene Secciones, y cada Sección contiene Potreros
- **Tenant_Isolation**: Aislamiento de datos geográficos por tenant_id del usuario autenticado
- **Cattle_Assignment**: Asignación de ganado (animales) a potreros específicos
- **Active_Status**: Estado activo de una entidad geográfica, permitiendo operaciones normales
- **Archived_Status**: Estado archivado de una entidad geográfica, preservando historial sin permitir modificaciones

## Requirements

### Requirement 1: Creación y Gestión de Ranchos

**User Story:** Como usuario de Vacapp, quiero crear y administrar ranchos que representan mis terrenos totales, para que pueda organizar mi operación ganadera de forma estructurada.

#### Acceptance Criteria

1. WHEN un usuario autenticado con rol business (admin, manager, veterinarian, worker) crea un rancho con nombre único y superficie válida, THE Geographic_Control_Module SHALL crear el rancho con status ACTIVE y retornar código HTTP 201 con los datos del rancho creado
2. WHEN un usuario intenta crear un rancho con nombre que ya existe en el mismo tenant (case-insensitive), THE Geographic_Control_Module SHALL retornar código HTTP 409 con mensaje "Ya existe un rancho con ese nombre"
3. THE Geographic_Control_Module SHALL permitir crear ranchos con el mismo nombre en diferentes tenants (WHERE tenant_id difiere)
4. WHEN se proporciona un nombre de rancho con longitud entre 2 y 100 caracteres (inclusive), THE Geographic_Control_Module SHALL aceptar el nombre
5. WHEN se proporciona una superficie de rancho mayor que 0 (cero) y menor o igual a 999,999,999 metros cuadrados, THE Geographic_Control_Module SHALL aceptar la superficie
6. WHEN se proporciona una superficie de rancho menor o igual a 0 o mayor a 999,999,999 metros cuadrados, THE Geographic_Control_Module SHALL retornar código HTTP 400 con mensaje "La superficie debe estar entre 1 y 999,999,999 metros cuadrados"
7. WHEN un nuevo rancho es creado, THE Geographic_Control_Module SHALL registrar created_at (timestamp UTC actual), created_by (user_id del autenticado), tenant_id del contexto, y status como ACTIVE
8. WHEN un usuario lista sus ranchos sin especificar página, THE Geographic_Control_Module SHALL retornar la primera página (page=0) con máximo 50 ranchos filtrados por tenant_id con metadatos de paginación (page, size, total)
9. WHEN un usuario busca un rancho por ID válido dentro de su tenant, THE Geographic_Control_Module SHALL retornar JSON con campos: rancho_id, nombre, superficie_total, superficie_disponible, superficie_usada, status, tenant_id, created_at, updated_at
10. WHEN un usuario actualiza el nombre o superficie de un rancho, THE Geographic_Control_Module SHALL validar que la nueva superficie sea >= superficie_usada por secciones/potreros hijos antes de persistir
11. IF la validación de superficie falla (nueva superficie < superficie_usada), THEN THE Geographic_Control_Module SHALL retornar código HTTP 400 con mensaje "La superficie del rancho debe ser mayor o igual a la superficie usada (X metros cuadrados)"
12. WHEN un usuario archiva un rancho que tiene secciones o potreros activos, THE Geographic_Control_Module SHALL retornar código HTTP 400 con mensaje "No se puede archivar un rancho con secciones o potreros activos"
13. WHEN un usuario archiva un rancho sin hijos activos, THE Geographic_Control_Module SHALL cambiar status a ARCHIVED y retornar código HTTP 200

### Requirement 2: Creación y Gestión de Secciones

**User Story:** Como usuario con un rancho grande, quiero dividir mi rancho en secciones, para que pueda administrar áreas geográficas diferenciadas dentro de mi terreno total.

#### Acceptance Criteria

1. WHEN un usuario crea una sección con nombre único dentro de un rancho y superficie válida, THE Geographic_Control_Module SHALL crear la sección vinculada al rancho con status ACTIVE y retornar código HTTP 201
2. WHEN un usuario intenta crear una sección con nombre que ya existe en el mismo rancho (case-insensitive), THE Geographic_Control_Module SHALL retornar código HTTP 409 con mensaje "Ya existe una sección con ese nombre en este rancho"
3. THE Geographic_Control_Module SHALL permitir crear secciones con el mismo nombre en diferentes ranchos del mismo tenant
4. WHEN se proporciona un rancho_id que no existe o no pertenece al tenant del usuario, THE Geographic_Control_Module SHALL retornar código HTTP 404 con mensaje "Rancho no encontrado"
5. WHEN se proporciona una superficie de sección mayor que 0 y menor o igual a la superficie del rancho padre, THE Geographic_Control_Module SHALL validar que la suma de superficies de todas las secciones del rancho no exceda la superficie_total del rancho
6. IF la validación de suma de superficies falla (suma > rancho.superficie_total), THEN THE Geographic_Control_Module SHALL retornar código HTTP 400 con mensaje "La suma de superficies de secciones excede la superficie total del rancho (disponible: X metros cuadrados)"
7. WHEN un usuario lista las secciones de un rancho, THE Geographic_Control_Module SHALL retornar solo las secciones WHERE rancho_id = :ranchoId AND tenant_id = :currentTenantId
8. WHEN un usuario actualiza la superficie de una sección, THE Geographic_Control_Module SHALL validar que: nueva superficie >= superficie_usada por potreros hijos Y suma de superficies de secciones hermanas + nueva superficie <= rancho.superficie_total
9. WHEN un usuario archiva una sección que tiene potreros activos, THE Geographic_Control_Module SHALL retornar código HTTP 400 con mensaje "No se puede archivar una sección con potreros activos"
10. WHEN un usuario archiva una sección sin potreros activos, THE Geographic_Control_Module SHALL cambiar status a ARCHIVED, actualizar superficie_disponible del rancho padre, y retornar código HTTP 200

### Requirement 3: Creación y Gestión de Potreros

**User Story:** Como usuario, quiero crear potreros dentro de mis ranchos o secciones, para que pueda organizar las áreas específicas donde pastorearán los animales.

#### Acceptance Criteria

1. WHEN un usuario crea un potrero vinculado directamente a un rancho (sin sección), THE Geographic_Control_Module SHALL crear el potrero con rancho_id NOT NULL y seccion_id NULL
2. WHEN un usuario crea un potrero vinculado a una sección, THE Geographic_Control_Module SHALL crear el potrero con rancho_id NOT NULL, seccion_id NOT NULL, y validar que la sección pertenece al rancho especificado
3. WHEN un usuario intenta crear un potrero con nombre que ya existe en el mismo rancho o sección (case-insensitive), THE Geographic_Control_Module SHALL retornar código HTTP 409 con mensaje "Ya existe un potrero con ese nombre en este rancho/sección"
4. WHEN se crea un potrero en un rancho sin secciones, THE Geographic_Control_Module SHALL validar que la suma de superficies de potreros del rancho no exceda rancho.superficie_total
5. WHEN se crea un potrero en una sección, THE Geographic_Control_Module SHALL validar que la suma de superficies de potreros de la sección no exceda seccion.superficie
6. THE Geographic_Control_Module SHALL permitir crear potreros con superficie mayor que 0 y menor o igual a 999,999,999 metros cuadrados
7. WHEN un usuario lista potreros de un rancho, THE Geographic_Control_Module SHALL retornar todos los potreros WHERE rancho_id = :ranchoId AND tenant_id = :currentTenantId ordenados por nombre
8. WHEN un usuario lista potreros de una sección, THE Geographic_Control_Module SHALL retornar solo los potreros WHERE seccion_id = :seccionId AND tenant_id = :currentTenantId
9. WHEN un usuario actualiza la superficie de un potrero, THE Geographic_Control_Module SHALL validar que: nueva superficie >= 0 Y suma de superficies de potreros hermanos + nueva superficie <= superficie del contenedor padre (rancho o sección)
10. WHEN un usuario archiva un potrero que tiene ganado asignado (cattle_count > 0), THE Geographic_Control_Module SHALL retornar código HTTP 400 con mensaje "No se puede archivar un potrero con ganado asignado. Traslade el ganado primero"
11. WHEN un usuario archiva un potrero sin ganado, THE Geographic_Control_Module SHALL cambiar status a ARCHIVED, actualizar superficie_disponible del contenedor padre, y retornar código HTTP 200
12. WHEN un usuario consulta un potrero por ID, THE Geographic_Control_Module SHALL incluir en la respuesta: potrero_id, nombre, superficie, rancho_id, seccion_id (nullable), cattle_count, status, created_at, updated_at

### Requirement 4: Validación de Integridad Jerárquica

**User Story:** Como sistema, quiero validar la integridad de la jerarquía geográfica, para que garantice consistencia de datos y evite configuraciones inválidas.

#### Acceptance Criteria

1. THE Geographic_Control_Module SHALL validar que ∀ rancho r: SUM(superficie de secciones activas) ≤ r.superficie_total
2. THE Geographic_Control_Module SHALL validar que ∀ rancho r sin secciones: SUM(superficie de potreros activos vinculados directamente) ≤ r.superficie_total
3. THE Geographic_Control_Module SHALL validar que ∀ sección s: SUM(superficie de potreros activos de la sección) ≤ s.superficie
4. WHEN una sección s es eliminada o archivada, THE Geographic_Control_Module SHALL validar que NO existan potreros activos WHERE seccion_id = s.id antes de permitir la operación
5. WHEN un rancho r es eliminado o archivado, THE Geographic_Control_Module SHALL validar que NO existan secciones activas NI potreros activos WHERE rancho_id = r.id
6. THE Geographic_Control_Module SHALL calcular automáticamente superficie_disponible como: superficie_total - SUM(superficie de hijos activos)
7. WHEN se actualiza la superficie de cualquier entidad, THE Geographic_Control_Module SHALL recalcular superficie_disponible de todos los ancestros en la jerarquía
8. THE Geographic_Control_Module SHALL prohibir modificar tenant_id de cualquier entidad geográfica después de la creación
9. THE Geographic_Control_Module SHALL validar que seccion_id y rancho_id correspondan al mismo rancho (seccion.rancho_id = potrero.rancho_id) cuando ambos están presentes
10. IF cualquier validación de integridad falla durante una operación, THEN THE Geographic_Control_Module SHALL revertir la transacción completa (rollback) y retornar HTTP 400 con mensaje descriptivo

### Requirement 5: Cálculo de Métricas y Estadísticas

**User Story:** Como usuario, quiero visualizar métricas y estadísticas de mis terrenos, para que pueda tomar decisiones informadas sobre el uso de la tierra.

#### Acceptance Criteria

1. WHEN un usuario consulta un rancho, THE Geographic_Control_Module SHALL calcular y retornar: superficie_total, superficie_usada (suma de hijos activos), superficie_disponible (total - usada), porcentaje_uso ((usada / total) * 100)
2. WHEN un usuario consulta una sección, THE Geographic_Control_Module SHALL calcular y retornar: superficie, superficie_usada_por_potreros, superficie_disponible, porcentaje_uso, count_potreros_activos
3. WHEN un usuario solicita estadísticas de un rancho via GET /api/v1/geography/ranchos/{id}/estadisticas, THE Geographic_Control_Module SHALL retornar JSON con: total_secciones, total_potreros, superficie_total, superficie_usada, superficie_disponible, porcentaje_uso, distribucion_por_seccion (array con nombre, superficie, porcentaje)
4. THE Geographic_Control_Module SHALL calcular porcentaje_uso con precisión de 2 decimales (ejemplo: 65.43%)
5. WHEN no existen secciones ni potreros en un rancho, THE Geographic_Control_Module SHALL retornar porcentaje_uso = 0.00%
6. THE Geographic_Control_Module SHALL considerar solo entidades con status = ACTIVE para cálculos de superficie_usada
7. WHEN un usuario lista ranchos con GET /api/v1/geography/ranchos?includeStats=true, THE Geographic_Control_Module SHALL incluir en cada rancho: superficie_usada, superficie_disponible, porcentaje_uso, total_potreros
8. THE Geographic_Control_Module SHALL cachear estadísticas calculadas durante 5 minutos para mejorar performance en consultas repetidas

### Requirement 6: API REST para Gestión Geográfica

**User Story:** Como desarrollador móvil, quiero consumir una API REST para gestionar la estructura geográfica, para que pueda integrar la funcionalidad en la aplicación móvil.

#### Acceptance Criteria

1. THE Geographic_Control_Module SHALL exponer endpoints REST bajo ruta base /api/v1/geography/ con autenticación JWT obligatoria
2. THE Geographic_Control_Module SHALL implementar endpoints: POST /ranchos, GET /ranchos, GET /ranchos/{id}, PUT /ranchos/{id}, DELETE /ranchos/{id}, GET /ranchos/{id}/estadisticas
3. THE Geographic_Control_Module SHALL implementar endpoints: POST /secciones, GET /secciones, GET /secciones/{id}, PUT /secciones/{id}, DELETE /secciones/{id}, GET /ranchos/{ranchoId}/secciones
4. THE Geographic_Control_Module SHALL implementar endpoints: POST /potreros, GET /potreros, GET /potreros/{id}, PUT /potreros/{id}, DELETE /potreros/{id}, GET /ranchos/{ranchoId}/potreros, GET /secciones/{seccionId}/potreros
5. THE Geographic_Control_Module SHALL aceptar requests con header Content-Type: application/json; charset=UTF-8 y retornar responses con mismo Content-Type
6. WHEN un endpoint retorna colección (lista de ranchos, secciones, potreros), THE Geographic_Control_Module SHALL incluir objeto pagination con campos: page (0-based), size (1-100), total (non-negative integer)
7. WHEN una operación completa exitosamente, THE Geographic_Control_Module SHALL retornar código HTTP: 200 para actualizaciones/lecturas, 201 para creaciones, 204 para archivado exitoso
8. WHEN un Request DTO falla validación Bean Validation, THE Geographic_Control_Module SHALL retornar código HTTP 400 con JSON conteniendo array de errores (field, message)
9. WHEN un JWT_Token es inválido o ausente, THE Geographic_Control_Module SHALL retornar código HTTP 401 con mensaje "Autenticación requerida"
10. WHEN un usuario intenta acceder a recurso de otro tenant, THE Geographic_Control_Module SHALL retornar código HTTP 403 con mensaje "Acceso denegado"
11. WHEN un recurso solicitado no existe (rancho/sección/potrero con ID inexistente), THE Geographic_Control_Module SHALL retornar código HTTP 404 con mensaje "Recurso no encontrado"
12. THE Geographic_Control_Module SHALL documentar todos los endpoints en archivo OpenAPI 3.0 YAML ubicado en src/main/resources/openapi/openapi-geography.yaml

### Requirement 7: Interfaz Web para Visualización Geográfica

**User Story:** Como usuario web, quiero visualizar mi estructura geográfica en una interfaz intuitiva, para que pueda navegar fácilmente entre ranchos, secciones y potreros.

#### Acceptance Criteria

1. THE Geographic_Control_Module SHALL servir página HTML en ruta /geography/ranchos con lista de ranchos en cards mostrando: nombre, superficie_total, porcentaje_uso, total_potreros
2. WHEN un usuario hace clic en un rancho, THE Geographic_Control_Module SHALL navegar a /geography/ranchos/{id} mostrando detalles del rancho con tabs para: Información General, Secciones (si existen), Potreros, Estadísticas
3. THE Geographic_Control_Module SHALL renderizar vista jerárquica en formato de árbol expandible: Rancho > Secciones > Potreros con íconos diferenciados
4. WHEN un usuario crea un nuevo rancho via web, THE Geographic_Control_Module SHALL mostrar formulario modal con campos: nombre (required, 2-100 chars), superficie_total (required, number > 0), descripción (optional, max 500 chars)
5. WHEN validación JavaScript detecta campo inválido, THE Geographic_Control_Module SHALL mostrar mensaje de error inline sin enviar request HTTP
6. THE Geographic_Control_Module SHALL implementar búsqueda en tiempo real de ranchos/potreros usando JavaScript vanilla (sin frameworks) con debounce de 300ms
7. WHEN una petición Fetch API retorna código 4xx o 5xx, THE Geographic_Control_Module SHALL mostrar notificación toast con mensaje de error en español y botón para cerrar
8. THE Geographic_Control_Module SHALL incluir fragmentos Thymeleaf: fragments/geo-card.html (card de rancho), fragments/geo-tree.html (árbol jerárquico), fragments/geo-stats.html (panel de estadísticas)
9. THE Geographic_Control_Module SHALL usar CSS Grid para layout responsivo que se adapte a tablets (mínimo 768px)

### Requirement 8: Auditoría de Cambios Geográficos

**User Story:** Como administrador, quiero un registro completo de cambios en la estructura geográfica, para que pueda rastrear modificaciones y cumplir con auditorías.

#### Acceptance Criteria

1. THE Geographic_Control_Module SHALL registrar en tabla geography_audit cada operación CREATE, UPDATE, ARCHIVE sobre Rancho, Seccion, o Potrero
2. WHEN se crea una entidad geográfica, THE Geographic_Control_Module SHALL insertar en geography_audit: audit_id (UUID), entity_type (RANCHO/SECCION/POTRERO), entity_id (UUID), operation_type (CREATE), timestamp (UTC), modified_by (user_id), tenant_id, old_values (null), new_values (JSON con datos completos)
3. WHEN se actualiza una entidad geográfica, THE Geographic_Control_Module SHALL insertar en geography_audit: operation_type (UPDATE), old_values (JSON con valores anteriores de campos modificados), new_values (JSON con valores nuevos)
4. WHEN se archiva una entidad geográfica, THE Geographic_Control_Module SHALL insertar en geography_audit: operation_type (ARCHIVE), old_values (JSON con status=ACTIVE), new_values (JSON con status=ARCHIVED), reason (string opcional max 500 chars)
5. THE Geographic_Control_Module SHALL incluir en old_values y new_values solo los campos que cambiaron durante UPDATE (no campos no modificados)
6. WHEN un usuario consulta historial de cambios via GET /api/v1/geography/audit con filtros opcionales: entity_type, entity_id, start_date, end_date, modified_by, THE Geographic_Control_Module SHALL retornar filas paginadas (máximo 1000 por página)
7. THE Geographic_Control_Module SHALL retener registros de auditoría por mínimo 730 días (2 años) antes de permitir purga manual por super_admin
8. WHEN se revierte una operación (rollback de transacción), THE Geographic_Control_Module SHALL NO registrar entrada en geography_audit (solo operaciones confirmadas)

### Requirement 9: Validación y Serialización de DTOs

**User Story:** Como desarrollador, quiero que los DTOs sean Records Java con validación automática, para que garantice calidad y consistencia de datos geográficos.

#### Acceptance Criteria

1. THE Geographic_Control_Module SHALL definir Request DTOs como Java Records en infrastructure/controllers/*/dtos/ con anotaciones Bean Validation (@NotNull, @NotBlank, @Positive, @DecimalMin, @DecimalMax)
2. THE Geographic_Control_Module SHALL definir Response DTOs como Java Records sin anotaciones Bean Validation
3. WHEN un Request DTO inválido es recibido, THE Geographic_Control_Module SHALL retornar código HTTP 400 con JSON: {"errors": [{"field": "nombre", "message": "validation message"}]}
4. THE Geographic_Control_Module SHALL mapear entre DTOs y objetos de dominio usando clases XxxMapper con métodos toEntity() y toDomain()
5. FOR ALL Response DTOs conteniendo campos de tipo BigDecimal (superficie, porcentaje_uso), THE Geographic_Control_Module SHALL formatear con máximo 2 decimales y separador de miles
6. FOR ALL Response DTOs con campos LocalDateTime o Instant, THE Geographic_Control_Module SHALL formatear como String en formato ISO 8601: yyyy-MM-dd'T'HH:mm:ss'Z' con timezone UTC
7. THE Geographic_Control_Module SHALL validar que nombre de rancho/sección/potrero no contenga solo espacios en blanco (trim antes de validar)
8. WHEN validación de superficie falla (valor negativo o cero), THE Geographic_Control_Module SHALL incluir en errors: {"field": "superficie", "message": "La superficie debe ser mayor que 0"}

### Requirement 10: Multi-tenancy y Seguridad

**User Story:** Como sistema multitenant, quiero que todos los datos geográficos estén aislados por tenant, para que cada organización solo acceda a sus propios ranchos y potreros.

#### Acceptance Criteria

1. THE Geographic_Control_Module SHALL aplicar filtro WHERE tenant_id = :currentTenantId automáticamente en todas las operaciones de lectura sobre Rancho, Seccion, y Potrero
2. WHEN un usuario con Business_Role ejecuta cualquier operación geográfica, THE Geographic_Control_Module SHALL extraer tenant_id del JWT_Token y aplicar filtro en queries
3. WHEN un usuario SaaS (super_admin o support) ejecuta operación de lectura sin especificar tenant_id en query params, THE Geographic_Control_Module SHALL omitir filtro y retornar datos de todos los tenants
4. THE Geographic_Control_Module SHALL validar en cada creación que tenant_id del objeto coincida con tenant_id del JWT_Token del usuario autenticado
5. WHEN un usuario intenta crear rancho/sección/potrero especificando tenant_id diferente al de su JWT, THE Geographic_Control_Module SHALL retornar código HTTP 403 con mensaje "No autorizado para modificar datos de otro tenant"
6. THE Geographic_Control_Module SHALL aplicar filtro tenant_id en operaciones de conteo (COUNT), agregación (SUM/AVG de superficie), y paginación
7. THE Geographic_Control_Module SHALL validar que sección.tenant_id = rancho.tenant_id durante creación de sección
8. THE Geographic_Control_Module SHALL validar que potrero.tenant_id = rancho.tenant_id = seccion.tenant_id (si existe sección) durante creación de potrero

### Requirement 11: Performance y Optimización

**User Story:** Como sistema, quiero optimizar consultas y cálculos geográficos, para que garantice tiempos de respuesta aceptables incluso con grandes volúmenes de datos.

#### Acceptance Criteria

1. THE Geographic_Control_Module SHALL crear índices en columnas: tenant_id, rancho_id, seccion_id, status en las tres tablas (ranchos, secciones, potreros)
2. THE Geographic_Control_Module SHALL utilizar queries JOIN optimizadas al listar potreros de un rancho incluyendo información de sección padre
3. WHEN un usuario consulta estadísticas de rancho, THE Geographic_Control_Module SHALL ejecutar máximo 3 queries SQL (1 para rancho, 1 para secciones, 1 para potreros) usando proyección de solo campos necesarios
4. THE Geographic_Control_Module SHALL cachear resultados de estadísticas durante 5 minutos usando @Cacheable con key = "stats:rancho:{id}:tenant:{tenantId}"
5. WHEN se modifica superficie de rancho/sección/potrero, THE Geographic_Control_Module SHALL invalidar cache de estadísticas del rancho afectado usando @CacheEvict
6. THE Geographic_Control_Module SHALL implementar paginación usando LIMIT y OFFSET con máximo 100 registros por página para prevenir queries lentas
7. WHEN se lista ranchos con includeStats=true, THE Geographic_Control_Module SHALL usar query batch (N+1 problem avoided) para obtener estadísticas de todos los ranchos en máximo 2 queries
8. THE Geographic_Control_Module SHALL usar @Transactional(readOnly = true) en operaciones de solo lectura para optimizar conexiones de base de datos

### Requirement 12: Manejo de Casos Especiales

**User Story:** Como sistema, quiero manejar correctamente casos especiales y edge cases, para que prevenga estados inconsistentes y errores inesperados.

#### Acceptance Criteria

1. WHEN un rancho tiene superficie_total = 1000 y usuario crea potreros con suma exacta = 1000, THE Geographic_Control_Module SHALL aceptar la operación y marcar superficie_disponible = 0
2. WHEN superficie_disponible = 0, THE Geographic_Control_Module SHALL permitir actualizaciones que NO aumenten superficie_usada (ejemplo: renombrar entidad, cambiar descripción)
3. WHEN un usuario intenta eliminar (DELETE) un rancho/sección/potrero, THE Geographic_Control_Module SHALL retornar HTTP 405 con mensaje "Use operación de archivado en su lugar" (deletes físicos no permitidos)
4. THE Geographic_Control_Module SHALL permitir des-archivar (reactivar) una entidad archivada si: el contenedor padre está activo Y hay superficie_disponible suficiente
5. WHEN un usuario des-archiva una sección o potrero, THE Geographic_Control_Module SHALL actualizar superficie_disponible del padre disminuyendo por la superficie del hijo reactivado
6. WHEN superficie_total o superficie de una entidad contiene decimales, THE Geographic_Control_Module SHALL almacenar con precisión de 2 decimales y redondear hacia arriba (CEILING) en cálculos de porcentaje_uso
7. WHEN un rancho no tiene secciones (configuración simple), THE Geographic_Control_Module SHALL permitir listar potreros con GET /ranchos/{id}/potreros retornando solo potreros con seccion_id = NULL
8. WHEN un rancho tiene secciones (configuración compleja), THE Geographic_Control_Module SHALL prohibir crear potreros directamente vinculados al rancho (sin sección) retornando HTTP 400 con mensaje "Este rancho utiliza secciones. Cree el potrero dentro de una sección"
9. WHEN un usuario consulta potrero que tiene ganado asignado (cattle_count > 0), THE Geographic_Control_Module SHALL incluir en respuesta warning: "Este potrero tiene ganado asignado. Traslade el ganado antes de archivar"
10. THE Geographic_Control_Module SHALL validar que nombres de rancho/sección/potrero no contengan caracteres especiales peligrosos (SQL injection, XSS) rechazando con HTTP 400 si detecta patrones: <script>, DROP, INSERT, UPDATE seguidos de espacios

## Notes

- Este módulo debe implementarse después del módulo user-management ya que depende de autenticación y tenant_id
- La jerarquía geográfica es fundamental para otros módulos funcionales (gestión de ganado, producción, eventos)
- La validación de superficie es crítica: la suma de las partes nunca debe exceder el todo
- Considerar futura integración con mapas geográficos (Google Maps, OpenStreetMap) para visualización
- La tabla geography_audit debe ser monitoreada regularmente para evitar crecimiento excesivo
- Implementar soft deletes (archivado) en lugar de deletes físicos para preservar historial
- El cálculo de superficie_disponible debe ser consistente en toda la jerarquía
- Considerar límite máximo de niveles de profundidad (actualmente 3: rancho → sección → potrero)
