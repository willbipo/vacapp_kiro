# Requirements Document

## Introduction

El módulo de inventario de ganado bovino (cattle-inventory) es el componente central y más crítico de Vacapp. Este módulo gestiona el registro completo del ganado, incluyendo identificación, genealogía, ubicación, estado reproductivo, historial de movimientos entre potreros, y eventos de salud. Cada animal tiene un arete único a nivel global en el sistema, cumple con regulaciones mexicanas (Folio REEMO), y mantiene trazabilidad completa desde nacimiento hasta salida del rancho. El módulo es la base sobre la cual operan todos los demás módulos funcionales (salud, reproducción, producción, nutrición).

## Glossary

- **Cattle_Inventory_Module**: El módulo que gestiona el registro completo del ganado bovino
- **Animal**: Entidad principal que representa un bovino en el sistema
- **Arete**: Número identificador único del animal a nivel global en Vacapp
- **Folio_REEMO**: Registro Electrónico de Movilización obligatorio en México para transporte de ganado
- **Cattle_Status**: Estado del animal (Activa, Vendida, Muerta, Prestada, Preñada, En Reposo)
- **Cattle_Type**: Tipo comercial del animal (Venta, Cría, Engorda, Semental, Vientre)
- **Sex**: Sexo del animal (Macho, Hembra)
- **Breed**: Raza del animal (Charolais, Angus, Brahman, Criollo, Cruzada, etc.)
- **Age_Calculator**: Componente que calcula edad en meses desde fecha de nacimiento
- **Genealogy**: Relación de parentesco (madre-hijo, padre-hijo)
- **Pasture_Assignment**: Asignación del animal a un potrero específico
- **Pasture_History**: Historial completo de movimientos del animal entre potreros
- **Weight_Record**: Registro de peso del animal en una fecha específica
- **Health_Event**: Evento de salud (vacunación, tratamiento médico, diagnóstico)
- **Birth_Event**: Evento de nacimiento de una cría
- **Cattle_Repository**: Puerto de salida para persistencia de animales
- **Pasture_Integration**: Integración con módulo geographic-control para validar potreros
- **Active_Inventory**: Animales con status Activa, Preñada o En Reposo que están físicamente en el rancho
- **Historical_Inventory**: Animales con status Vendida o Muerta que ya no están en el rancho

## Requirements

### Requirement 1: Registro de Animal con Identificación Única

**User Story:** Como usuario de Vacapp, quiero registrar un nuevo animal con arete único, para que pueda rastrear individualmente cada bovino en mi rancho.

#### Acceptance Criteria

1. WHEN un usuario registra un nuevo animal con número de arete único (no existe previamente en el sistema a nivel global), THE Cattle_Inventory_Module SHALL crear el registro con status Activa y retornar código HTTP 201 con datos del animal
2. WHEN un usuario intenta registrar un animal con arete que ya existe en cualquier tenant, THE Cattle_Inventory_Module SHALL retornar código HTTP 409 con mensaje "Arete ya registrado en el sistema"
3. THE Cattle_Inventory_Module SHALL validar que el arete contenga entre 4 y 20 caracteres alfanuméricos (letras mayúsculas, minúsculas y números) antes de persistir
4. WHEN se proporciona un arete con caracteres especiales (\#, @, /, espacios), THE Cattle_Inventory_Module SHALL retornar código HTTP 400 con mensaje "El arete solo puede contener letras y números"
5. WHEN un nuevo animal es registrado, THE Cattle_Inventory_Module SHALL asignar automáticamente: animal_id (UUID), created_at (timestamp UTC actual), created_by (user_id del autenticado), tenant_id del contexto, status = Activa
6. WHEN un usuario lista animales de su rancho sin especificar página, THE Cattle_Inventory_Module SHALL retornar la primera página (page=0) con máximo 100 animales filtrados por tenant_id y rancho_id con metadatos de paginación
7. WHEN un usuario busca un animal por arete, THE Cattle_Inventory_Module SHALL realizar búsqueda case-insensitive y retornar el animal si pertenece al tenant del usuario
8. THE Cattle_Inventory_Module SHALL almacenar arete en mayúsculas (toUpperCase) independientemente de cómo lo ingrese el usuario para consistencia
9. WHEN un animal es registrado, THE Cattle_Inventory_Module SHALL registrar en tabla cattle_audit: operation_type (CREATE), timestamp UTC, modified_by, old_values (null), new_values (JSON con datos del animal)
10. WHEN un usuario proporciona arete_anterior (opcional, max 20 caracteres), THE Cattle_Inventory_Module SHALL almacenar el valor sin validar unicidad (permite duplicados)

### Requirement 2: Información Biológica y Genealógica

**User Story:** Como ganadero, quiero registrar información biológica del animal (sexo, raza, fecha de nacimiento, padres), para que tenga trazabilidad de la genética y genealogía de mi hato.

#### Acceptance Criteria

1. WHEN se proporciona sexo del animal, THE Cattle_Inventory_Module SHALL validar que el valor esté en catálogo [Macho, Hembra] y retornar HTTP 400 si es inválido
2. WHEN se proporciona raza del animal, THE Cattle_Inventory_Module SHALL validar que el valor esté en catálogo [Charolais, Angus, Brahman, Hereford, Simmental, Limousin, Criollo, Brangus, Santa Gertrudis, Cruzada] y retornar HTTP 400 si es inválido con mensaje "Raza no válida"
3. WHEN se proporciona fecha_nacimiento posterior a la fecha actual UTC, THE Cattle_Inventory_Module SHALL retornar código HTTP 400 con mensaje "La fecha de nacimiento no puede ser futura"
4. WHEN se proporciona fecha_nacimiento válida (≤ fecha actual), THE Age_Calculator SHALL calcular automáticamente el campo meses como: TIMESTAMPDIFF(MONTH, fecha_nacimiento, NOW()) y almacenar en base de datos
5. THE Cattle_Inventory_Module SHALL permitir especificar madre_id (UUID opcional) que debe existir en tabla cattle, tener sexo = Hembra, y pertenecer al mismo rancho
6. THE Cattle_Inventory_Module SHALL permitir especificar padre_id (UUID opcional) que debe existir en tabla cattle, tener sexo = Macho, y pertenecer al mismo rancho
7. WHEN se especifica madre_id que no existe, es de sexo Macho, o pertenece a otro rancho, THE Cattle_Inventory_Module SHALL retornar código HTTP 400 con mensaje "Madre inválida"
8. WHEN se especifica padre_id que no existe, es de sexo Hembra, o pertenece a otro rancho, THE Cattle_Inventory_Module SHALL retornar código HTTP 400 con mensaje "Padre inválido"
9. WHEN un usuario consulta un animal, THE Cattle_Inventory_Module SHALL incluir en la respuesta: arete, nombre_madre (JOIN), nombre_padre (JOIN), total_hijos (COUNT de animales donde madre_id = animal_id o padre_id = animal_id)
10. THE Cattle_Inventory_Module SHALL actualizar automáticamente el campo meses en cada consulta GET sin persistir en base de datos (cálculo en tiempo real)

### Requirement 3: Ubicación y Movimientos en Potreros

**User Story:** Como usuario, quiero registrar en qué potrero está cada animal y su historial de movimientos, para que sepa la ubicación actual y pasada de mi ganado.

#### Acceptance Criteria

1. WHEN un animal es registrado o movido a un potrero, THE Cattle_Inventory_Module SHALL validar que potrero_id exista y esté activo consultando geographic-control module via GeographyService.isPotreroActive(UUID)
2. IF GeographyService.isPotreroActive(potrero_id) retorna false, THEN THE Cattle_Inventory_Module SHALL retornar código HTTP 400 con mensaje "Potrero no existe o está inactivo"
3. WHEN un animal es asignado a un potrero por primera vez, THE Cattle_Inventory_Module SHALL insertar en tabla pasture_history: animal_id, potrero_id, fecha_entrada (timestamp UTC actual), fecha_salida (null), created_by
4. WHEN un animal es movido de un potrero a otro, THE Cattle_Inventory_Module SHALL actualizar el registro anterior en pasture_history con fecha_salida = timestamp UTC actual, luego insertar nuevo registro con fecha_entrada = timestamp UTC actual y fecha_salida = null
5. THE Cattle_Inventory_Module SHALL garantizar que un animal tenga máximo 1 registro en pasture_history con fecha_salida = null en cualquier momento (ubicación actual única)
6. WHEN un usuario consulta el historial de movimientos de un animal via GET /api/v1/cattle/{id}/movements, THE Cattle_Inventory_Module SHALL retornar array ordenado cronológicamente (fecha_entrada DESC) con campos: potrero_id, nombre_potrero, fecha_entrada, fecha_salida, dias_permanencia (DATEDIFF(fecha_salida, fecha_entrada) o DATEDIFF(NOW(), fecha_entrada) si fecha_salida null)
7. WHEN un usuario lista animales de un potrero específico via GET /api/v1/pastures/{potrero_id}/cattle, THE Cattle_Inventory_Module SHALL retornar solo animales WHERE EXISTS(pasture_history WHERE potrero_id = :id AND fecha_salida IS NULL)
8. THE Cattle_Inventory_Module SHALL filtrar animales por tenant_id en todas las consultas de ubicación/movimientos
9. WHEN se mueve un animal, THE Cattle_Inventory_Module SHALL registrar en cattle_audit: operation_type (MOVE_PASTURE), old_values (potrero anterior), new_values (potrero nuevo), timestamp, modified_by
10. WHEN un animal es vendido o muere, THE Cattle_Inventory_Module SHALL actualizar el último registro de pasture_history con fecha_salida = timestamp actual antes de cambiar status

### Requirement 4: Estados del Animal (Status)

**User Story:** Como usuario, quiero registrar el estado actual del animal (activa, preñada, vendida, muerta, etc.), para que tenga control del inventario activo y trazabilidad del ganado que sale del rancho.

#### Acceptance Criteria

1. THE Cattle_Inventory_Module SHALL soportar exactamente seis valores de status: Activa, Vendida, Muerta, Prestada, Preñada, En Reposo
2. WHEN un animal nuevo es registrado sin especificar status, THE Cattle_Inventory_Module SHALL asignar automáticamente status = Activa
3. WHEN un usuario cambia status de un animal a Vendida, THE Cattle_Inventory_Module SHALL requerir campos fecha_venta (NOT NULL) y precio_venta (decimal > 0 opcional) en el request
4. WHEN un usuario cambia status a Muerta, THE Cattle_Inventory_Module SHALL requerir campos fecha_muerte (NOT NULL) y motivo_muerte (string max 500 chars opcional)
5. WHEN un usuario cambia status a Preñada, THE Cattle_Inventory_Module SHALL validar que el animal tenga sexo = Hembra antes de persistir, retornar HTTP 400 con mensaje "Solo hembras pueden estar preñadas" si sexo ≠ Hembra
6. WHEN status cambia a Vendida o Muerta, THE Cattle_Inventory_Module SHALL actualizar el registro actual en pasture_history con fecha_salida = timestamp actual (sacar del potrero)
7. WHEN un usuario lista animales con filtro status, THE Cattle_Inventory_Module SHALL retornar solo animales WHERE status IN (:statusValues)
8. THE Cattle_Inventory_Module SHALL definir Active_Inventory como: status IN (Activa, Preñada, En Reposo) AND tenant_id = :currentTenantId
9. THE Cattle_Inventory_Module SHALL definir Historical_Inventory como: status IN (Vendida, Muerta) AND tenant_id = :currentTenantId
10. WHEN se cambia status, THE Cattle_Inventory_Module SHALL registrar en cattle_audit: operation_type (CHANGE_STATUS), old_values (status anterior + campos relacionados), new_values (status nuevo + campos relacionados), timestamp, modified_by, reason (opcional)

### Requirement 5: Tipos Comerciales

**User Story:** Como ganadero, quiero clasificar animales por tipo comercial (venta, cría, engorda, etc.), para que organice mi inventario según finalidad productiva.

#### Acceptance Criteria

1. THE Cattle_Inventory_Module SHALL soportar valores de tipo: Venta, Cría, Engorda, Semental, Vientre
2. WHEN un animal es registrado sin especificar tipo, THE Cattle_Inventory_Module SHALL asignar automáticamente tipo = Venta
3. WHEN un usuario filtra animales por tipo via GET /api/v1/cattle?tipo={tipo}, THE Cattle_Inventory_Module SHALL retornar solo animales WHERE tipo = :tipo AND tenant_id = :currentTenantId
4. WHEN un usuario cambia tipo de un animal, THE Cattle_Inventory_Module SHALL validar que el nuevo tipo esté en el catálogo y retornar HTTP 400 si es inválido
5. THE Cattle_Inventory_Module SHALL permitir combinar filtros status + tipo (ejemplo: animales Activas de tipo Vientre)
6. WHEN se cambia tipo, THE Cattle_Inventory_Module SHALL registrar en cattle_audit: operation_type (CHANGE_TYPE), old_values, new_values

### Requirement 6: Folio REEMO y Documentación Legal

**User Story:** Como usuario sujeto a regulación mexicana, quiero registrar el Folio REEMO de mis animales, para que cumpla con los requisitos legales de movilización de ganado.

#### Acceptance Criteria

1. THE Cattle_Inventory_Module SHALL permitir campo folio_reemo (string alfanumérico max 50 caracteres, opcional) en el registro del animal
2. WHEN se proporciona folio_reemo con caracteres especiales diferentes a guión (-), THE Cattle_Inventory_Module SHALL retornar HTTP 400 con mensaje "Folio REEMO solo puede contener letras, números y guiones"
3. THE Cattle_Inventory_Module SHALL permitir que múltiples animales tengan el mismo folio_reemo (movilización en lote)
4. WHEN un usuario busca animales por folio_reemo via GET /api/v1/cattle?folio_reemo={folio}, THE Cattle_Inventory_Module SHALL retornar todos los animales WHERE folio_reemo = :folio AND tenant_id = :currentTenantId
5. WHEN se actualiza folio_reemo, THE Cattle_Inventory_Module SHALL registrar cambio en cattle_audit

### Requirement 7: Registro de Pesos

**User Story:** Como usuario, quiero registrar pesos del animal en diferentes fechas, para que monitoree el crecimiento y engorda de mi ganado.

#### Acceptance Criteria

1. THE Cattle_Inventory_Module SHALL permitir registrar múltiples pesos para un animal en tabla cattle_weights con campos: weight_id (UUID PK), animal_id (FK), peso_kg (decimal >= 0), fecha_pesaje (date NOT NULL), notas (text opcional), created_at, created_by
2. WHEN un usuario registra un peso con fecha_pesaje futura, THE Cattle_Inventory_Module SHALL retornar HTTP 400 con mensaje "La fecha de pesaje no puede ser futura"
3. WHEN un usuario registra un peso con peso_kg negativo o cero, THE Cattle_Inventory_Module SHALL retornar HTTP 400 con mensaje "El peso debe ser mayor que cero"
4. WHEN un usuario consulta historial de pesos via GET /api/v1/cattle/{id}/weights, THE Cattle_Inventory_Module SHALL retornar array ordenado por fecha_pesaje DESC con campos: peso_kg, fecha_pesaje, notas, ganancia_diaria (calculada desde registro anterior)
5. THE Cattle_Inventory_Module SHALL calcular ganancia_diaria como: (peso_actual - peso_anterior) / DATEDIFF(fecha_actual, fecha_anterior) y expresarla en kg/día
6. WHEN un usuario consulta un animal, THE Cattle_Inventory_Module SHALL incluir peso_actual (peso más reciente) extraído de cattle_weights WHERE animal_id = :id ORDER BY fecha_pesaje DESC LIMIT 1
7. THE Cattle_Inventory_Module SHALL validar que animal_id existe y pertenece al tenant antes de permitir registro de peso

### Requirement 8: Eventos de Salud

**User Story:** Como usuario, quiero registrar eventos de salud del animal (vacunaciones, tratamientos, partos), para que tenga historial médico y reproductivo completo.

#### Acceptance Criteria

1. THE Cattle_Inventory_Module SHALL permitir registrar eventos de salud en tabla health_events con campos: event_id (UUID PK), animal_id (FK), event_type (VACCINATION, TREATMENT, BIRTH, DIAGNOSIS), fecha_evento (date NOT NULL), descripcion (text max 1000 chars), costo (decimal >= 0 opcional), veterinario_id (UUID FK opcional), created_at, created_by
2. WHEN un usuario registra un evento de tipo VACCINATION, THE Cattle_Inventory_Module SHALL requerir campo vacuna_nombre (string max 100 chars NOT NULL) en el JSON de descripcion
3. WHEN un usuario registra un evento de tipo TREATMENT, THE Cattle_Inventory_Module SHALL requerir campo medicamento_nombre y dosis en el JSON de descripcion
4. WHEN un usuario registra un evento de tipo BIRTH (parto), THE Cattle_Inventory_Module SHALL validar que animal_id sea hembra, requerir campo cria_arete (string del arete de la cría recién nacida), y actualizar status de la madre a Activa si estaba en Preñada
5. WHEN un evento BIRTH es registrado, THE Cattle_Inventory_Module SHALL crear automáticamente un nuevo animal con: arete = cria_arete, madre_id = animal_id, fecha_nacimiento = fecha_evento, sexo (requerido en request), status = Activa, potrero_id = potrero actual de la madre (copiado de pasture_history)
6. WHEN un usuario consulta historial de salud via GET /api/v1/cattle/{id}/health, THE Cattle_Inventory_Module SHALL retornar array ordenado por fecha_evento DESC
7. WHEN un usuario lista vacunaciones próximas a vencer (30 días), THE Cattle_Inventory_Module SHALL calcular próxima dosis basada en event_type = VACCINATION y fecha_evento + intervalo_dias (almacenado en descripcion JSON)
8. THE Cattle_Inventory_Module SHALL filtrar eventos por tenant_id y validar que animal_id pertenece al tenant antes de crear evento

### Requirement 9: Nota y Observaciones

**User Story:** Como usuario, quiero añadir notas y observaciones libres sobre cada animal, para que registre información relevante que no entra en campos estructurados.

#### Acceptance Criteria

1. THE Cattle_Inventory_Module SHALL permitir campo nota (text max 2000 caracteres, opcional) en el registro del animal
2. WHEN un usuario actualiza la nota, THE Cattle_Inventory_Module SHALL reemplazar completamente el contenido anterior (no append) y registrar cambio en cattle_audit
3. WHEN un usuario consulta un animal, THE Cattle_Inventory_Module SHALL incluir la nota en la respuesta JSON
4. THE Cattle_Inventory_Module SHALL permitir búsqueda por texto libre en notas via GET /api/v1/cattle?search={texto} ejecutando LIKE '%:texto%' en el campo nota

### Requirement 10: Fecha de Aretado

**User Story:** Como usuario, quiero registrar la fecha en que se colocó el arete al animal, para que tenga trazabilidad del proceso de identificación.

#### Acceptance Criteria

1. THE Cattle_Inventory_Module SHALL permitir campo fecha_aretado (date opcional) en el registro del animal
2. WHEN se proporciona fecha_aretado posterior a la fecha actual UTC, THE Cattle_Inventory_Module SHALL retornar HTTP 400 con mensaje "La fecha de aretado no puede ser futura"
3. WHEN se proporciona fecha_aretado anterior a fecha_nacimiento, THE Cattle_Inventory_Module SHALL retornar HTTP 400 con mensaje "La fecha de aretado no puede ser anterior al nacimiento"
4. WHEN un animal es creado automáticamente desde un evento BIRTH, THE Cattle_Inventory_Module SHALL asignar fecha_aretado = fecha_evento (fecha del parto)

### Requirement 11: API REST para Gestión de Ganado

**User Story:** Como desarrollador móvil, quiero consumir una API REST para gestionar ganado, para que integre la funcionalidad en la aplicación móvil.

#### Acceptance Criteria

1. THE Cattle_Inventory_Module SHALL exponer endpoints REST bajo ruta base /api/v1/cattle/ con autenticación JWT obligatoria
2. THE Cattle_Inventory_Module SHALL implementar endpoints: POST /cattle, GET /cattle, GET /cattle/{id}, PUT /cattle/{id}, DELETE /cattle/{id}, GET /cattle/{id}/movements, POST /cattle/{id}/move, GET /cattle/{id}/weights, POST /cattle/{id}/weights, GET /cattle/{id}/health, POST /cattle/{id}/health, GET /cattle/{id}/offspring, GET /pastures/{id}/cattle
3. THE Cattle_Inventory_Module SHALL aceptar requests con header Content-Type: application/json; charset=UTF-8
4. WHEN un endpoint retorna colección (lista de animales), THE Cattle_Inventory_Module SHALL incluir objeto pagination con campos: page (0-based), size (1-100), total (non-negative integer)
5. WHEN una operación completa exitosamente, THE Cattle_Inventory_Module SHALL retornar código HTTP: 200 para lecturas/actualizaciones, 201 para creaciones, 204 para archivado
6. WHEN un Request DTO falla validación Bean Validation, THE Cattle_Inventory_Module SHALL retornar código HTTP 400 con JSON conteniendo array de errores (field, message)
7. WHEN un JWT_Token es inválido o ausente, THE Cattle_Inventory_Module SHALL retornar código HTTP 401 con mensaje "Autenticación requerida"
8. WHEN un usuario intenta acceder a animal de otro tenant, THE Cattle_Inventory_Module SHALL retornar código HTTP 403 con mensaje "Acceso denegado"
9. WHEN un recurso solicitado no existe, THE Cattle_Inventory_Module SHALL retornar código HTTP 404 con mensaje "Animal no encontrado"
10. THE Cattle_Inventory_Module SHALL documentar todos los endpoints en archivo OpenAPI 3.0 YAML ubicado en src/main/resources/openapi/openapi-cattle.yaml

### Requirement 12: Interfaz Web para Gestión de Inventario

**User Story:** Como usuario web, quiero visualizar y gestionar mi inventario de ganado en una interfaz intuitiva, para que acceda fácilmente desde navegador.

#### Acceptance Criteria

1. THE Cattle_Inventory_Module SHALL servir página HTML en ruta /cattle con lista de animales en tabla mostrando: arete, sexo, raza, edad (meses), tipo, status, potrero_actual, peso_actual
2. WHEN un usuario hace clic en un animal, THE Cattle_Inventory_Module SHALL navegar a /cattle/{id} mostrando detalles completos con tabs para: Información General, Genealogía, Historial de Movimientos, Pesos, Salud
3. THE Cattle_Inventory_Module SHALL renderizar árbol genealógico visual mostrando: madre, padre, abuelos maternos, abuelos paternos (si existen), hijos del animal
4. WHEN un usuario crea un nuevo animal via web, THE Cattle_Inventory_Module SHALL mostrar formulario modal con campos: arete (required), sexo (required), raza (required), fecha_nacimiento (required), tipo (select), potrero_id (select), madre_id (select opcional), padre_id (select opcional)
5. WHEN validación JavaScript detecta campo inválido, THE Cattle_Inventory_Module SHALL mostrar mensaje de error inline sin enviar request HTTP
6. THE Cattle_Inventory_Module SHALL implementar búsqueda en tiempo real de animales por arete usando JavaScript vanilla con debounce de 300ms
7. WHEN una petición Fetch API retorna código 4xx o 5xx, THE Cattle_Inventory_Module SHALL mostrar notificación toast con mensaje de error en español
8. THE Cattle_Inventory_Module SHALL incluir fragmentos Thymeleaf: fragments/cattle-card.html (tarjeta de animal), fragments/genealogy-tree.html (árbol genealógico), fragments/health-timeline.html (línea de tiempo de eventos de salud)

### Requirement 13: Integración con Módulo Geográfico

**User Story:** Como sistema integrado, quiero que el inventario de ganado se sincronice con la estructura de potreros, para que garantice consistencia entre ubicación de animales y disponibilidad de terreno.

#### Acceptance Criteria

1. WHEN un animal es asignado a un potrero, THE Cattle_Inventory_Module SHALL invocar GeographyService.isPotreroActive(potrero_id) para validar existencia y estado activo
2. WHEN un potrero es archivado en geographic-control, THE Cattle_Inventory_Module SHALL actualizar todos los animales en ese potrero con fecha_salida en pasture_history
3. THE Cattle_Inventory_Module SHALL exponer método público CattleService.countAnimalsInPasture(UUID potreroId) para que geographic-control consulte cantidad de ganado antes de archivar potrero
4. THE Cattle_Inventory_Module SHALL filtrar potreros disponibles en formularios web solo mostrando poteros WHERE GeographyService.isPotreroActive(id) = true

### Requirement 14: Auditoría de Cambios en Inventario

**User Story:** Como administrador, quiero un registro completo de cambios en el inventario, para que rastree modificaciones y cumpla con auditorías.

#### Acceptance Criteria

1. THE Cattle_Inventory_Module SHALL registrar en tabla cattle_audit cada operación CREATE, UPDATE, DELETE, CHANGE_STATUS, MOVE_PASTURE sobre animales
2. WHEN se crea un animal, THE Cattle_Inventory_Module SHALL insertar en cattle_audit: audit_id (UUID), animal_id, operation_type (CREATE), timestamp (UTC), modified_by, tenant_id, old_values (null), new_values (JSON completo del animal)
3. WHEN se actualiza un animal, THE Cattle_Inventory_Module SHALL insertar en cattle_audit: operation_type (UPDATE), old_values (solo campos modificados), new_values (solo campos modificados)
4. WHEN se mueve un animal de potrero, THE Cattle_Inventory_Module SHALL insertar operation_type (MOVE_PASTURE), old_values (potrero_id anterior), new_values (potrero_id nuevo)
5. THE Cattle_Inventory_Module SHALL incluir reason (string max 500 chars opcional) en registros de auditoría para cambios críticos (status Vendida, Muerta)
6. THE Cattle_Inventory_Module SHALL retener registros de auditoría por mínimo 730 días (2 años)

### Requirement 15: Estadísticas e Indicadores

**User Story:** Como ganadero, quiero visualizar estadísticas clave de mi inventario, para que tome decisiones informadas sobre mi operación.

#### Acceptance Criteria

1. WHEN un usuario consulta estadísticas via GET /api/v1/cattle/stats, THE Cattle_Inventory_Module SHALL retornar JSON con: total_animales, total_activos, total_machos, total_hembras, distribucion_por_raza (array), distribucion_por_tipo (array), distribucion_por_status (array), distribucion_por_potrero (array con count)
2. THE Cattle_Inventory_Module SHALL calcular promedio_edad como: AVG(meses) WHERE status IN (Activa, Preñada, En Reposo)
3. THE Cattle_Inventory_Module SHALL calcular promedio_peso como: AVG(peso_kg) FROM cattle_weights WHERE animal_id IN (animales activos) AND fecha_pesaje >= CURRENT_DATE - INTERVAL 30 DAY (último mes)
4. THE Cattle_Inventory_Module SHALL cachear estadísticas durante 15 minutos usando @Cacheable con key = "stats:cattle:tenant:{tenantId}:rancho:{ranchoId}"

### Requirement 16: Validaciones de Negocio Complejas

**User Story:** Como sistema, quiero validar reglas de negocio complejas, para que prevenga estados inconsistentes en el inventario.

#### Acceptance Criteria

1. WHEN un animal tiene status Vendida o Muerta, THE Cattle_Inventory_Module SHALL prohibir: cambio de potrero, registro de nuevo peso, cambio a status Preñada, retornar HTTP 400 con mensaje "No se puede modificar un animal vendido o muerto"
2. WHEN un animal macho tiene status Preñada, THE Cattle_Inventory_Module SHALL retornar HTTP 400 (validación preventiva, no debería ocurrir)
3. WHEN se intenta registrar madre_id = animal_id (animal es su propia madre), THE Cattle_Inventory_Module SHALL retornar HTTP 400 con mensaje "Un animal no puede ser su propia madre"
4. WHEN se intenta registrar padre_id = animal_id, THE Cattle_Inventory_Module SHALL retornar HTTP 400 con mensaje "Un animal no puede ser su propio padre"
5. WHEN se intenta registrar fecha_nacimiento anterior a fecha_nacimiento de la madre, THE Cattle_Inventory_Module SHALL retornar HTTP 400 con mensaje "La fecha de nacimiento no puede ser anterior al nacimiento de la madre"

## Notes

- Este módulo es el núcleo del sistema y debe implementarse después de user-management y geographic-control
- La relación con geographic-control es crítica: validar siempre potrero_id antes de asignar
- El cálculo de edad (meses) debe ser en tiempo real, no almacenado, para precisión
- El arete debe ser único a nivel global (no solo por tenant) para prevenir duplicados en futuras integraciones
- La genealogía puede extenderse a más generaciones en futuras versiones (actualmente solo padres directos)
- Los eventos de salud pueden integrarse con un módulo dedicado de veterinaria en el futuro
- Considerar implementar cálculo de consanguinidad en futuras versiones
- El historial de movimientos es crítico para análisis de pastoreo rotacional
- Los pesos deben registrarse con frecuencia regular para monitoreo efectivo de engorda
