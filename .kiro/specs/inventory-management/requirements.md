# Requirements Document

## Introduction

El módulo de gestión de inventario de insumos (inventory-management) es un componente fundamental de Vacapp que proporciona control completo del inventario de materiales, alimentos, medicamentos, herramientas y suministros del rancho. Este módulo permite gestionar categorías personalizadas, registrar movimientos de entrada/salida con trazabilidad completa, vincular consumo a potreros y actividades específicas, generar alertas automáticas de stock bajo y vencimientos, y producir reportes de valoración y costos. Se integra con geographic-control para asignar insumos a potreros y con user-management para rastrear responsables de movimientos.

## Glossary

- **Inventory_Management_Module**: El módulo que gestiona inventario de insumos del rancho
- **Supply**: Insumo o artículo del inventario (alimentos, medicamentos, herramientas, etc.)
- **Supply_Category**: Categoría personalizable de insumos creada por usuario
- **Measurement_Unit**: Unidad de medida predefinida (kg, L, unidades, sacos, ton, piezas, etc.)
- **Supply_Movement**: Movimiento de entrada o salida del inventario
- **Movement_Type**: Tipo de movimiento (Entrada, Salida)
- **Movement_Reason**: Motivo del movimiento (Compra, Consumo, Merma, Donación, Ajuste, Devolución)
- **Supply_Lot**: Lote opcional de un insumo con fecha de vencimiento
- **Stock_Alert**: Alerta automática cuando cantidad <= cantidadMinima
- **Expiration_Alert**: Alerta automática para insumos próximos a vencer
- **Inventory_Valuation**: Valoración monetaria total del inventario
- **Consumption_Report**: Reporte de consumo de insumos por periodo
- **Cost_Report**: Reporte de costos de consumo
- **Pasture_Assignment**: Asignación de consumo de insumo a potrero específico
- **Multi_Tenant_Isolation**: Aislamiento de datos por tenant_id y rancho_id

## Requirements

### Requirement 1: Gestión de Unidades de Medida

**User Story:** Como administrador del sistema, quiero gestionar catálogo de unidades de medida, para que estandarice mediciones en el inventario.

#### Acceptance Criteria

1. THE Inventory_Management_Module SHALL soportar unidades predefinidas: Kilogramo, Gramo, Tonelada, Litro, Mililitro, Unidad, Pieza, Saco, Caja, Paquete, Bulto, Dosis, Metro, Galón
2. WHEN un usuario consulta unidades via GET /api/v1/inventory/units, THE Inventory_Management_Module SHALL retornar array con id_unidad, nombre, abreviatura, tipo (Peso, Volumen, Cantidad, Longitud)
3. THE Inventory_Management_Module SHALL almacenar unidades en tabla measurement_units con campos: id_unidad (UUID PK), nombre (VARCHAR 50 UNIQUE), abreviatura (VARCHAR 10), tipo (ENUM), activo (BOOLEAN DEFAULT TRUE)
4. WHEN se crea un insumo, THE Inventory_Management_Module SHALL validar que id_unidad existe y está activo, retornar HTTP 400 si es inválido

### Requirement 2: Gestión de Categorías Personalizadas

**User Story:** Como usuario de Vacapp, quiero crear y gestionar mis propias categorías de insumos, para que organice el inventario según mis necesidades.

#### Acceptance Criteria

1. WHEN un usuario crea una categoría via POST /api/v1/inventory/categories, THE Inventory_Management_Module SHALL crear registro con campos: id_categoria (UUID), nombre (VARCHAR 100 NOT NULL), descripcion (TEXT), color (VARCHAR 7 para hex color), icono (VARCHAR 50), rancho_id (UUID FK), tenant_id (UUID), activo (BOOLEAN DEFAULT TRUE), created_at, created_by
2. WHEN se proporciona nombre vacío o solo espacios, THE Inventory_Management_Module SHALL retornar HTTP 400 con mensaje "Nombre de categoría es requerido"
3. WHEN se crea categoría con nombre que ya existe en el mismo rancho, THE Inventory_Management_Module SHALL retornar HTTP 409 con mensaje "Ya existe una categoría con ese nombre"
4. THE Inventory_Management_Module SHALL filtrar categorías por tenant_id y rancho_id en todas las consultas
5. WHEN un usuario lista categorías via GET /api/v1/inventory/categories, THE Inventory_Management_Module SHALL retornar array WHERE tenant_id = :currentTenantId AND rancho_id = :currentRanchoId AND activo = TRUE ordenado por nombre ASC
6. WHEN un usuario consulta una categoría, THE Inventory_Management_Module SHALL incluir: datos básicos, total_insumos (COUNT de insumos en la categoría), valor_total (SUM de cantidad * precioUnitario)
7. THE Inventory_Management_Module SHALL permitir actualizar nombre, descripción, color, icono de una categoría
8. WHEN un usuario archiva una categoría, THE Inventory_Management_Module SHALL cambiar activo = FALSE sin eliminar físicamente
9. THE Inventory_Management_Module SHALL prohibir archivar categoría que tiene insumos activos asociados, retornar HTTP 409 con mensaje "No se puede archivar categoría con insumos activos"

### Requirement 3: Registro de Insumos en Inventario

**User Story:** Como usuario, quiero registrar insumos en el inventario con toda su información, para que tenga control de materiales y suministros del rancho.

#### Acceptance Criteria

1. WHEN un usuario registra un nuevo insumo via POST /api/v1/inventory/supplies, THE Inventory_Management_Module SHALL crear registro con campos: id_insumo (UUID), nombre (VARCHAR 200 NOT NULL), id_categoria (UUID FK NOT NULL), id_unidad_medida (UUID FK NOT NULL), cantidad_total (DECIMAL 10,3 DEFAULT 0 calculada desde lotes), cantidad_minima (DECIMAL 10,3 DEFAULT 0), descripcion (TEXT), ubicacion (VARCHAR 200), rancho_id (UUID FK), tenant_id (UUID), activo (BOOLEAN DEFAULT TRUE), created_at, created_by
2. WHEN se proporciona nombre vacío, THE Inventory_Management_Module SHALL retornar HTTP 400 con mensaje "Nombre del insumo es requerido"
3. WHEN se proporciona cantidad_minima negativa, THE Inventory_Management_Module SHALL retornar HTTP 400 con mensaje "Cantidad mínima no puede ser negativa"
4. THE Inventory_Management_Module SHALL filtrar insumos por tenant_id y rancho_id en todas las consultas
5. WHEN un usuario lista insumos via GET /api/v1/inventory/supplies, THE Inventory_Management_Module SHALL retornar paginación con insumos WHERE tenant_id = :currentTenantId AND rancho_id = :currentRanchoId AND activo = TRUE
6. WHEN un usuario consulta un insumo via GET /api/v1/inventory/supplies/{id}, THE Inventory_Management_Module SHALL incluir: datos básicos, categoria_nombre, unidad_nombre, cantidad_total (SUM de lotes), lotes_activos (COUNT), valor_total (SUM de lotes con cantidad > 0), stock_status (OK, BAJO, CRITICO), proximo_vencimiento
7. THE Inventory_Management_Module SHALL calcular cantidad_total como: SELECT SUM(cantidad_disponible) FROM supply_lots WHERE id_insumo = :id AND fecha_vencimiento >= CURRENT_DATE (o IS NULL)
8. THE Inventory_Management_Module SHALL clasificar stock_status como: CRITICO si cantidad_total <= cantidad_minima * 0.5, BAJO si cantidad_total <= cantidad_minima, OK si cantidad_total > cantidad_minima
9. WHEN un usuario busca insumos via GET /api/v1/inventory/supplies?search={texto}, THE Inventory_Management_Module SHALL ejecutar LIKE case-insensitive en campos: nombre, descripcion, ubicacion
10. WHEN un usuario filtra por categoría via GET /api/v1/inventory/supplies?categoria={id}, THE Inventory_Management_Module SHALL retornar solo insumos WHERE id_categoria = :id
11. WHEN un usuario filtra por stock bajo via GET /api/v1/inventory/supplies?stockBajo=true, THE Inventory_Management_Module SHALL retornar solo insumos WHERE cantidad_total <= cantidad_minima
12. THE Inventory_Management_Module SHALL permitir actualizar campos del insumo excepto cantidad_total (se calcula desde lotes)
13. WHEN un usuario archiva un insumo, THE Inventory_Management_Module SHALL cambiar activo = FALSE sin eliminar físicamente

### Requirement 4: Gestión de Lotes de Insumos

**User Story:** Como usuario, quiero registrar múltiples lotes de un mismo insumo con diferentes precios y fechas de vencimiento, para que tenga control detallado del inventario.

#### Acceptance Criteria

1. THE Inventory_Management_Module SHALL permitir registrar múltiples lotes por insumo en tabla supply_lots con campos: id_lote (UUID PK), id_insumo (UUID FK NOT NULL), numero_lote (VARCHAR 50 NOT NULL), fecha_compra (DATE NOT NULL), cantidad_disponible (DECIMAL 10,3 NOT NULL DEFAULT 0), precio_unitario (DECIMAL 10,2 NOT NULL), fecha_vencimiento (DATE opcional), proveedor (VARCHAR 200), observaciones (TEXT), rancho_id (UUID FK), tenant_id (UUID), created_at, created_by
2. WHEN se crea un lote, THE Inventory_Management_Module SHALL validar que precio_unitario > 0, retornar HTTP 400 si es inválido
3. WHEN se proporciona fecha_vencimiento en el pasado, THE Inventory_Management_Module SHALL retornar HTTP 400 con mensaje "Fecha de vencimiento no puede estar en el pasado"
4. WHEN se proporciona fecha_compra futura, THE Inventory_Management_Module SHALL retornar HTTP 400 con mensaje "Fecha de compra no puede ser futura"
5. WHEN un usuario consulta lotes de un insumo via GET /api/v1/inventory/supplies/{id}/lots, THE Inventory_Management_Module SHALL retornar array ordenado por fecha_compra ASC con campos: id_lote, numero_lote, fecha_compra, cantidad_disponible, precio_unitario, fecha_vencimiento, proveedor, dias_para_vencer, valor_lote (cantidad_disponible * precio_unitario)
6. THE Inventory_Management_Module SHALL calcular stock_total de un insumo como: SELECT SUM(cantidad_disponible) FROM supply_lots WHERE id_insumo = :id AND (fecha_vencimiento >= CURRENT_DATE OR fecha_vencimiento IS NULL)
7. THE Inventory_Management_Module SHALL permitir ajustar cantidad de un lote via PUT /api/v1/supply-lots/{id}/adjust con motivo (entrada manual, corrección inventario, merma)
8. WHEN se ajusta cantidad, THE Inventory_Management_Module SHALL registrar en tabla lot_adjustments: id_ajuste, id_lote, cantidad_anterior, cantidad_nueva, motivo, created_at, created_by
9. THE Inventory_Management_Module SHALL marcar lotes vencidos ejecutando UPDATE supply_lots SET cantidad_disponible = 0 WHERE fecha_vencimiento < CURRENT_DATE - 1 DAY (job nocturno)
10. WHEN se crea un lote nuevo via entrada de compra, THE Inventory_Management_Module SHALL registrar también en supply_movements con tipo=ENTRADA, motivo=COMPRA

### Requirement 5: Movimientos de Inventario (Entradas)

**User Story:** Como usuario, quiero registrar entradas de insumos creando lotes automáticamente, para que tenga trazabilidad de cada compra.

#### Acceptance Criteria

1. WHEN un usuario registra entrada via POST /api/v1/inventory/movements, THE Inventory_Management_Module SHALL crear automáticamente un lote nuevo con campos: id_movimiento (UUID), id_insumo (UUID FK NOT NULL), id_lote_generado (UUID FK), tipo_movimiento (ENUM 'ENTRADA'), motivo (ENUM NOT NULL), cantidad (DECIMAL 10,3 NOT NULL > 0), precio_unitario (DECIMAL 10,2 NOT NULL), fecha_movimiento (DATE NOT NULL), numero_lote (VARCHAR 50 generado automáticamente), fecha_vencimiento (DATE opcional), proveedor (VARCHAR 200), id_usuario_responsable (UUID FK), observaciones (TEXT), rancho_id (UUID FK), tenant_id (UUID), created_at, created_by
2. THE Inventory_Management_Module SHALL soportar motivos de entrada: COMPRA, DONACION, DEVOLUCION, AJUSTE_ENTRADA
3. WHEN se registra entrada con tipo_movimiento = ENTRADA, THE Inventory_Management_Module SHALL crear lote nuevo en supply_lots con: cantidad_disponible = cantidad, precio_unitario = precio_unitario_movimiento, fecha_compra = fecha_movimiento, numero_lote autogenerado (formato: "{INSUMO}-{YYYYMMDD}-{SEQUENCE}")
4. WHEN se proporciona cantidad <= 0, THE Inventory_Management_Module SHALL retornar HTTP 400 con mensaje "Cantidad debe ser mayor que cero"
5. WHEN se proporciona fecha_movimiento futura, THE Inventory_Management_Module SHALL retornar HTTP 400 con mensaje "Fecha de movimiento no puede ser futura"
6. WHEN se proporciona id_insumo que no existe o no pertenece al tenant/rancho, THE Inventory_Management_Module SHALL retornar HTTP 400 con mensaje "Insumo no encontrado"
7. IF motivo = COMPRA, THEN precio_unitario SHALL ser obligatorio (NOT NULL > 0)
8. IF motivo IN (DONACION, DEVOLUCION), THEN precio_unitario SHALL ser opcional (puede ser 0 o estimado)
9. IF se proporciona fecha_vencimiento, THEN THE Inventory_Management_Module SHALL verificar que sea futura, retornar HTTP 400 si está en el pasado, y almacenar en lote
10. WHEN entrada exitosa crea lote y cantidad_total > cantidad_minima, THE Inventory_Management_Module SHALL resolver alertas de stock bajo pendientes: UPDATE stock_alerts SET resuelta = TRUE, fecha_resolucion = NOW() WHERE id_insumo = :id AND resuelta = FALSE
11. THE Inventory_Management_Module SHALL registrar en inventory_audit: operation_type (STOCK_IN), entity_id (id_movimiento), old_values (stock_total anterior), new_values (stock_total nuevo + datos del lote)
12. THE Inventory_Management_Module SHALL retornar HTTP 201 con MovementResponse conteniendo: id_movimiento, id_lote, numero_lote, nuevo_stock_total, valor_lote (cantidad * precio_unitario)

### Requirement 6: Movimientos de Inventario (Salidas con FIFO)

**User Story:** Como usuario, quiero registrar salidas de insumos consumiendo automáticamente desde los lotes más antiguos (FIFO), para que tenga trazabilidad exacta de qué compra se consumió.

#### Acceptance Criteria

1. WHEN un usuario registra salida via POST /api/v1/inventory/movements con tipo_movimiento = SALIDA, THE Inventory_Management_Module SHALL validar que cantidad <= stock_total_disponible, retornar HTTP 409 con mensaje "Stock insuficiente" si no hay suficiente
2. THE Inventory_Management_Module SHALL soportar motivos de salida: CONSUMO, MERMA, DONACION, AJUSTE_SALIDA
3. WHEN se registra salida exitosa, THE Inventory_Management_Module SHALL aplicar método FIFO (First In, First Out): decrementar cantidad_disponible de lotes ordenados por fecha_compra ASC hasta completar la cantidad solicitada
4. THE Inventory_Management_Module SHALL crear registros en tabla lot_consumption_detail vinculando cada salida con los lotes específicos consumidos: id_detalle (UUID), id_movimiento (UUID FK), id_lote (UUID FK), cantidad_consumida (DECIMAL), precio_unitario_lote (DECIMAL), costo_parcial (cantidad_consumida * precio_unitario_lote)
5. IF motivo = CONSUMO AND se proporciona id_potrero, THEN THE Inventory_Management_Module SHALL validar que potrero existe y está activo consultando GeographyService.isPotreroActive(id_potrero), retornar HTTP 400 si es inválido
6. IF motivo = CONSUMO, THEN id_usuario_responsable SHALL ser obligatorio (NOT NULL)
7. WHEN salida causa que cantidad_total <= cantidad_minima, THE Inventory_Management_Module SHALL crear Stock_Alert automáticamente
8. IF motivo = MERMA, THEN observaciones SHALL ser obligatorio con justificación de la merma
9. THE Inventory_Management_Module SHALL calcular costo_total_movimiento como: SUM(cantidad_consumida * precio_unitario_lote) de todos los lotes afectados y almacenar en campo costo_calculado del movimiento
10. THE Inventory_Management_Module SHALL registrar en inventory_audit: operation_type (STOCK_OUT), motivo, cantidad, lotes_afectados (JSON con detalles), potrero_id (si aplica)
11. WHEN un usuario consulta movimientos de un insumo via GET /api/v1/inventory/supplies/{id}/movements, THE Inventory_Management_Module SHALL retornar array ordenado por fecha_movimiento DESC con campos: tipo_movimiento, motivo, cantidad, costo_calculado, fecha_movimiento, usuario_responsable_nombre, potrero_nombre (si aplica), lotes_consumidos (array con detalle), observaciones
12. WHEN un usuario consulta detalle de una salida via GET /api/v1/inventory/movements/{id}/lot-details, THE Inventory_Management_Module SHALL retornar array de lotes consumidos con: numero_lote, cantidad_consumida, precio_unitario_lote, costo_parcial, fecha_compra_lote, proveedor_lote

### Requirement 6: Consumo de Insumos Vinculado a Potreros

**User Story:** Como usuario, quiero registrar consumo de insumos asignado a potreros específicos, para que rastree qué materiales se usan en cada área.

#### Acceptance Criteria

1. WHEN se registra movimiento con motivo = CONSUMO e id_potrero NOT NULL, THE Inventory_Management_Module SHALL crear vínculo en tabla pasture_supply_consumption con campos: id_consumo (UUID), id_movimiento (UUID FK), id_potrero (UUID FK), cantidad, fecha_consumo, id_usuario_responsable (UUID FK), tenant_id, rancho_id
2. WHEN un usuario consulta consumo por potrero via GET /api/v1/pastures/{potrero_id}/supply-consumption, THE Inventory_Management_Module SHALL retornar array agrupado por insumo con: insumo_nombre, total_cantidad_consumida, unidad_medida, costo_total, ultimo_consumo_fecha
3. WHEN un usuario consulta reporte de consumo por periodo via GET /api/v1/reports/supply-consumption?fecha_inicio={fecha}&fecha_fin={fecha}&potrero_id={id}, THE Inventory_Management_Module SHALL retornar consumo filtrado por potrero y rango de fechas
4. THE Inventory_Management_Module SHALL permitir filtrar consumo por categoría de insumo
5. THE Inventory_Management_Module SHALL calcular costo total de consumo por potrero como: SUM(cantidad * precio_unitario) GROUP BY id_potrero

### Requirement 7: Alertas de Stock Bajo

**User Story:** Como usuario, quiero recibir alertas automáticas cuando stock de insumos esté bajo, para que reponga materiales a tiempo.

#### Acceptance Criteria

1. WHEN stock de un insumo cae a cantidad <= cantidad_minima tras movimiento de salida, THE Inventory_Management_Module SHALL crear registro en tabla stock_alerts con campos: id_alerta (UUID), id_insumo (UUID FK), cantidad_actual (DECIMAL), cantidad_minima (DECIMAL), nivel_alerta (ENUM), fecha_alerta (TIMESTAMP), resuelta (BOOLEAN DEFAULT FALSE), fecha_resolucion (TIMESTAMP), tenant_id, rancho_id
2. THE Inventory_Management_Module SHALL clasificar nivel_alerta como: CRITICO si cantidad <= cantidad_minima * 0.5, BAJO si cantidad <= cantidad_minima
3. WHEN un usuario consulta alertas activas via GET /api/v1/inventory/alerts/stock, THE Inventory_Management_Module SHALL retornar array WHERE resuelta = FALSE filtrado por tenant_id y rancho_id ordenado por nivel_alerta DESC, cantidad_actual ASC
4. THE Inventory_Management_Module SHALL incluir en respuesta: insumo_nombre, categoria_nombre, cantidad_actual, cantidad_minima, unidad_medida, nivel_alerta, dias_desde_alerta
5. WHEN stock se incrementa por encima de cantidad_minima via entrada, THE Inventory_Management_Module SHALL marcar alerta como resuelta automáticamente
6. WHEN un usuario marca manualmente alerta como resuelta via PUT /api/v1/inventory/alerts/{id}/resolve, THE Inventory_Management_Module SHALL actualizar resuelta = TRUE, fecha_resolucion = NOW()

### Requirement 8: Alertas de Vencimiento de Insumos

**User Story:** Como usuario, quiero alertas de insumos próximos a vencer, para que use productos antes de que caduquen.

#### Acceptance Criteria

1. THE Inventory_Management_Module SHALL ejecutar job diario @Scheduled(cron = "0 0 7 * * *") que identifica insumos WHERE fecha_vencimiento BETWEEN CURRENT_DATE AND CURRENT_DATE + 30 DAYS AND cantidad > 0
2. WHEN se detecta insumo próximo a vencer, THE Inventory_Management_Module SHALL crear registro en tabla expiration_alerts con campos: id_alerta (UUID), id_insumo (UUID FK), fecha_vencimiento (DATE), dias_para_vencer (INT), cantidad_en_riesgo (DECIMAL), nivel_urgencia (ENUM), fecha_alerta (TIMESTAMP), tenant_id, rancho_id
3. THE Inventory_Management_Module SHALL clasificar nivel_urgencia como: URGENTE si dias_para_vencer <= 7, PROXIMO si dias_para_vencer <= 15, PREVENTIVO si dias_para_vencer <= 30
4. WHEN un usuario consulta alertas de vencimiento via GET /api/v1/inventory/alerts/expiration, THE Inventory_Management_Module SHALL retornar array ordenado por dias_para_vencer ASC con: insumo_nombre, categoria_nombre, fecha_vencimiento, dias_para_vencer, cantidad_en_riesgo, unidad_medida, nivel_urgencia, ubicacion
5. WHEN insumo vence (fecha_vencimiento < CURRENT_DATE), THE Inventory_Management_Module SHALL marcar con flag vencido = TRUE sin eliminar registro
6. THE Inventory_Management_Module SHALL permitir registrar movimiento de salida con motivo = MERMA para insumos vencidos

### Requirement 9: Reportes de Valoración de Inventario

**User Story:** Como administrador, quiero reportes de valoración del inventario, para que conozca el valor monetario de mis activos.

#### Acceptance Criteria

1. WHEN un usuario consulta valoración via GET /api/v1/reports/inventory-valuation, THE Inventory_Management_Module SHALL calcular: valor_total_inventario = SUM(cantidad * precio_unitario) WHERE activo = TRUE AND cantidad > 0
2. THE Inventory_Management_Module SHALL agrupar valoración por categoría mostrando: categoria_nombre, total_insumos, cantidad_total_items, valor_total_categoria, porcentaje_del_total
3. THE Inventory_Management_Module SHALL incluir top 10 insumos más valiosos ordenados por (cantidad * precio_unitario) DESC
4. THE Inventory_Management_Module SHALL incluir resumen de alertas: total_alertas_stock_bajo, total_alertas_vencimiento, valor_en_riesgo_por_vencimiento
5. THE Inventory_Management_Module SHALL filtrar por tenant_id y rancho_id
6. THE Inventory_Management_Module SHALL permitir exportar reporte en PDF (integración futura)

### Requirement 10: Reportes de Consumo de Insumos

**User Story:** Como usuario, quiero reportes de consumo de insumos por periodo, para que analice uso de materiales.

#### Acceptance Criteria

1. WHEN un usuario genera reporte via GET /api/v1/reports/supply-consumption?fecha_inicio={fecha}&fecha_fin={fecha}, THE Inventory_Management_Module SHALL retornar array agrupado por insumo con: insumo_nombre, categoria_nombre, total_cantidad_salida (SUM de movimientos tipo SALIDA), costo_total, movimientos_count, potreros_utilizados (ARRAY de nombres)
2. THE Inventory_Management_Module SHALL permitir filtrar por categoría, potrero, motivo (CONSUMO, MERMA, etc.)
3. THE Inventory_Management_Module SHALL incluir gráfica de tendencia: array de {fecha, cantidad_consumida} agrupado por día/semana/mes
4. THE Inventory_Management_Module SHALL calcular consumo_promedio_diario como: total_cantidad / dias_en_periodo
5. THE Inventory_Management_Module SHALL identificar insumos de mayor consumo en el periodo

### Requirement 11: Reportes de Costos de Consumo

**User Story:** Como administrador, quiero reportes de costos de consumo de insumos, para que controle gastos operativos.

#### Acceptance Criteria

1. WHEN un usuario genera reporte de costos via GET /api/v1/reports/consumption-costs?fecha_inicio={fecha}&fecha_fin={fecha}, THE Inventory_Management_Module SHALL calcular: costo_total = SUM(cantidad * precio_unitario) FROM movements WHERE tipo_movimiento = SALIDA AND motivo IN (CONSUMO, MERMA)
2. THE Inventory_Management_Module SHALL agrupar costos por: categoría, motivo, potrero (si aplica), usuario_responsable
3. THE Inventory_Management_Module SHALL incluir comparación con periodo anterior mostrando variación porcentual
4. THE Inventory_Management_Module SHALL identificar categorías con mayor gasto
5. THE Inventory_Management_Module SHALL calcular costo_promedio_por_potrero si se filtra por consumo

### Requirement 12: API REST para Gestión de Inventario

**User Story:** Como desarrollador móvil, quiero consumir API REST para gestión de inventario, para que integre la funcionalidad en la aplicación móvil.

#### Acceptance Criteria

1. THE Inventory_Management_Module SHALL exponer endpoints REST bajo ruta base /api/v1/inventory/ con autenticación JWT obligatoria
2. THE Inventory_Management_Module SHALL implementar endpoints: POST /inventory/categories, GET /inventory/categories, GET /inventory/categories/{id}, PUT /inventory/categories/{id}, DELETE /inventory/categories/{id}, POST /inventory/supplies, GET /inventory/supplies, GET /inventory/supplies/{id}, PUT /inventory/supplies/{id}, DELETE /inventory/supplies/{id}, GET /inventory/supplies/{id}/lots, POST /inventory/movements, GET /inventory/movements, GET /inventory/supplies/{id}/movements, GET /inventory/movements/{id}/lot-details, GET /inventory/alerts/stock, GET /inventory/alerts/expiration, PUT /inventory/alerts/{id}/resolve, GET /reports/inventory-valuation, GET /reports/supply-consumption, GET /reports/consumption-costs, GET /inventory/units
3. THE Inventory_Management_Module SHALL aceptar requests con Content-Type: application/json; charset=UTF-8
4. WHEN endpoint retorna colección, THE Inventory_Management_Module SHALL incluir pagination
5. WHEN operación completa exitosamente, THE Inventory_Management_Module SHALL retornar: 200 para lecturas/actualizaciones, 201 para creaciones, 204 para archivado
6. WHEN Request DTO falla validación, THE Inventory_Management_Module SHALL retornar HTTP 400 con array de errores
7. WHEN JWT inválido, THE Inventory_Management_Module SHALL retornar HTTP 401
8. WHEN usuario intenta acceder a recurso de otro tenant, THE Inventory_Management_Module SHALL retornar HTTP 403
9. WHEN recurso no existe, THE Inventory_Management_Module SHALL retornar HTTP 404
10. THE Inventory_Management_Module SHALL documentar endpoints en OpenAPI 3.0 YAML en src/main/resources/openapi/openapi-inventory.yaml

### Requirement 13: Interfaz Web para Gestión de Inventario

**User Story:** Como usuario web, quiero gestionar inventario de insumos en interfaz intuitiva, para que administre fácilmente desde navegador.

#### Acceptance Criteria

1. THE Inventory_Management_Module SHALL servir página HTML en /inventory con lista de insumos mostrando: nombre, categoría, cantidad, unidad, stock_status (badge), valor_total, alertas
2. WHEN usuario hace clic en insumo, THE Inventory_Management_Module SHALL navegar a /inventory/supplies/{id} con tabs: Información, Movimientos, Estadísticas
3. THE Inventory_Management_Module SHALL renderizar formulario para registrar movimiento con campos: tipo (entrada/salida), motivo (select), cantidad, fecha, potrero (si consumo), usuario_responsable, observaciones
4. WHEN usuario crea nuevo insumo, THE Inventory_Management_Module SHALL mostrar modal con todos los campos
5. THE Inventory_Management_Module SHALL implementar búsqueda en tiempo real con debounce 300ms
6. THE Inventory_Management_Module SHALL mostrar dashboard con KPIs: valor_total_inventario, total_alertas_activas, insumos_por_vencer_7dias, movimientos_ultimo_mes
7. THE Inventory_Management_Module SHALL mostrar notificaciones badge con count de alertas

### Requirement 14: Integración con Módulo Geográfico

**User Story:** Como sistema integrado, quiero que inventario se sincronice con estructura de potreros, para que registre consumo por área.

#### Acceptance Criteria

1. WHEN se registra consumo con id_potrero, THE Inventory_Management_Module SHALL invocar GeographyService.isPotreroActive(id_potrero) para validar
2. THE Inventory_Management_Module SHALL exponer método público InventoryService.getSupplyConsumptionByPasture(UUID potreroId, LocalDate fechaInicio, LocalDate fechaFin)
3. THE Inventory_Management_Module SHALL exponer método público InventoryService.getTotalCostByPasture(UUID potreroId, LocalDate fechaInicio, LocalDate fechaFin)

### Requirement 15: Auditoría de Operaciones

**User Story:** Como administrador, quiero auditoría completa de operaciones de inventario, para que rastree cambios.

#### Acceptance Criteria

1. THE Inventory_Management_Module SHALL registrar en tabla inventory_audit cada operación CREATE_SUPPLY, UPDATE_SUPPLY, STOCK_IN, STOCK_OUT, CREATE_CATEGORY
2. WHEN se registra movimiento, THE Inventory_Management_Module SHALL insertar en inventory_audit: audit_id, operation_type, entity_id, timestamp, modified_by, tenant_id, old_values (stock anterior), new_values (stock nuevo)
3. THE Inventory_Management_Module SHALL retener registros por mínimo 730 días

## Notes

- Este módulo debe implementarse después de user-management y geographic-control
- Las categorías son personalizadas por usuario/rancho para máxima flexibilidad
- Los movimientos de stock deben ser transaccionales para evitar inconsistencias
- Las alertas automáticas son críticas para evitar falta de insumos
- El consumo vinculado a potreros permite análisis de costos por área
- La valoración de inventario es fundamental para reportes financieros
- Considerar implementar sistema de códigos de barras en futuras versiones
