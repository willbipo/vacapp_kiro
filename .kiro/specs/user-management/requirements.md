# Requirements Document

## Introduction

El módulo de gestión de usuarios (user-management) proporciona funcionalidades completas de autenticación, autorización y administración de usuarios para Vacapp, una aplicación SaaS de gestión ganadera multitenant. El sistema implementa una arquitectura de roles en dos niveles: roles SaaS para la administración de la plataforma y roles de negocio para la gestión dentro de cada tenant. El módulo soporta tanto una interfaz web basada en Thymeleaf como una API REST para aplicaciones móviles con autenticación JWT.

## Glossary

- **User_Module**: El módulo de gestión de usuarios que gestiona autenticación, autorización y CRUD de usuarios
- **Authentication_System**: Subsistema responsable de validar credenciales y generar tokens JWT
- **Authorization_System**: Subsistema responsable de verificar permisos basados en roles
- **User_Repository**: Puerto de salida para acceder y persistir usuarios
- **Role_System**: Subsistema que gestiona los roles y sus jerarquías
- **SaaS_Role**: Roles de nivel plataforma (super_admin, support) que operan sin restricción de tenant
- **Business_Role**: Roles de nivel tenant (admin, manager, veterinarian, worker) que operan dentro de un tenant específico
- **Tenant_Context**: Contexto de seguridad que identifica el tenant actual
- **JWT_Token**: Token JSON Web Token utilizado para autenticación stateless
- **Password_Encoder**: Componente que cifra y verifica contraseñas usando BCrypt
- **Mobile_API**: Endpoints REST JSON para consumo desde aplicación móvil
- **Web_Interface**: Controladores MVC que sirven vistas Thymeleaf
- **Pretty_Printer**: Componente que formatea objetos de dominio en representaciones legibles

## Requirements

### Requirement 1: Autenticación de Usuarios

**User Story:** Como usuario de Vacapp, quiero autenticarme con mi email y contraseña, para que pueda acceder de forma segura a la aplicación web y móvil.

#### Acceptance Criteria

1. WHEN un usuario proporciona email registrado con contraseña que coincide con el hash BCrypt almacenado, THE Authentication_System SHALL generar un JWT_Token válido con duración de exactamente 24 horas desde el momento de emisión
2. WHEN un usuario proporciona email no registrado o contraseña que no coincide con el hash, THE Authentication_System SHALL retornar código HTTP 401 con mensaje "Credenciales inválidas" sin indicar cuál campo es incorrecto
3. WHEN un JWT_Token con claim exp menor que el timestamp actual UTC es presentado, THE Authentication_System SHALL rechazar la petición con código HTTP 401 y mensaje "Token expirado"
4. THE Password_Encoder SHALL utilizar BCrypt con factor de trabajo entre 10 y 12 (inclusive) para cifrar contraseñas
5. WHEN un usuario con Business_Role se autentica exitosamente, THE Authentication_System SHALL incluir en el JWT_Token el tenant_id de su registro y un array con su rol activo (longitud 1)
6. THE Authentication_System SHALL validar que el email tenga formato RFC 5322 con longitud máxima 254 caracteres antes de consultar la base de datos
7. WHEN un usuario con SaaS_Role se autentica exitosamente, THE Authentication_System SHALL generar JWT_Token con tenant_id null y array de roles conteniendo su rol SaaS
8. THE Authentication_System SHALL incluir en el JWT_Token los claims: user_id (UUID string), email (string), tenant_id (UUID string o null), roles (array de strings con longitud máxima 10)
9. WHEN un usuario con estado inactivo proporciona credenciales correctas, THE Authentication_System SHALL retornar código HTTP 403 con mensaje "Cuenta inactiva"
10. WHEN un usuario supera 5 intentos fallidos de autenticación en ventana de 15 minutos, THE Authentication_System SHALL bloquear temporalmente el usuario y retornar código HTTP 429 con mensaje "Demasiados intentos, intente en X minutos"
11. WHEN el bloqueo temporal de un usuario expira después de 15 minutos desde el quinto intento fallido, THE Authentication_System SHALL permitir nuevos intentos de autenticación

### Requirement 2: Registro de Usuarios

**User Story:** Como administrador de tenant, quiero registrar nuevos usuarios en mi organización, para que puedan acceder al sistema con sus propias credenciales.

#### Acceptance Criteria

1. WHEN un administrador (con rol admin o super_admin) registra un nuevo usuario con email único dentro del tenant, THE User_Module SHALL crear el usuario con estado activo y retornar código HTTP 201 con los datos del usuario creado
2. WHEN un administrador intenta registrar un usuario con email que ya existe en el mismo tenant (case-insensitive), THE User_Module SHALL retornar código HTTP 409 con mensaje "Email ya registrado en esta organización"
3. THE User_Module SHALL permitir crear usuarios con el mismo email en diferentes tenants (WHERE tenant_id difiere)
4. WHEN un nuevo usuario es registrado sin especificar rol, THE User_Module SHALL asignar automáticamente el rol "worker" antes de persistir
5. WHEN se proporciona un email con formato válido RFC 5322 y longitud máxima 255 caracteres, THE User_Module SHALL aceptar el email
6. WHEN se proporciona una contraseña con longitud entre 8 y 128 caracteres (inclusive), al menos una letra mayúscula, una minúscula, y un dígito, THE User_Module SHALL aceptar la contraseña y cifrarla con BCrypt
7. WHEN se proporciona una contraseña que no cumple los criterios de complejidad, THE User_Module SHALL retornar código HTTP 400 con mensaje "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número"
8. WHEN un super_admin registra un usuario especificando tenant_id null, THE User_Module SHALL crear el usuario sin asociación a tenant (usuario SaaS)
9. WHEN un nuevo usuario es registrado, THE User_Module SHALL registrar created_at (timestamp UTC actual), created_by (user_id del autenticado), y tenant_id del contexto (o null para SaaS)
10. WHEN se proporciona un rol que no existe en el catálogo [super_admin, support, admin, manager, veterinarian, worker], THE User_Module SHALL retornar código HTTP 400 con mensaje "Rol inválido"

### Requirement 3: Gestión de Roles

**User Story:** Como administrador, quiero asignar y modificar roles de usuarios, para que tengan los permisos apropiados según sus responsabilidades.

#### Acceptance Criteria

1. THE Role_System SHALL soportar exactamente seis roles: super_admin, support, admin, manager, veterinarian, worker
2. THE Role_System SHALL clasificar super_admin y support como SaaS_Roles (WHERE tenant_id IS NULL en la base de datos)
3. THE Role_System SHALL clasificar admin, manager, veterinarian y worker como Business_Roles (WHERE tenant_id IS NOT NULL)
4. WHEN un administrador (con rol admin o super_admin) asigna un Business_Role a un usuario, THE Role_System SHALL validar que user.tenant_id = authenticated_user.tenant_id antes de persistir
5. IF la validación de tenant falla durante asignación de Business_Role, THEN THE Role_System SHALL retornar código HTTP 403 con mensaje "No puede asignar roles a usuarios de otro tenant"
6. WHEN un super_admin asigna un SaaS_Role (super_admin o support), THE Role_System SHALL permitir la asignación sin validar tenant_id y retornar código HTTP 200
7. THE User_Module SHALL garantizar que cada registro en la tabla users tenga exactamente un valor no-null en la columna role
8. WHEN se cambia el rol de un usuario (UPDATE users SET role = new_role WHERE user_id = X), THE Role_System SHALL registrar en users_audit: timestamp UTC, user_id del responsable, old_value del rol anterior, new_value del nuevo rol
9. IF el cambio de rol falla por restricción de base de datos o permiso denegado, THEN THE Role_System SHALL retornar código HTTP 400 o 403 sin modificar el rol existente

### Requirement 4: Autorización Basada en Roles

**User Story:** Como sistema, quiero verificar permisos según el rol del usuario, para que solo accedan a funcionalidades permitidas.

#### Acceptance Criteria

1. WHEN un usuario autenticado con rol super_admin intenta ejecutar cualquier operación CRUD sobre cualquier recurso en cualquier tenant, THE Authorization_System SHALL permitir la operación
2. WHEN un usuario autenticado con rol support intenta ejecutar operaciones de lectura (GET) sobre cualquier recurso en cualquier tenant, THE Authorization_System SHALL permitir la operación
3. WHEN un usuario autenticado con rol support intenta ejecutar operaciones de escritura (POST/PUT/DELETE) sobre cualquier recurso, THE Authorization_System SHALL denegar la operación
4. WHILE un usuario tiene rol admin, THE Authorization_System SHALL permitir operaciones CRUD sobre recursos users, roles, y configuration dentro de su tenant
5. WHILE un usuario tiene rol manager, THE Authorization_System SHALL permitir operaciones de lectura (GET) sobre usuarios de su tenant y operaciones CRUD sobre recursos cattle, events, y production dentro de su tenant
6. WHILE un usuario tiene rol veterinarian, THE Authorization_System SHALL permitir operaciones CRUD sobre recursos health, treatments, y diagnoses dentro de su tenant y operaciones de lectura (GET) sobre cattle dentro de su tenant
7. WHILE un usuario tiene rol worker, THE Authorization_System SHALL permitir solo operaciones de lectura (GET) sobre recursos cattle, events, y production dentro de su tenant
8. WHEN un usuario intenta acceder a un recurso con tenant_id diferente al tenant_id en su JWT_Token (para Business_Roles), THE Authorization_System SHALL denegar el acceso sin ejecutar la operación
9. WHEN un usuario no autenticado (sin JWT_Token válido) intenta acceder a cualquier endpoint protegido, THE Authorization_System SHALL rechazar la petición con código HTTP 401

### Requirement 5: Filtrado Multi-tenant

**User Story:** Como sistema multitenant, quiero que todos los datos de usuarios sean aislados por tenant, para que cada organización solo acceda a sus propios datos.

#### Acceptance Criteria

1. THE User_Repository SHALL aplicar filtro WHERE tenant_id = :currentTenantId automáticamente en todas las operaciones de lectura (SELECT) y escritura (UPDATE/DELETE) cuando el Tenant_Context contenga un tenant_id
2. WHEN un usuario con Business_Role ejecuta una operación de lectura (SELECT), THE User_Repository SHALL aplicar filtro WHERE tenant_id = :currentTenantId extraído del JWT_Token antes de retornar resultados
3. WHEN un usuario con SaaS_Role (super_admin o support) ejecuta una operación de lectura sin especificar tenant_id en los parámetros de consulta, THE User_Repository SHALL omitir el filtro tenant_id y retornar datos de todos los tenants
4. IF el Tenant_Context no contiene tenant_id Y el usuario autenticado tiene Business_Role (admin, manager, veterinarian, worker), THEN THE User_Repository SHALL lanzar SecurityException con mensaje "Contexto de tenant requerido" sin ejecutar la consulta
5. WHEN un usuario intenta crear o actualizar un registro especificando tenant_id diferente al tenant_id en su JWT_Token (para Business_Roles), THE User_Module SHALL retornar código HTTP 403 con mensaje "No autorizado para modificar datos de otro tenant"
6. WHEN un usuario con SaaS_Role ejecuta una operación de escritura (INSERT/UPDATE/DELETE) especificando explícitamente tenant_id en los parámetros, THE User_Repository SHALL aplicar la operación sobre el tenant especificado sin restricción
7. THE User_Repository SHALL incluir filtro tenant_id en operaciones de conteo (COUNT), agregación (SUM/AVG), y búsqueda paginada siguiendo las mismas reglas de lectura

### Requirement 6: CRUD de Usuarios

**User Story:** Como administrador, quiero gestionar usuarios (crear, leer, actualizar, desactivar), para que mantenga actualizado el directorio de mi organización.

#### Acceptance Criteria

1. WHEN un administrador (admin o super_admin) lista usuarios sin especificar página, THE User_Module SHALL retornar la primera página (page=0) con máximo 50 usuarios filtrados por tenant_id con metadatos de paginación (page, size, total)
2. WHEN un administrador busca un usuario por ID válido dentro de su tenant, THE User_Module SHALL retornar JSON con campos: user_id, email, name, phone, role, status, tenant_id, created_at, updated_at, created_by, updated_by
3. WHEN un administrador busca un usuario por ID que no existe o pertenece a otro tenant, THE User_Module SHALL retornar código HTTP 404 con mensaje "Usuario no encontrado"
4. WHEN un administrador actualiza nombre (longitud 2-100 caracteres), email (RFC 5322 con max 255 chars), teléfono (7-20 dígitos), o rol de un usuario, THE User_Module SHALL persistir los cambios y retornar código HTTP 200 con datos actualizados
5. IF un administrador intenta modificar tenant_id de un usuario existente, THEN THE User_Module SHALL retornar código HTTP 400 con mensaje "No se permite cambiar la organización de un usuario"
6. WHEN un administrador desactiva un usuario (UPDATE users SET status = 'inactive'), THE User_Module SHALL marcar status como inactivo sin eliminar el registro (DELETE no ejecutado) y registrar el cambio en users_audit
7. WHEN un usuario con status = 'inactive' intenta autenticarse con credenciales correctas, THE Authentication_System SHALL retornar código HTTP 403 con mensaje "Cuenta inactiva, contacte al administrador"
8. THE User_Module SHALL registrar en users_audit cada operación de creación, actualización y desactivación con: operation_type (CREATE/UPDATE/DEACTIVATE), timestamp UTC, modified_by (user_id del autenticado)
9. WHEN un administrador crea un nuevo usuario, THE User_Module SHALL validar email único dentro del tenant y retornar código HTTP 409 si ya existe (ver Requirement 2)

### Requirement 7: API REST para Móvil

**User Story:** Como desarrollador móvil, quiero consumir una API REST bien documentada, para que pueda integrar la funcionalidad de usuarios en la aplicación móvil.

#### Acceptance Criteria

1. THE Mobile_API SHALL exponer endpoints bajo ruta base /api/v1/users con autenticación JWT obligatoria validada mediante filtro Spring Security
2. THE Mobile_API SHALL implementar exactamente estos endpoints: POST /api/v1/auth/login, GET /api/v1/users, GET /api/v1/users/{id}, POST /api/v1/users, PUT /api/v1/users/{id}, DELETE /api/v1/users/{id}
3. THE Mobile_API SHALL aceptar requests con header Content-Type: application/json; charset=UTF-8 y retornar responses con mismo Content-Type
4. WHEN un endpoint retorna colección de usuarios (GET /api/v1/users), THE Mobile_API SHALL incluir objeto pagination con campos: page (0-based integer), size (integer 1-100), total (non-negative integer)
5. WHEN una operación completa exitosamente, THE Mobile_API SHALL retornar código HTTP 200 para lecturas/actualizaciones, 201 para creaciones exitosas
6. WHEN un Request DTO falla validación Bean Validation, THE Mobile_API SHALL retornar código HTTP 400 con JSON conteniendo array de errores (field, message)
7. WHEN un JWT_Token es inválido o ausente, THE Mobile_API SHALL retornar código HTTP 401 con mensaje "Autenticación requerida"
8. WHEN un usuario intenta acceder a recurso de otro tenant, THE Mobile_API SHALL retornar código HTTP 403 con mensaje "Acceso denegado"
9. WHEN un recurso solicitado no existe (usuario con ID inexistente), THE Mobile_API SHALL retornar código HTTP 404 con mensaje "Recurso no encontrado"
10. WHEN se intenta crear usuario con email duplicado en el tenant, THE Mobile_API SHALL retornar código HTTP 409 con mensaje "Email ya registrado"
11. THE Mobile_API SHALL documentar todos los endpoints en archivo OpenAPI 3.0 YAML ubicado en src/main/resources/openapi/openapi-users.yaml con schemas completos para Request/Response DTOs

### Requirement 8: Interfaz Web Thymeleaf

**User Story:** Como administrador web, quiero acceder a una interfaz gráfica para gestionar usuarios, para que pueda administrar sin usar herramientas técnicas.

#### Acceptance Criteria

1. THE Web_Interface SHALL servir página HTML en ruta /auth/login con formulario conteniendo campos input[type=text][name=email] e input[type=password][name=password] y botón submit que envía POST a /api/v1/auth/login
2. WHEN el POST /api/v1/auth/login retorna código HTTP 200 con JSON conteniendo campo token, THE Web_Interface SHALL ejecutar sessionStorage.setItem('vacapp_token', token) y window.location.href = '/dashboard'
3. WHEN el POST /api/v1/auth/login retorna código HTTP 4xx o 5xx, THE Web_Interface SHALL mostrar mensaje de error en elemento div.error-message con texto del campo message del JSON de respuesta
4. THE Web_Interface SHALL servir página HTML en ruta /admin/usuarios con tabla HTML que renderiza lista de usuarios obtenida de GET /api/v1/users usando Fetch API con header Authorization: Bearer {token}
5. THE Web_Interface SHALL incluir en /admin/usuarios formulario modal para crear usuario con campos: input[name=name] (required, min 2 max 100 chars), input[name=email] (required, type=email), input[name=password] (required, min 8 chars), select[name=role], que valida en JavaScript antes de enviar POST a /api/v1/users
6. WHEN validación JavaScript detecta campo inválido en formulario de usuario, THE Web_Interface SHALL mostrar mensaje "El campo X debe Y" en span.error adyacente al campo sin enviar request
7. WHEN una petición Fetch API retorna código 4xx o 5xx, THE Web_Interface SHALL mostrar en elemento div.notification el mensaje de error en español extraído del campo message del JSON de respuesta con botón para cerrar
8. THE Web_Interface SHALL incluir fragmentos Thymeleaf reutilizables: fragments/navbar.html (logo, usuario actual, botón logout) y fragments/sidebar.html (links a /dashboard, /admin/usuarios) que se importan con th:replace en todas las páginas internas

### Requirement 9: Serialización y Validación de DTOs

**User Story:** Como desarrollador, quiero que los DTOs sean Records Java con validación automática, para que garantice consistencia y calidad de datos.

#### Acceptance Criteria

1. THE User_Module SHALL definir todos los Request DTOs como Java Records ubicados en infrastructure/controllers/*/dtos/ con anotaciones Bean Validation (@NotNull, @Size, @Email, @Pattern)
2. THE User_Module SHALL definir todos los Response DTOs como Java Records ubicados en infrastructure/controllers/*/dtos/ sin anotaciones Bean Validation
3. WHEN un Request DTO inválido es recibido (falla @Valid en controller), THE User_Module SHALL retornar código HTTP 400 con JSON structure: {"errors": [{"field": "fieldName", "message": "validation message"}]}
4. THE User_Module SHALL mapear entre Request DTOs (de controllers) y objetos de dominio (en domain/model/) usando clases XxxMapper ubicadas en infrastructure/persistence/ con métodos toEntity y toDomain
5. FOR ALL Response DTOs conteniendo campos de tipo java.time.LocalDateTime o java.time.Instant, THE Pretty_Printer SHALL formatear fechas como String en formato ISO 8601: yyyy-MM-dd'T'HH:mm:ss'Z' con timezone UTC
6. THE User_Module SHALL omitir campo password de todos los Response DTOs retornados por endpoints GET (User_Repository selecciona todos los campos EXCEPT password)
7. FOR ALL Request DTOs con campo password (LoginRequest, CreateUserRequest, UpdatePasswordRequest), THE User_Module SHALL validar que password cumpla: longitud 8-128 caracteres, al menos una mayúscula [A-Z], una minúscula [a-z], un dígito [0-9], un carácter especial [@$!%*?&]
8. WHEN validación de password falla en Request DTO, THE User_Module SHALL incluir en array errors el objeto: {"field": "password", "message": "La contraseña debe tener 8-128 caracteres, una mayúscula, minúscula, dígito y carácter especial"}

### Requirement 10: Parseo y Formateo de Tokens JWT

**User Story:** Como sistema de autenticación, quiero parsear y formatear tokens JWT de forma consistente, para que garantice la integridad y trazabilidad de las sesiones.

#### Acceptance Criteria

1. WHEN un JWT_Token es recibido en header Authorization: Bearer {token}, THE Authentication_System SHALL parsear el token validando: estructura header.payload.signature, firma HMAC-SHA256 válida, claim exp > timestamp actual UTC con tolerancia de 60 segundos
2. WHEN un JWT_Token es generado tras login exitoso, THE Pretty_Printer SHALL formatear los claims como JSON con indentación de 2 espacios para logging (sin incluir en response HTTP)
3. WHEN un JWT_Token es recibido, THE Authentication_System SHALL validar presencia obligatoria de claims: sub (UUID string del user_id), exp (Unix timestamp), iat (Unix timestamp), tenant_id (UUID string o null), roles (JSON array de strings)
4. IF un JWT_Token malformado es recibido (estructura inválida, sin puntos separadores, base64 inválido), THEN THE Authentication_System SHALL retornar código HTTP 401 con mensaje "Token inválido"
5. FOR ALL JWT_Tokens válidos, ejecutar parsear(token) luego formatear(claims) luego parsear(formatear(claims)) SHALL producir objeto claims con contenido idéntico (mismos valores y tipos: sub string, exp long, roles array)
6. WHEN un JWT_Token tiene firma HMAC-SHA256 que no coincide con la clave secreta, THE Authentication_System SHALL retornar código HTTP 401 con mensaje "Token inválido" sin revelar detalles de firma o clave
7. THE Authentication_System SHALL cargar clave secreta desde variable de entorno JWT_SECRET o archivo application.yml con longitud mínima 256 bits (32 bytes), nunca hardcodeada en código fuente
8. FOR ALL operaciones de parseo que fallan (firma inválida, token expirado, estructura malformada), THE Authentication_System SHALL registrar en tabla authentication_log: timestamp UTC, error_type (InvalidSignature/Expired/Malformed), client_ip (extraído de request), sin incluir token completo ni clave secreta

### Requirement 11: Seguridad y Protección de Datos

**User Story:** Como usuario del sistema, quiero que mis datos personales estén protegidos, para que mi información sensible no sea expuesta.

#### Acceptance Criteria

1. THE User_Module SHALL nunca escribir valores de campos password, passwordHash, o cualquier campo cuyo nombre contenga "password" o "secret" (case-insensitive) en logs de cualquier nivel (ERROR, WARN, INFO, DEBUG, TRACE)
2. WHEN un error de validación incluye un campo cuyo nombre contiene "password" (case-insensitive), THE User_Module SHALL reemplazar el valor del campo con literal "[REDACTED]" en el mensaje de error retornado
3. IF un request HTTP es recibido en protocolo HTTP (no HTTPS) en ambiente de producción (definido por propiedad spring.profiles.active=prod), THEN THE User_Module SHALL rechazar el request con código HTTP 400 y mensaje "Solo se permiten conexiones HTTPS"
4. WHEN se detectan 5 requests POST a /api/v1/auth/login desde la misma dirección IP en ventana de 1 minuto, THE User_Module SHALL retornar código HTTP 429 con mensaje "Demasiados intentos, intente en {segundos_restantes} segundos" para requests adicionales de esa IP hasta que expire el minuto
5. WHEN se detectan 5 intentos de login fallidos consecutivos para el mismo email en ventana de 15 minutos, THE Authentication_System SHALL marcar user.status como 'locked' y retornar código HTTP 403 con mensaje "Cuenta bloqueada temporalmente por seguridad, intente en {minutos_restantes} minutos"
6. WHEN el User_Module recibe un Request DTO con campos de tipo String, THE User_Module SHALL rechazar el request con código HTTP 400 si detecta patrones de inyección SQL (keywords: SELECT, DROP, INSERT, UPDATE, DELETE seguidos de espacios) o XSS (tags HTML: <script>, <iframe>, onclick=)
7. THE User_Module SHALL definir inactividad web como tiempo desde última request HTTP autenticada con JWT válido, excluyendo requests a rutas públicas (/auth/login, /health)
8. WHEN han transcurrido exactamente 30 minutos desde el último request autenticado exitoso del usuario, THE Web_Interface SHALL ejecutar sessionStorage.removeItem('vacapp_token') y window.location.href = '/auth/login?timeout=true'
9. WHEN el User_Module rechaza un request por sesión expirada (token generado hace más de 30 minutos), THE User_Module SHALL retornar código HTTP 401 con mensaje "Sesión expirada, inicie sesión nuevamente"

### Requirement 12: Auditoría y Trazabilidad

**User Story:** Como auditor, quiero tener un registro completo de cambios en usuarios, para que pueda rastrear modificaciones y cumplir con regulaciones.

#### Acceptance Criteria

1. THE User_Module SHALL registrar en cada fila de tabla users los campos: created_at (timestamp UTC NOT NULL), updated_at (timestamp UTC NOT NULL actualizado en cada UPDATE), created_by (UUID del user_id responsable, NOT NULL), updated_by (UUID del user_id responsable, actualizado en cada UPDATE)
2. WHEN una operación UPDATE modifica campos de un usuario (name, email, role, status), THE User_Module SHALL insertar en tabla users_audit los campos: audit_id (UUID), user_id (UUID del usuario modificado), timestamp (UTC actual), modified_by (UUID del autenticado), operation_type ('UPDATE'), old_values (JSON con valores anteriores), new_values (JSON con valores nuevos)
3. THE User_Module SHALL insertar en tabla users_audit filas para operaciones: CREATE (al insertar usuario), UPDATE (al modificar campos), DEACTIVATE (al cambiar status a 'inactive'), con operation_type correspondiente
4. WHEN un intento de login ocurre (exitoso o fallido), THE User_Module SHALL insertar en tabla authentication_log los campos: log_id (UUID), email (string del intento), timestamp (UTC actual), success (boolean), client_ip (extraído de request header X-Forwarded-For o request.getRemoteAddr()), user_agent (extraído de request header User-Agent max 500 chars)
5. WHEN un administrador desactiva un usuario (UPDATE users SET status = 'inactive'), THE User_Module SHALL incluir en users_audit campo reason (string max 500 caracteres con motivo proporcionado en request) y modified_by (UUID del autenticado)
6. THE User_Module SHALL configurar tablas users_audit y authentication_log sin DELETE automático, reteniendo todas las filas por mínimo 730 días (2 años) antes de permitir purga manual por super_admin
7. WHEN un super_admin ejecuta consulta GET /api/v1/audit/users con parámetros opcionales start_date, end_date, user_id, operation_type, THE User_Module SHALL retornar filas de users_audit filtradas por los parámetros con paginación máxima de 1000 registros por página
8. IF la consulta de auditoría falla por filtros inválidos (fechas malformadas, user_id no UUID), THEN THE User_Module SHALL retornar código HTTP 400 con mensaje descriptivo del error de validación

## Notes

- Este módulo es fundamental y debe implementarse primero antes que otros módulos funcionales
- La separación clara entre SaaS_Roles y Business_Roles es crítica para el modelo de negocio multitenant
- El OpenAPI YAML debe ser el contrato principal entre backend y frontend móvil
- La interfaz web Thymeleaf debe ser responsiva y accesible desde tablets usadas en campo
- Considerar futura integración con SSO/OAuth2 para clientes enterprise (no implementar ahora)
