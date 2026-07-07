# Módulo Users - Vacapp

## Descripción General

El módulo **Users** es el núcleo de autenticación y gestión de usuarios de Vacapp, implementado siguiendo la arquitectura Spring Modulith + Clean Architecture. Proporciona autenticación JWT, autorización basada en roles, gestión multi-tenant de usuarios y una interfaz dual (API REST + Web Thymeleaf).

## Arquitectura

Este módulo sigue estrictamente los principios de Spring Modulith:

- **API Pública**: `UsersService.java` en la raíz del paquete `mx.vacapp.users/`
- **Implementación Privada**: Todo bajo `internal/` está encapsulado y no debe ser importado por otros módulos

### Estructura de Directorios

```
mx.vacapp.users/
├── UsersService.java                          ← API PÚBLICA (único punto de entrada)
│
└── internal/                                  ← PRIVADO (inaccesible para otros módulos)
    ├── domain/
    │   ├── model/                             ← Entidades de negocio puras (sin JPA)
    │   │   ├── User.java
    │   │   └── Role.java (enum)
    │   ├── repository/                        ← Puertos de salida (interfaces)
    │   │   ├── UserRepository.java
    │   │   └── AuditRepository.java
    │   └── exceptions/                        ← Excepciones de dominio
    │       ├── InvalidCredentialsException.java
    │       ├── UserNotFoundException.java
    │       ├── UserAlreadyExistsException.java
    │       └── AccountLockedException.java
    │
    ├── application/
    │   ├── usecases/                          ← Casos de uso (orquestación)
    │   │   ├── auth/
    │   │   │   ├── LoginUseCase.java
    │   │   │   └── ValidateTokenUseCase.java
    │   │   └── management/
    │   │       ├── CreateUserUseCase.java
    │   │       ├── UpdateUserUseCase.java
    │   │       ├── GetUserUseCase.java
    │   │       ├── ListUsersUseCase.java
    │   │       ├── DeactivateUserUseCase.java
    │   │       └── ChangeUserRoleUseCase.java
    │   └── commands/                          ← Comandos y resultados (Records)
    │       ├── LoginCommand.java
    │       ├── AuthResult.java
    │       ├── CreateUserCommand.java
    │       └── UserResult.java
    │
    └── infrastructure/
        ├── controllers/
        │   ├── web/                           ← Controladores MVC (Thymeleaf)
        │   │   ├── AuthWebController.java
        │   │   ├── UserWebController.java
        │   │   └── dtos/
        │   │       ├── LoginFormDto.java
        │   │       └── UserFormDto.java
        │   └── mobile/                        ← Controladores REST API (JSON/JWT)
        │       ├── AuthRestController.java
        │       ├── UserRestController.java
        │       └── dtos/
        │           ├── LoginRequest.java
        │           ├── LoginResponse.java
        │           ├── CreateUserRequest.java
        │           └── UserResponse.java
        ├── persistence/
        │   ├── entities/
        │   │   ├── UserEntity.java            ← @Entity JPA
        │   │   ├── UserAuditEntity.java
        │   │   └── AuthLogEntity.java
        │   ├── jpa/
        │   │   ├── UserJpaRepository.java     ← extends JpaRepository
        │   │   ├── UserAuditJpaRepository.java
        │   │   └── AuthLogJpaRepository.java
        │   ├── impl/
        │   │   ├── UserRepositoryImpl.java    ← implements UserRepository
        │   │   └── AuditRepositoryImpl.java
        │   └── mappers/
        │       ├── UserMapper.java            ← Mapea User ↔ UserEntity
        │       └── AuditMapper.java
        ├── security/
        │   ├── JwtTokenProvider.java
        │   ├── JwtAuthenticationFilter.java
        │   ├── TenantContext.java
        │   ├── SecurityConfig.java
        │   └── PasswordEncoderConfig.java
        └── config/
            └── UsersModuleConfig.java         ← Configuración del módulo
```

## Multi-tenancy

El módulo implementa multi-tenancy **row-level** usando el campo `tenant_id` en la tabla `users`:

- **Usuarios SaaS**: `tenant_id = NULL` (roles: `super_admin`, `support`)
- **Usuarios Business**: `tenant_id = UUID` (roles: `admin`, `manager`, `veterinarian`, `worker`)

### Aislamiento Automático

Todos los repositorios JPA aplican filtrado automático por `tenant_id` extraído del contexto de seguridad (`TenantContext`):

```java
// Ejemplo: UserRepositoryImpl filtra automáticamente por tenant_id
public List<User> findAll(int page, int size) {
    UUID currentTenantId = TenantContext.getCurrentTenantId();
    
    if (currentTenantId == null) {
        // Usuario SaaS: puede ver todos los usuarios
        return jpaRepository.findAll(PageRequest.of(page, size))
            .map(mapper::toDomain)
            .toList();
    } else {
        // Usuario Business: solo ve usuarios de su tenant
        return jpaRepository.findAllByTenantId(currentTenantId, PageRequest.of(page, size))
            .map(mapper::toDomain)
            .toList();
    }
}
```

## Seguridad y Autenticación

### JWT Token

El sistema usa JWT (JSON Web Tokens) con las siguientes características:

- **Algoritmo**: HS512
- **Expiración**: 24 horas (configurable en `application.yml`)
- **Claims**: `userId`, `email`, `name`, `role`, `tenantId`

Ejemplo de token decodificado:

```json
{
  "sub": "usuario@example.com",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "usuario@example.com",
  "name": "Juan Pérez",
  "role": "ADMIN",
  "tenantId": "789e4567-e89b-12d3-a456-426614174999",
  "iat": 1710345600,
  "exp": 1710432000
}
```

### Roles y Permisos

El sistema define 6 roles con una jerarquía clara:

| Rol            | Tipo     | Permisos                                              |
|----------------|----------|-------------------------------------------------------|
| `SUPER_ADMIN`  | SaaS     | Acceso completo al sistema, gestión de todos los tenants |
| `SUPPORT`      | SaaS     | Soporte técnico, acceso de solo lectura a todos los tenants |
| `ADMIN`        | Business | Administrador del tenant, gestión completa de usuarios |
| `MANAGER`      | Business | Gerente, acceso a reportes y gestión limitada        |
| `VETERINARIAN` | Business | Veterinario, acceso a registros de salud animal      |
| `WORKER`       | Business | Trabajador, acceso básico de lectura                 |

## API Pública del Módulo

Otros módulos de Vacapp pueden usar la interfaz `UsersService` para consultar información de usuarios:

```java
package mx.vacapp.users;

public interface UsersService {
    boolean isUserActive(UUID userId);
    UUID getUserTenantId(UUID userId);
    boolean hasRole(UUID userId, String role);
}
```

### Ejemplo de Uso desde otro Módulo

```java
package mx.vacapp.cattle;

import mx.vacapp.users.UsersService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CattleService {
    private final UsersService usersService;
    
    public void registerAnimal(UUID userId, RegisterAnimalCommand command) {
        // Verificar que el usuario esté activo
        if (!usersService.isUserActive(userId)) {
            throw new UserNotActiveException("Usuario inactivo");
        }
        
        // Obtener el tenant del usuario
        UUID tenantId = usersService.getUserTenantId(userId);
        
        // Registrar el animal en el tenant correspondiente
        // ...
    }
}
```

## API REST Endpoints

El módulo expone los siguientes endpoints REST (definidos en OpenAPI YAML):

### Autenticación

#### POST /api/v1/auth/login

Autentica un usuario y retorna un token JWT.

**Request Body:**

```json
{
  "email": "usuario@example.com",
  "password": "password123"
}
```

**Response 200 OK:**

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "usuario@example.com",
  "name": "Juan Pérez",
  "role": "ADMIN",
  "tenantId": "789e4567-e89b-12d3-a456-426614174999"
}
```

**Response 401 Unauthorized:**

```json
{
  "timestamp": "2024-03-15T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Credenciales inválidas"
}
```

### Gestión de Usuarios

#### GET /api/v1/users

Lista usuarios con paginación (requiere autenticación JWT).

**Query Parameters:**
- `page` (opcional, default: 0)
- `size` (opcional, default: 10, max: 50)

**Headers:**
```
Authorization: Bearer <token>
```

**Response 200 OK:**

```json
{
  "users": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "email": "usuario@example.com",
      "name": "Juan Pérez",
      "phone": "+52 123 456 7890",
      "role": "ADMIN",
      "status": "ACTIVE",
      "tenantId": "789e4567-e89b-12d3-a456-426614174999",
      "createdAt": "2024-03-01T10:00:00"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "total": 25,
    "totalPages": 3
  }
}
```

#### GET /api/v1/users/{id}

Obtiene un usuario por ID (requiere autenticación JWT).

**Response 200 OK:**

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "usuario@example.com",
  "name": "Juan Pérez",
  "phone": "+52 123 456 7890",
  "role": "ADMIN",
  "status": "ACTIVE",
  "tenantId": "789e4567-e89b-12d3-a456-426614174999",
  "createdAt": "2024-03-01T10:00:00"
}
```

#### POST /api/v1/users

Crea un nuevo usuario (requiere autenticación JWT).

**Request Body:**

```json
{
  "email": "nuevo@example.com",
  "name": "Nuevo Usuario",
  "phone": "+52 123 456 7890",
  "password": "securePassword123",
  "role": "WORKER"
}
```

**Response 201 Created:**

```json
{
  "id": "456e7890-e89b-12d3-a456-426614174000",
  "email": "nuevo@example.com",
  "name": "Nuevo Usuario",
  "phone": "+52 123 456 7890",
  "role": "WORKER",
  "status": "ACTIVE",
  "tenantId": "789e4567-e89b-12d3-a456-426614174999",
  "createdAt": "2024-03-15T10:30:00"
}
```

#### PUT /api/v1/users/{id}

Actualiza un usuario existente (requiere autenticación JWT).

**Request Body:**

```json
{
  "name": "Nombre Actualizado",
  "phone": "+52 987 654 3210",
  "role": "MANAGER"
}
```

**Response 200 OK:**

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "usuario@example.com",
  "name": "Nombre Actualizado",
  "phone": "+52 987 654 3210",
  "role": "MANAGER",
  "status": "ACTIVE",
  "tenantId": "789e4567-e89b-12d3-a456-426614174999",
  "updatedAt": "2024-03-15T11:00:00"
}
```

#### DELETE /api/v1/users/{id}

Desactiva un usuario (soft delete, requiere autenticación JWT).

**Response 204 No Content**

## Interfaz Web (Thymeleaf)

El módulo también proporciona una interfaz web usando Thymeleaf:

### Rutas Web

- **GET /auth/login**: Formulario de login
- **GET /dashboard**: Dashboard principal (requiere autenticación)
- **GET /admin/usuarios**: Gestión de usuarios (requiere autenticación)

### Vistas Implementadas

1. **templates/auth/login.html**: Formulario de login con JavaScript vanilla
2. **templates/dashboard/index.html**: Dashboard con estadísticas y accesos rápidos
3. **templates/admin/usuarios.html**: Tabla de usuarios con formulario modal para CRUD
4. **templates/fragments/navbar.html**: Barra de navegación superior
5. **templates/fragments/sidebar.html**: Menú lateral

### CSS Vanilla

El frontend no usa frameworks CSS (Tailwind, Bootstrap, etc.), solo CSS vanilla con variables:

- **static/css/global.css**: Variables CSS custom y reset
- **static/css/navbar.css**: Estilos del navbar
- **static/css/sidebar.css**: Estilos del sidebar
- **static/css/dashboard.css**: Estilos del dashboard
- **static/css/login.css**: Estilos del formulario de login

## Base de Datos

### Tabla: users

```sql
CREATE TABLE users (
    user_id CHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('SUPER_ADMIN', 'SUPPORT', 'ADMIN', 'MANAGER', 'VETERINARIAN', 'WORKER') NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'LOCKED') NOT NULL DEFAULT 'ACTIVE',
    tenant_id CHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by CHAR(36),
    updated_by CHAR(36),
    UNIQUE KEY unique_email_per_tenant (email, tenant_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Tabla: users_audit

```sql
CREATE TABLE users_audit (
    audit_id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by CHAR(36) NOT NULL,
    old_values TEXT,
    new_values TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Tabla: authentication_log

```sql
CREATE TABLE authentication_log (
    log_id CHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    success BOOLEAN NOT NULL,
    client_ip VARCHAR(45),
    user_agent VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_timestamp (timestamp),
    INDEX idx_success (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## Configuración

### application.yml

```yaml
vacapp:
  jwt:
    secret: ${JWT_SECRET:defaultSecretKeyForDevelopmentOnly}
    expiration-ms: 86400000  # 24 horas

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/vacapp?useSSL=false&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:password}
    
  security:
    cors:
      allowed-origins: http://localhost:3000,http://localhost:8080
      allowed-methods: GET,POST,PUT,DELETE,OPTIONS
      allowed-headers: '*'
      allow-credentials: true
```

## Testing

El módulo cuenta con 3 niveles de testing:

### 1. Tests de Integración (TestContainers)

Prueban el sistema completo con una base de datos MySQL real en contenedor Docker.

```bash
mvn test -Dtest=*IntegrationTest
```

### 2. Tests Unitarios (JUnit + Mockito)

Prueban casos de uso individuales con mocks.

```bash
mvn test -Dtest=*UseCaseTest
```

### 3. Property-Based Tests (jqwik)

Validan propiedades matemáticas y de correctitud con miles de casos generados automáticamente.

```bash
mvn test -Dtest=*PropertiesTest
```

## Logging y Auditoría

### Sanitización Automática

El módulo implementa sanitización automática de datos sensibles en logs usando `SensitiveDataMaskingLayout`:

```
2024-03-15 10:30:00 INFO  LoginUseCase - User login attempt: email=usuario@example.com, password=[REDACTED]
```

### Auditoría Completa

Todas las operaciones críticas se registran en la tabla `users_audit`:

- Creación de usuarios
- Actualización de usuarios
- Desactivación de usuarios
- Cambios de rol

Todos los intentos de autenticación (exitosos y fallidos) se registran en `authentication_log`.

## Comandos Útiles

```bash
# Compilar el proyecto
mvn clean compile

# Ejecutar tests
mvn test

# Generar JavaDoc
mvn javadoc:javadoc

# Ejecutar la aplicación
mvn spring-boot:run

# Verificar arquitectura Spring Modulith
mvn spring-modulith:verify
```

## Swagger UI

El módulo genera automáticamente documentación interactiva usando OpenAPI 3.0:

- **URL**: http://localhost:8080/swagger-ui/index.html
- **Spec YAML**: `src/main/resources/openapi/openapi-users.yaml`

## Contacto y Soporte

Para preguntas o reportar problemas relacionados con el módulo Users:

- **Equipo de desarrollo**: Vacapp Development Team
- **Repositorio**: [GitHub - Vacapp](https://github.com/your-org/vacapp)

---

**Última actualización**: 2024-03-15  
**Versión del módulo**: 1.0.0  
**Spring Boot**: 4.1.0  
**Java**: 21
