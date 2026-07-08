# Requirements Document

## Introduction

El módulo de gestión de vacunas (vaccination-management) es un componente crítico de Vacapp que gestiona el inventario completo de vacunas, su aplicación a los animales, control de stock automático, cálculo de próximas dosis de refuerzo, y trazabilidad completa de vacunación por animal. Este módulo se integra estrechamente con cattle-inventory para registrar aplicaciones de vacunas, y con user-management para validar veterinarios autorizados. Incluye alertas inteligentes de stock bajo y vencimientos próximos, reportes de consumo por periodo, y control de costos de vacunación.

## Glossary

- **Vaccination_Management_Module**: El módulo que gestiona inventario de vacunas y su aplicación
- **Vaccine**: Entidad principal que representa un producto biológico para inmunización
- **Vaccine_Category**: Categoría funcional de la vacuna (Reproductiva, Respiratoria, Digestiva, Clostridiales, Parasitarias)
- **Vaccine_Type**: Tipo biológico de la vacuna (Virus Vivo, Virus Muerto, Bacterina, Toxoide, Recombinante)
- **Administration_Route**: Vía de administración (Intramuscular, Subcutánea, Intranasal, Oral, Intravenosa)
- **Vaccine_Lot**: Lote específico de una vacuna con fecha de vencimiento y stock particular
- **Vaccination_Application**: Registro de aplicación de una vacuna a un animal específico
- **Next_Dose_Calculator**: Componente que calcula fecha de próxima dosis basada en intervalo de refuerzo
- **Stock_Alert**: Alerta automática cuando stock cae por debajo del umbral mínimo
- **Expiration_Alert**: Alerta automática cuando una vacuna está próxima a vencer
- **Vaccination_History**: Historial completo de vacunaciones de un animal
- **Consumption_Report**: Reporte de consumo de vacunas por periodo
- **Cost_Report**: Reporte de costos de vacunación
- **Vaccination_Coverage**: Porcentaje de animales vacunados con determinada vacuna
- **Multi_Tenant_Isolation**: Aislamiento de datos por tenant_id y rancho_id

## Requirements

### Requirement 1: Gestión de Categorías de Vacunas

**User Story:** Como administrador del sistema, quiero gestionar catálogos de categorías de vacunas con CRUD completo, para que cree, modifique y organice categorías personalizadas según las necesidades del rancho.

#### Acceptance Criteria

1. WHEN un usuario crea una categoría via POST /api/v1/vaccines/categories, THE Vaccination_Management_Module SHALL crear registro con campos: id_categoria (UUID), nombre (VARCHAR 50 NOT NULL UNIQUE), descripcion (TEXT), rancho_id (UUID NOT NULL), tenant_id (UUID NOT NULL), activo (BOOLEAN DEFAULT TRUE), created_at, created_by
2. WHEN se proporciona nombre vacío o solo espacios, THE Vaccination_Management_Module SHALL retornar HTTP 400 con mensaje "Nombre de categoría es requerido"
3. WHEN se proporciona nombre duplicado para el mismo tenant/rancho, THE Vaccination_Management_Module SHALL retornar HTTP 409 con mensaje "Ya existe una categoría con ese nombre"
4. WHEN un usuario consulta categorías via GET /api/v1/vaccines/categories, THE Vaccination_Management_Module SHALL retornar array filtrado por tenant_id y rancho_id con campos: id_categoria, nombre, descripcion, activo, fecha_creacion
5. WHEN un usuario actualiza una categoría via PUT /api/v1/vaccines/categories/{id}, THE Vaccination_Management_Module SHALL validar que pertenece a su tenant/rancho y actualizar nombre y descripcion
6. WHEN un usuario archiva una categoría via DELETE /api/v1/vaccines/categories/{id}, THE Vaccination_Management_Module SHALL cambiar activo = FALSE sin eliminar físicamente
7. THE Vaccination_Management_Module SHALL validar que categoría pertenece al tenant/rancho antes de archivarla
8. WHEN se archiva una categoría, THE Vaccination_Management_Module SHALL validar que no existan vacunas activas con esa categoría, retornar HTTP 409 si existen
9. WHEN se crea una vacuna, THE Vaccination_Management_Module SHALL validar que id_categoria existe, pertenece al mismo tenant/rancho, y está activo, retornar HTTP 400 si es inválido
10. THE Vaccination_Management_Module SHALL almacenar categorías en tabla vaccine_categories con índice en (tenant_id, rancho_id, nombre) para búsquedas rápidas

### Requirement 2: Gestión de Tipos de Vacunas

**User Story:** Como administrador, quiero gestionar tipos biológicos de vacunas con CRUD completo, para que cree tipos personalizados según la composición de las vacunas que utilizo.

#### Acceptance Criteria

1. WHEN un usuario crea un tipo via POST /api/v1/vaccines/types, THE Vaccination_Management_Module SHALL crear registro con campos: id_tipo_vacuna (UUID), nombre (VARCHAR 50 NOT NULL UNIQUE), descripcion (TEXT), rancho_id (UUID NOT NULL), tenant_id (UUID NOT NULL), activo (BOOLEAN DEFAULT TRUE), created_at, created_by
2. WHEN se proporciona nombre vacío o duplicado, THE Vaccination_Management_Module SHALL retornar HTTP 400/409 respectivamente
3. WHEN un usuario consulta tipos via GET /api/v1/vaccines/types, THE Vaccination_Management_Module SHALL retornar array filtrado por tenant_id y rancho_id
4. WHEN un usuario actualiza un tipo via PUT /api/v1/vaccines/types/{id}, THE Vaccination_Management_Module SHALL validar pertenencia a tenant/rancho
5. WHEN un usuario archiva un tipo via DELETE /api/v1/vaccines/types/{id}, THE Vaccination_Management_Module SHALL validar que no existan vacunas activas con ese tipo
6. WHEN se crea una vacuna, THE Vaccination_Management_Module SHALL validar que id_tipo_vacuna existe, pertenece al tenant/rancho, y está activo

### Requirement 3: Gestión de Vías de Administración

**User Story:** Como administrador, quiero gestionar vías de administración de vacunas con CRUD completo, para que cree y personalice los métodos de aplicación según mis necesidades.

#### Acceptance Criteria

1. WHEN un usuario crea una vía via POST /api/v1/vaccines/routes, THE Vaccination_Management_Module SHALL crear registro con campos: id_via_administracion (UUID), nombre (VARCHAR 50 NOT NULL UNIQUE), abreviatura (VARCHAR 10), descripcion (TEXT), rancho_id (UUID NOT NULL), tenant_id (UUID NOT NULL), activo (BOOLEAN DEFAULT TRUE), created_at, created_by
2. WHEN se proporciona nombre vacío o duplicado, THE Vaccination_Management_Module SHALL retornar HTTP 400/409 respectivamente
3. WHEN un usuario consulta vías via GET /api/v1/vaccines/routes, THE Vaccination_Management_Module SHALL retornar array filtrado por tenant_id y rancho_id con campos: id_via_administracion, nombre, abreviatura, descripcion, activo
4. WHEN un usuario actualiza una vía via PUT /api/v1/vaccines/routes/{id}, THE Vaccination_Management_Module SHALL validar pertenencia a tenant/rancho y actualizar campos
5. WHEN un usuario archiva una vía via DELETE /api/v1/vaccines/routes/{id}, THE Vaccination_Management_Module SHALL validar que no existan vacunas activas con esa vía
6. WHEN se crea una vacuna, THE Vaccination_Management_Module SHALL validar que id_via_administracion existe, pertenece al tenant/rancho, y está activo

### Requirement 4: Registro de Vacunas en Inventario

**User Story:** Como usuario de Vacapp, quiero registrar vacunas en el inventario con toda su información comercial y técnica, para que tenga control del inventario de biológicos.

#### Acceptance Criteria

1. WHEN un usuario registra una nueva vacuna, THE Vaccination_Management_Module SHALL crear registro con campos: id_vacuna (UUID), nombre_comercial (VARCHAR 200 NOT NULL), id_categoria (UUID FK NOT NULL), id_tipo_vacuna (UUID FK NOT NULL), id_via_administracion (UUID FK NOT NULL), laboratorio (VARCHAR 100 NOT NULL), dosis_recomendada (VARCHAR 50), intervalo_refuerzo_dias (INT), stock_minimo (INT DEFAULT 10), rancho_id (UUID FK NOT NULL), tenant_id (UUID NOT NULL), activo (BOOLEAN DEFAULT TRUE), created_at, created_by
2. WHEN se proporciona nombre_comercial vacío o solo espacios, THE Vaccination_Management_Module SHALL retornar HTTP 400 con mensaje "Nombre comercial es requerido"
3. WHEN se proporciona laboratorio vacío, THE Vaccination_Management_Module SHALL retornar HTTP 400 con mensaje "Laboratorio es requerido"
4. WHEN se proporciona intervalo_refuerzo_dias negativo, THE Vaccination_Management_Module SHALL retornar HTTP 400 con mensaje "Intervalo de refuerzo debe ser positivo"
5. THE Vaccination_Management_Module SHALL permitir intervalo_refuerzo_dias NULL para vacunas de dosis única (sin refuerzo)
6. THE Vaccination_Management_Module SHALL filtrar vacunas por tenant_id y rancho_id en todas las consultas
7. WHEN un usuario lista vacunas via GET /api/v1/vaccines, THE Vaccination_Management_Module SHALL retornar paginación con vacunas WHERE tenant_id = :currentTenantId AND rancho_id = :currentRanchoId AND activo = TRUE
8. WHEN un usuario consulta una vacuna específica via GET /api/v1/vaccines/{id}, THE Vaccination_Management_Module SHALL incluir: datos básicos, stock_total (SUM de stock_disponible de todos los lotes), lotes_activos (COUNT lotes WHERE stock > 0), categoria_nombre, tipo_nombre, via_nombre
9. THE Vaccination_Management_Module SHALL permitir búsqueda de vacunas por nombre_comercial con LIKE case-insensitive
10. WHEN un usuario archiva una vacuna, THE Vaccination_Management_Module SHALL cambiar activo = FALSE sin eliminar físicamente

### Requirement 5: Gestión de Lotes de Vacunas

**User Story:** Como usuario, quiero registrar múltiples lotes de la misma vacuna con diferentes fechas de vencimiento y stocks, para que tenga control por lote del inventario.

#### Acceptance Criteria

1. THE Vaccination_Management_Module SHALL permitir registrar múltiples lotes por vacuna en tabla vaccine_lots con campos: id_lote (UUID PK), id_vacuna (UUID FK NOT NULL), numero_lote (VARCHAR 50 NOT NULL), fecha_vencimiento (DATE NOT NULL), stock_disponible (INT NOT NULL DEFAULT 0), precio_costo (DECIMAL 10,2), fecha_ingreso (DATE NOT NULL), created_at, created_by
2. WHEN se proporciona fecha_vencimiento anterior a fecha actual, THE Vaccination_Management_Module SHALL retornar HTTP 400 con mensaje "Fecha de vencimiento no puede estar en el pasado"
3. WHEN se proporciona stock_disponible negativo, THE Vaccination_Management_Module SHALL retornar HTTP 400 con mensaje "Stock no puede ser negativo"
4. WHEN se proporciona precio_costo negativo, THE Vaccination_Management_Module SHALL retornar HTTP 400 con mensaje "Precio no puede ser negativo"
5. THE Vaccination_Management_Module SHALL calcular stock_total de una vacuna como: SELECT SUM(stock_disponible) FROM vaccine_lots WHERE id_vacuna = :id AND fecha_vencimiento >= CURRENT_DATE
6. WHEN un usuario consulta lotes de una vacuna via GET /api/v1/vaccines/{id}/lots, THE Vaccination_Management_Module SHALL retornar array ordenado por fecha_vencimiento ASC con campos: id_lote, numero_lote, fecha_vencimiento, stock_disponible, precio_costo, dias_para_vencer (DATEDIFF(fecha_vencimiento, CURRENT_DATE))
7. THE Vaccination_Management_Module SHALL permitir ajustar stock de un lote via PUT /api/v1/vaccine-lots/{id}/adjust-stock con motivo (entrada/salida manual, corrección inventario, merma)
8. WHEN se ajusta stock, THE Vaccination_Management_Module SHALL registrar en tabla stock_adjustments: id_ajuste, id_lote, cantidad_anterior, cantidad_nueva, motivo, created_at, created_by
9. THE Vaccination_Management_Module SHALL marcar lotes vencidos ejecutando UPDATE vaccine_lots SET stock_disponible = 0 WHERE fecha_vencimiento < CURRENT_DATE - 1 DAY (job nocturno)

### Requirement 6: Aplicación de Vacunas a Animales

**User Story:** Como usuario, quiero registrar la aplicación de una vacuna a un animal, para que tenga historial de vacunación y control de stock automático.

#### Acceptance Criteria

1. WHEN un usuario aplica una vacuna via POST /api/v1/vaccinations, THE Vaccination_Management_Module SHALL crear registro en tabla vaccination_applications con campos: id_aplicacion (UUID PK), id_animal (UUID FK NOT NULL), id_vacuna (UUID FK NOT NULL), id_lote (UUID FK NOT NULL), fecha_aplicacion (DATE NOT NULL), dosis_aplicada (VARCHAR 50), id_veterinario (UUID FK), proxima_dosis_fecha (DATE calculada automáticamente), observaciones (TEXT), costo_aplicacion (DECIMAL 10,2), rancho_id (UUID FK NOT NULL), tenant_id (UUID NOT NULL), created_at, created_by
2. WHEN se proporciona id_animal que no existe o no pertenece al tenant/rancho, THE Vaccination_Management_Module SHALL retornar HTTP 400 con mensaje "Animal no encontrado"
3. THE Vaccination_Management_Module SHALL validar que id_animal está activo consultando CattleService.isAnimalActive(id_animal), retornar HTTP 400 con mensaje "No se puede vacunar animal vendido o muerto" si es falso
4. WHEN se proporciona id_lote sin stock disponible (stock_disponible = 0), THE Vaccination_Management_Module SHALL retornar HTTP 409 con mensaje "Lote sin stock disponible"
5. WHEN se proporciona id_lote con fecha_vencimiento < fecha_aplicacion, THE Vaccination_Management_Module SHALL retornar HTTP 400 con mensaje "No se puede aplicar vacuna vencida"
6. WHEN se proporciona fecha_aplicacion futura, THE Vaccination_Management_Module SHALL retornar HTTP 400 con mensaje "Fecha de aplicación no puede ser futura"
7. WHEN se proporciona id_veterinario que no existe o no tiene rol VETERINARIO, THE Vaccination_Management_Module SHALL retornar HTTP 400 con mensaje "Veterinario no válido"
8. WHEN se registra aplicación exitosamente, THE Vaccination_Management_Module SHALL decrementar stock: UPDATE vaccine_lots SET stock_disponible = stock_disponible - 1 WHERE id_lote = :id en transacción atómica
9. IF la vacuna tiene intervalo_refuerzo_dias NOT NULL, THEN THE Next_Dose_Calculator SHALL calcular proxima_dosis_fecha = fecha_aplicacion + intervalo_refuerzo_dias y almacenar en vaccination_applications
10. IF la vacuna tiene intervalo_refuerzo_dias NULL, THEN proxima_dosis_fecha SHALL ser NULL (vacuna de dosis única)
11. THE Vaccination_Management_Module SHALL registrar en cattle audit log: operation_type (VACCINATION_APPLIED), animal_id, vaccine_id, fecha_aplicacion, veterinario_id
12. WHEN stock después de decrementar cae a <= stock_minimo, THE Vaccination_Management_Module SHALL crear Stock_Alert automáticamente
13. THE Vaccination_Management_Module SHALL retornar HTTP 201 con VaccinationApplicationResponse conteniendo: id_aplicacion, animal_arete, vaccine_nombre, fecha_aplicacion, proxima_dosis_fecha, stock_restante

### Requirement 7: Historial de Vacunación por Animal

**User Story:** Como usuario, quiero consultar el historial completo de vacunación de un animal, para que tenga trazabilidad de todas las vacunas aplicadas.

#### Acceptance Criteria

1. WHEN un usuario consulta historial via GET /api/v1/animals/{id}/vaccinations, THE Vaccination_Management_Module SHALL validar que animal pertenece a su tenant/rancho
2. THE Vaccination_Management_Module SHALL retornar array ordenado por fecha_aplicacion DESC con campos: id_aplicacion, vaccine_nombre_comercial, categoria_nombre, fecha_aplicacion, dosis_aplicada, lote_numero, veterinario_nombre, proxima_dosis_fecha, dias_hasta_proxima_dosis (DATEDIFF(proxima_dosis_fecha, CURRENT_DATE) o NULL)
3. WHEN un usuario filtra historial por categoria via GET /api/v1/animals/{id}/vaccinations?categoria={categoria}, THE Vaccination_Management_Module SHALL retornar solo vacunaciones WHERE id_categoria = :categoria
4. THE Vaccination_Management_Module SHALL marcar visualmente vacunaciones donde proxima_dosis_fecha <= CURRENT_DATE + 7 DAYS (próximas a vencer)
5. THE Vaccination_Management_Module SHALL incluir metadatos: total_vacunaciones, ultima_vacunacion_fecha, proximas_dosis (array de vacunas con refuerzo pendiente)

### Requirement 8: Próximas Vacunaciones Pendientes

**User Story:** Como usuario, quiero ver qué animales necesitan vacunación próximamente, para que planifique campañas de vacunación.

#### Acceptance Criteria

1. WHEN un usuario consulta próximas vacunaciones via GET /api/v1/vaccinations/upcoming, THE Vaccination_Management_Module SHALL retornar array de animales con vacunas pendientes WHERE proxima_dosis_fecha BETWEEN CURRENT_DATE AND CURRENT_DATE + :dias (default 30 días)
2. THE Vaccination_Management_Module SHALL agrupar por vacuna mostrando: vaccine_nombre, cantidad_animales_pendientes, array de animales (id_animal, arete, proxima_dosis_fecha)
3. WHEN un usuario filtra por vacuna específica via GET /api/v1/vaccinations/upcoming?vaccine_id={id}, THE Vaccination_Management_Module SHALL retornar solo animales con esa vacuna pendiente
4. THE Vaccination_Management_Module SHALL filtrar por tenant_id y rancho_id
5. WHEN un usuario consulta animales sin vacunar via GET /api/v1/vaccinations/unvaccinated?vaccine_id={id}, THE Vaccination_Management_Module SHALL retornar animales WHERE NOT EXISTS (SELECT 1 FROM vaccination_applications WHERE id_animal = animals.animal_id AND id_vacuna = :id) AND status IN (Activa, Preñada, En Reposo)

### Requirement 9: Alertas de Stock Bajo

**User Story:** Como usuario, quiero recibir alertas automáticas cuando el stock de una vacuna esté bajo, para que reponga inventario a tiempo.

#### Acceptance Criteria

1. WHEN stock total de una vacuna (SUM de todos sus lotes) <= stock_minimo, THE Vaccination_Management_Module SHALL crear registro en tabla stock_alerts con campos: id_alerta (UUID PK), id_vacuna (UUID FK NOT NULL), stock_actual (INT), stock_minimo (INT), fecha_alerta (TIMESTAMP NOT NULL), resuelta (BOOLEAN DEFAULT FALSE), fecha_resolucion (TIMESTAMP), tenant_id, rancho_id
2. WHEN un usuario consulta alertas activas via GET /api/v1/alerts/stock, THE Vaccination_Management_Module SHALL retornar array WHERE resuelta = FALSE filtrado por tenant_id y rancho_id ordenado por fecha_alerta DESC
3. THE Vaccination_Management_Module SHALL incluir en alerta: vaccine_nombre_comercial, laboratorio, stock_actual, stock_minimo, dias_desde_alerta (DATEDIFF(CURRENT_DATE, fecha_alerta))
4. WHEN stock se incrementa por encima de stock_minimo, THE Vaccination_Management_Module SHALL marcar alerta como resuelta: UPDATE stock_alerts SET resuelta = TRUE, fecha_resolucion = CURRENT_TIMESTAMP WHERE id_vacuna = :id AND resuelta = FALSE
5. THE Vaccination_Management_Module SHALL enviar notificación push/email a usuarios con rol ADMIN o VETERINARIO cuando se crea Stock_Alert (integración futura)

### Requirement 10: Alertas de Vencimiento Próximo

**User Story:** Como usuario, quiero recibir alertas cuando vacunas estén próximas a vencer, para que use el stock antes de que caduque.

#### Acceptance Criteria

1. THE Vaccination_Management_Module SHALL ejecutar job diario que identifica lotes WHERE fecha_vencimiento BETWEEN CURRENT_DATE AND CURRENT_DATE + 30 DAYS AND stock_disponible > 0
2. WHEN se detecta lote próximo a vencer, THE Vaccination_Management_Module SHALL crear registro en tabla expiration_alerts con campos: id_alerta (UUID PK), id_lote (UUID FK NOT NULL), fecha_vencimiento (DATE), dias_para_vencer (INT), stock_en_riesgo (INT), fecha_alerta (TIMESTAMP), tenant_id, rancho_id
3. WHEN un usuario consulta alertas de vencimiento via GET /api/v1/alerts/expiration, THE Vaccination_Management_Module SHALL retornar array ordenado por dias_para_vencer ASC con campos: vaccine_nombre_comercial, lote_numero, fecha_vencimiento, dias_para_vencer, stock_en_riesgo, laboratorio
4. THE Vaccination_Management_Module SHALL clasificar alertas en tres niveles: CRITICO (≤7 días), IMPORTANTE (8-15 días), INFORMATIVO (16-30 días)
5. WHEN un lote vence (fecha_vencimiento < CURRENT_DATE), THE Vaccination_Management_Module SHALL marcar stock_disponible = 0 y eliminar alerta

### Requirement 11: Reportes de Consumo de Vacunas

**User Story:** Como usuario, quiero generar reportes de consumo de vacunas por periodo, para que analice el uso del inventario.

#### Acceptance Criteria

1. WHEN un usuario genera reporte via GET /api/v1/reports/vaccine-consumption?fecha_inicio={fecha}&fecha_fin={fecha}, THE Vaccination_Management_Module SHALL retornar array agrupado por vacuna con campos: vaccine_id, vaccine_nombre_comercial, categoria_nombre, total_dosis_aplicadas (COUNT), animales_vacunados (COUNT DISTINCT id_animal), lotes_utilizados (COUNT DISTINCT id_lote), costo_total (SUM costo_aplicacion)
2. THE Vaccination_Management_Module SHALL permitir filtrar por categoria via GET /api/v1/reports/vaccine-consumption?categoria={id}
3. THE Vaccination_Management_Module SHALL permitir filtrar por vacuna específica via GET /api/v1/reports/vaccine-consumption?vaccine_id={id}
4. THE Vaccination_Management_Module SHALL calcular vaccination_coverage como: (animales_vacunados / total_animales_activos) * 100
5. THE Vaccination_Management_Module SHALL incluir gráfica de tendencia temporal: array de {fecha, cantidad_aplicaciones} agrupado por día/semana/mes según rango de fechas
6. THE Vaccination_Management_Module SHALL filtrar por tenant_id y rancho_id

### Requirement 12: Reportes de Costos de Vacunación

**User Story:** Como administrador, quiero reportes de costos de vacunación, para que controle gastos en salud animal.

#### Acceptance Criteria

1. WHEN un usuario genera reporte de costos via GET /api/v1/reports/vaccination-costs?fecha_inicio={fecha}&fecha_fin={fecha}, THE Vaccination_Management_Module SHALL retornar resumen con: costo_total (SUM costo_aplicacion), costo_promedio_por_animal, total_aplicaciones, costo_por_categoria (array), costo_por_veterinario (array)
2. THE Vaccination_Management_Module SHALL calcular costo_promedio_por_animal como: costo_total / COUNT(DISTINCT id_animal)
3. THE Vaccination_Management_Module SHALL incluir top 5 vacunas más costosas ordenadas por costo_total DESC
4. THE Vaccination_Management_Module SHALL permitir comparar costos entre dos periodos mostrando variación porcentual
5. THE Vaccination_Management_Module SHALL exportar reporte en formato PDF y Excel (integración futura)

### Requirement 13: API REST para Gestión de Vacunas

**User Story:** Como desarrollador móvil, quiero consumir API REST para gestión de vacunas, para que integre la funcionalidad en la aplicación móvil.

#### Acceptance Criteria

1. THE Vaccination_Management_Module SHALL exponer endpoints REST bajo ruta base /api/v1/vaccines/ y /api/v1/vaccinations/ con autenticación JWT obligatoria
2. THE Vaccination_Management_Module SHALL implementar endpoints: POST /vaccines/categories, GET /vaccines/categories, PUT /vaccines/categories/{id}, DELETE /vaccines/categories/{id} (soft delete), POST /vaccines/types, GET /vaccines/types, PUT /vaccines/types/{id}, DELETE /vaccines/types/{id}, POST /vaccines/routes, GET /vaccines/routes, PUT /vaccines/routes/{id}, DELETE /vaccines/routes/{id}, POST /vaccines, GET /vaccines, GET /vaccines/{id}, PUT /vaccines/{id}, DELETE /vaccines/{id} (soft delete), GET /vaccines/{id}/lots, POST /vaccine-lots, PUT /vaccine-lots/{id}/adjust-stock, POST /vaccinations, GET /vaccinations, GET /animals/{id}/vaccinations, GET /vaccinations/upcoming, GET /vaccinations/unvaccinated, GET /alerts/stock, GET /alerts/expiration, GET /reports/vaccine-consumption, GET /reports/vaccination-costs
3. THE Vaccination_Management_Module SHALL aceptar requests con Content-Type: application/json; charset=UTF-8
4. WHEN endpoint retorna colección, THE Vaccination_Management_Module SHALL incluir pagination con campos: page, size, total
5. WHEN operación completa exitosamente, THE Vaccination_Management_Module SHALL retornar: 200 para lecturas/actualizaciones, 201 para creaciones, 204 para archivado
6. WHEN Request DTO falla validación, THE Vaccination_Management_Module SHALL retornar HTTP 400 con array de errores
7. WHEN JWT inválido o ausente, THE Vaccination_Management_Module SHALL retornar HTTP 401
8. WHEN usuario intenta acceder a recurso de otro tenant, THE Vaccination_Management_Module SHALL retornar HTTP 403
9. WHEN recurso no existe, THE Vaccination_Management_Module SHALL retornar HTTP 404
10. THE Vaccination_Management_Module SHALL documentar todos los endpoints en OpenAPI 3.0 YAML en src/main/resources/openapi/openapi-vaccination.yaml

### Requirement 14: Interfaz Web para Gestión de Vacunas

**User Story:** Como usuario web, quiero gestionar inventario de vacunas en interfaz intuitiva, para que administre fácilmente desde navegador.

#### Acceptance Criteria

1. THE Vaccination_Management_Module SHALL servir página HTML en /vaccines con lista de vacunas mostrando: nombre_comercial, laboratorio, categoria, stock_total, lotes_activos, alertas activas (badge)
2. WHEN usuario hace clic en vacuna, THE Vaccination_Management_Module SHALL navegar a /vaccines/{id} con tabs: Información General, Lotes, Historial de Aplicaciones, Estadísticas
3. THE Vaccination_Management_Module SHALL renderizar formulario para aplicar vacuna con campos: seleccionar_animal (autocomplete), seleccionar_lote (dropdown), fecha_aplicacion (date picker), dosis_aplicada, veterinario (dropdown), observaciones
4. WHEN usuario crea nueva vacuna, THE Vaccination_Management_Module SHALL mostrar modal con campos: nombre_comercial, categoria (select), tipo (select), via_administracion (select), laboratorio, dosis_recomendada, intervalo_refuerzo_dias, stock_minimo
5. THE Vaccination_Management_Module SHALL implementar búsqueda en tiempo real de vacunas con debounce 300ms
6. THE Vaccination_Management_Module SHALL mostrar dashboard con KPIs: total_vacunas_activas, stock_total, alertas_activas, aplicaciones_ultimo_mes
7. THE Vaccination_Management_Module SHALL mostrar notificaciones badge con count de alertas sin resolver

### Requirement 15: Integración con Módulo de Ganado

**User Story:** Como sistema integrado, quiero que vacunación se sincronice con inventario de ganado, para que garantice consistencia.

#### Acceptance Criteria

1. WHEN se aplica vacuna a animal, THE Vaccination_Management_Module SHALL invocar CattleService.isAnimalActive(id_animal) para validar estado
2. WHEN se consulta historial de vacunación, THE Vaccination_Management_Module SHALL invocar CattleService para obtener arete y datos básicos del animal
3. THE Vaccination_Management_Module SHALL exponer método público VaccinationService.getVaccinationCountByAnimal(UUID animalId) para que cattle-inventory consulte total de vacunaciones
4. THE Vaccination_Management_Module SHALL exponer método público VaccinationService.getNextScheduledVaccination(UUID animalId) para mostrar próxima dosis en detalle de animal

### Requirement 16: Auditoría de Operaciones

**User Story:** Como administrador, quiero auditoría completa de operaciones de vacunación, para que rastree cambios.

#### Acceptance Criteria

1. THE Vaccination_Management_Module SHALL registrar en tabla vaccination_audit cada operación CREATE_VACCINE, UPDATE_VACCINE, CREATE_LOT, ADJUST_STOCK, APPLY_VACCINATION
2. WHEN se aplica vacuna, THE Vaccination_Management_Module SHALL insertar en vaccination_audit: audit_id, operation_type (APPLY_VACCINATION), entity_id (id_aplicacion), timestamp, modified_by, tenant_id, old_values (stock anterior), new_values (stock nuevo + datos aplicación)
3. THE Vaccination_Management_Module SHALL retener registros de auditoría por mínimo 730 días

## Notes

- Este módulo debe implementarse después de user-management y cattle-inventory
- La integración con cattle-inventory es crítica: siempre validar estado del animal
- El descuento automático de stock debe ser transaccional para evitar inconsistencias
- Las alertas de stock bajo y vencimiento deben ejecutarse en jobs programados
- El cálculo de próxima dosis es fundamental para planificación de campañas
- Considerar implementar notificaciones push/email en futuras versiones
- Los reportes de consumo y costos son críticos para análisis financiero
- La validación de veterinarios asegura que solo personal autorizado aplique vacunas
